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

    private boolean handled;

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

        // handled = false;

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        if (!request.getHeader("X-GitHub-Event").equals("push")) {
            System.out.println("Got HTTP req but was not GitHub push");
            return;
        }

        switch (request.getMethod()) {
            case "PUT":
                handlePUT(request);
                break;

            case "POST":
                System.out.println("- - - - - - - - -  - POST START - - - - - - -");
                try {
                    handlePOST(request);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("- - - - - - - - -  - POST DONE - - - - - - -");
                break;

            case "GET":
                handleGET(target, response);
                break;

            default:
                break;
        }


        // handled = true;

        // System.out.println("HANDLE");
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

        final Path logDir = Utils.ALL_BUILDS_DIR.resolve(payload.repository.full_name);

        // --- Step 0: Prepare Github comms ---
        String[] strs = payload.repository.full_name.split("/");
        String owner = strs[0];
        String repoName = strs[1];
        String commitSha = payload.after;
        String context = "continuous integration";
        GithubUtils.CommitState commitState = GithubUtils.CommitState.PENDING;

        // --- Step 1. Clone the project ---

        System.out.println("----------- HttpHandler: Clone --------------");
        RepoCloner cloner = new RepoCloner();
        String cloneUrl = payload.repository.clone_url;
        final Path REPO_DIR = ALL_REPOS_DIR.resolve(payload.repository.full_name);

        if (Files.isDirectory(REPO_DIR)) {
            new RepoCleanup().deleteRepo(REPO_DIR);
            System.out.println("INFO: Repo existed on disk; deleting it");
        }
        Files.createDirectories(REPO_DIR);
        cloner.runGitClone(cloneUrl, REPO_DIR);
        System.out.println("----------- HttpHandler: Clone DONE --------------");

        // --- Step 2: Check out affected branch ---

        System.out.println("----------- HttpHandler: Checkout --------------");
        String branch = payload.ref.substring(GithubUtils.BRANCH_PREFIX.length());
        BranchCheckout checkouter = new BranchCheckout();
        checkouter.checkoutBranch(REPO_DIR, branch);
        System.out.println("----------- HttpHandler: Checkout DONE --------------");

        // --- Step 2.5: Set commit state to PENDING ---
        System.out.println("----------- HttpHandler: Set PENDING --------------");
        String description = "Done: Cloned and checked out affected branch.";
        try {
            handleCommitStatus(owner, repoName, commitSha, commitState, null, description, context);
        } catch (GithubCommitException e) {
            System.out.println("CI job failed when setting status to PENDING. Status: " + e.CI_STATUS + ". Stopping CI job.");
            new RepoCleanup().deleteRepo(REPO_DIR);
            return;
        }
        System.out.println("----------- HttpHandler: Set PENDING DONE --------------");
        
        // --- Step 3: Build the project ---
        System.out.println("----------- HttpHandler: Build --------------");
        CmdResult buildResult = Utils.execGradleCommandInRepo(REPO_DIR, "build", "-x", "test", "--no-daemon");

        // Step 3.5: Update commit state description
        String targetUrl = null;
        switch (buildResult.status) {
            case CmdResult.Type.SUCCESS:
                description = "Build succeeded";
                break;
            case CmdResult.Type.FAILURE:
                description = "Build failed";
                commitState = GithubUtils.CommitState.FAILURE;
                CmdResult emptyTestResult = new CmdResult(CmdResult.Type.NON_EXISTENT);
                Path filePath = Utils.saveLogToFile(logDir, buildResult, emptyTestResult, commitSha);
                targetUrl = "http://localhost:" + Main.PORT + "/" + filePath.toString();
                break;

            case CmdResult.Type.ERROR:
                description = "Build error (couldn't finish build)";
                commitState = GithubUtils.CommitState.ERROR;
                break;
            default:
                break;
        }

        try {
            handleCommitStatus(owner, repoName, commitSha, commitState, targetUrl, description, context);
        } catch (GithubCommitException e) {
            System.out.println("CI job failed when setting status to " + commitState + ". Status: " + e.CI_STATUS + ". Stopping CI job.");
            new RepoCleanup().deleteRepo(REPO_DIR);
            return;
        }

        if (buildResult.status != CmdResult.Type.SUCCESS) {
            System.out.println("Build: Not success, returning.");
            return;
        }
        System.out.println("----------- HttpHandler: Build DONE --------------");
        
        // --- Step 4: Test the project ---
        System.out.println("----------- HttpHandler: Test --------------");
        // CmdResult testResult = TestRunner.runTests(REPO_DIR.toFile());
        CmdResult testResult = Utils.execGradleCommandInRepo(REPO_DIR, "test");

        switch (testResult.status) {
            case CmdResult.Type.SUCCESS:
                description = "All tests passed";
                commitState = GithubUtils.CommitState.SUCCESS;
                break;
            case CmdResult.Type.FAILURE:
                description = "Test(s) failed";
                commitState = GithubUtils.CommitState.FAILURE;
                break;
            case CmdResult.Type.ERROR:
                description = "Test error (couldn't finish tests)";
                commitState = GithubUtils.CommitState.ERROR;
                break;
            default:
                break;
        }

        if (testResult.status != CmdResult.Type.ERROR) {
            Path filePath = Utils.saveLogToFile(logDir, buildResult, testResult, commitSha);
            targetUrl = "http://localhost:" + Main.PORT + "/" + filePath.toString();
        }

        try {
            handleCommitStatus(owner, repoName, commitSha, commitState, targetUrl, description, context);
            System.out.println("CI job finished successfully");
        } catch (GithubCommitException e) {
            System.out.println("CI job failed when setting status to " + commitState + ". Status: " + e.CI_STATUS + ". Stopping CI job.");
            new RepoCleanup().deleteRepo(REPO_DIR);
            return;
        }
        System.out.println("----------- HttpHandler: Test DONE --------------");

        // --- Delete cloned repo from disk and link to build log
        System.out.println("----------- HttpHandler: Remove repo --------------");
        new RepoCleanup().deleteRepo(REPO_DIR);
        System.out.println("----------- HttpHandler: Remove repo DONE --------------");
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
