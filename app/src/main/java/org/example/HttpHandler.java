package org.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.example.payload.PushPayload;

/**
 * Handler for a simple CI webhook endpoint.
 *
 * Handles incoming webhook requests and dispatches to method-specific
 * handlers. This class focuses on parsing requests and extracting metadata
 * from push events.
 */
public class HttpHandler extends AbstractHandler
{
    private static final Path ALL_REPOS_DIR = Path.of("repos");
    private static final Path ALL_BUILDS_DIR = Path.of("builds-archive");

    private String configFileName = "config.properties";
    private String token; // Personal access token for GitHub
    
    /**
     * Handle incoming HTTP requests and dispatch by method.
     *
     * @param target path or target for Jetty routing
     * @param baseRequest Jetty request object used to mark the request handled
     * @param request servlet request
     * @param response servlet response
     * @throws IOException if reading request data fails
     */
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        switch (request.getMethod()) {
            case "PUT":
                handlePUT(request);
                break;

            case "POST":
                try {
                    handlePOST(request);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;

            case "GET":
                handleGET(target, response);
                break;

            default:
                break;
        }
    }
 
    /**
     * Handle PUT requests.
     *
     * @param request servlet request
     * @throws IOException if reading request data fails
     */
    public void handlePUT(HttpServletRequest request) throws IOException {
        // TODO: Can be removed if unused.
    }

    /**
     * Handle POST requests that carry webhook payloads.
     *
     * @param request servlet request
     * @throws IOException if reading request data fails
     * @throws InterruptedException If the HTTP request is interrupted
     */
    public void handlePOST(HttpServletRequest request) throws IOException, InterruptedException {
        String jsonString = request.getReader().lines().collect(Collectors.joining("\n")); // takes the request and stringafies it into a json structure

        if (jsonString.isBlank()) {
            // to ignore empty messages, seems like it can be solved by checking headers for push
            return;
        }
        ObjectMapper mapper = new ObjectMapper(); // maps JSON structure to existing class
        PushPayload payload = mapper.readValue(jsonString, PushPayload.class); // maps the JSON to the class PushPayload

        // --- Step 0: Prepare Github comms ---
        String[] strs = payload.repository.full_name.split("/");
        String owner = strs[0];
        String repoName = strs[1];
        String commitSha = payload.after;
        String context = "continuous integration";
        GithubUtils.CommitState commitState = GithubUtils.CommitState.PENDING;

        // --- Step 1. Clone the project ---
        RepoCloner cloner = new RepoCloner();
        String cloneUrl = payload.repository.clone_url;
        final Path REPO_DIR = ALL_REPOS_DIR.resolve(payload.repository.full_name);

        if (Files.isDirectory(REPO_DIR)) {
            new RepoCleanup().deleteRepo(REPO_DIR);
            System.out.println("INFO: Repo existed on disk; deleting it");
        }
        Files.createDirectories(REPO_DIR);
        cloner.runGitClone(cloneUrl, REPO_DIR);

        // --- Step 2: Check out affected branch ---
        String branch = payload.ref.substring(GithubUtils.BRANCH_PREFIX.length());
        BranchCheckout checkouter = new BranchCheckout();
        checkouter.checkoutBranch(REPO_DIR, branch);

        // --- Step 2.5: Set commit state to PENDING ---
        String description = "Done: Cloned and checked out affected branch.";
        try {
            handleCommitStatus(owner, repoName, commitSha, commitState, null, description, context);
        } catch (GithubCommitException e) {
            System.out.println("CI job failed when setting status to PENDING. Status: " + e.CI_STATUS + ". Stopping CI job.");
            new RepoCleanup().deleteRepo(REPO_DIR);
            return;
        }
        
        // --- Step 3: Build the project ---
        BuildResult buildResult = GradleBuildRunner.run(REPO_DIR);

        // Step 3.5: Update commit state description
        switch (buildResult.status) {
            case BuildResult.Status.SUCCESS:
                description = "Build succeeded";
                break;
            case BuildResult.Status.FAILURE:
                description = "Build failed";
                commitState = GithubUtils.CommitState.FAILURE;

                String timeNow = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                Path outputDir = ALL_BUILDS_DIR.resolve(payload.repository.full_name);
                Path outputPath = outputDir.resolve(timeNow + ".json");

                Files.createDirectories(outputDir);

                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));

                // Create log and add info
                ObjectMapper logMapper = new ObjectMapper();
                ObjectNode jsonObject = logMapper.createObjectNode();

                jsonObject.put("timestamp", timeNow);
                jsonObject.put("status", buildResult.status.toString());
                jsonObject.put("commitIdentifier", commitSha);
                jsonObject.put("log", buildResult.log);

                writer.write(jsonObject.toPrettyString());
                writer.close();
                break;

            case BuildResult.Status.ERROR:
                description = "Build error (couldn't finish build)";
                commitState = GithubUtils.CommitState.ERROR;
                break;
        }

        try {
            handleCommitStatus(owner, repoName, commitSha, commitState, null, description, context);
        } catch (GithubCommitException e) {
            System.out.println("CI job failed when setting status to " + commitState + ". Status: " + e.CI_STATUS + ". Stopping CI job.");
            new RepoCleanup().deleteRepo(REPO_DIR);
            return;
        }

        if (buildResult.status != BuildResult.Status.SUCCESS) {
            System.out.println("Build: Not success, returning.");
            return;
        }
        
        // --- Step 4: Test the project ---
        BuildResult testResult = TestRunner.runTests(REPO_DIR.toFile());
        switch (testResult.status) {
            case BuildResult.Status.SUCCESS:
                description = "All tests passed";
                commitState = GithubUtils.CommitState.SUCCESS;
                break;
            case BuildResult.Status.FAILURE:
                description = "Test(s) failed";
                commitState = GithubUtils.CommitState.FAILURE;
                break;
            case BuildResult.Status.ERROR:
                description = "Test error (couldn't finish tests)";
                commitState = GithubUtils.CommitState.ERROR;
                break;
        }
        try {
            handleCommitStatus(owner, repoName, commitSha, commitState, null, description, context);
            System.out.println("CI job finished successfully");
        } catch (GithubCommitException e) {
            System.out.println("CI job failed when setting status to " + commitState + ". Status: " + e.CI_STATUS + ". Stopping CI job.");
            new RepoCleanup().deleteRepo(REPO_DIR);
            return;
        }

        // --- Delete cloned repo from disk and link to build log
        new RepoCleanup().deleteRepo(REPO_DIR);
    }

    /**
     * Handle GET requests.
     *
     * @param target request target path
     * @param response servlet response
     * @throws IOException if writing the response fails
     */
    public void handleGET(String target, HttpServletResponse response) throws IOException {
    }

    /**
     * Updates the status of a specific commit on GitHub.
     * 
     * This method ensures a GitHub token is loaded (from {@code config.properties}) if it hasn't been set already,
     * then creates an {@link UpdateGithubStatus} instance to send the commit status update.
     *
     * @param owner GitHub repository owner 
     * @param repo GitHub repository name
     * @param sha Commit SHA to update the status for
     * @param state The state of the commit status; valid values: "error", "failure", "pending", or "success"
     * @param targetUrl Optional URL linking to more details about the status
     * @param description Optional short description of the status
     * @param context Optional context name to differentiate this status from others
     * @return {@code HttpResponse} containing the response from GitHub
     * @throws IOException If the GitHub token cannot be loaded or an I/O error occurs while sending the request
     * @throws InterruptedException If the HTTP request is interrupted
     */
    public void handleCommitStatus(String owner, 
                                   String repo, 
                                   String sha, 
                                   GithubUtils.CommitState state,
                                   String targetUrl,
                                   String description,
                                   String context) throws IOException, InterruptedException, GithubCommitException {
        
        if (token == null || token.isBlank()) {
            token = GithubUtils.loadToken(configFileName);
        }

        HttpResponse<String> response =  GithubUtils.updateStatus(token, owner, repo, sha, state, targetUrl, description, context);

        int ciStatus = response.statusCode();
        if (!(ciStatus == 200 || ciStatus == 201)) {
            throw new GithubCommitException(ciStatus);
        }
    }
}
