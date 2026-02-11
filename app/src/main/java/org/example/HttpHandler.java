package org.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        String branch = payload.ref.replace("^refs/heads/", "");
        BranchCheckout checkouter = new BranchCheckout();
        checkouter.checkoutBranch(REPO_DIR, branch);

        // --- Step 3: Build the project ---
        GradleBuildRunner.run(REPO_DIR);

        // Bellow is just example usage and for testing
        String repoName = payload.repository.full_name;
        String commitSha = payload.after;

        System.out.println("branch: " + branch);
        System.out.println("clone URL: " + cloneUrl);
        System.out.println("repository: " + repoName);
        System.out.println("commit sha: " + commitSha);

        GithubUtils.CommitState status = GithubUtils.CommitState.FAILURE; // placeholder
        String owner = repoName.split("/")[0];
        String repo = repoName.split("/")[1];
        String targetUrl = ""; // placeholder, should be URL to the build
        String description = ""; // placeholder, optional description of the status
        String context = ""; // placeholder, optional context name

        HttpResponse<String> githubResponse = handleCommitStatus(owner, repo, commitSha, status, targetUrl, description, context);
        int ci_status = githubResponse.statusCode();
        if(ci_status == 201) {
            System.out.println("CI job done");
        } else {
            System.out.println("CI job failed. Status: " + ci_status);
        }
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
    public HttpResponse<String> handleCommitStatus(String owner, 
                                   String repo, 
                                   String sha, 
                                   GithubUtils.CommitState state,
                                   String targetUrl,
                                   String description,
                                   String context) throws IOException, InterruptedException {
        
        if (token == null || token.isBlank()) {
            token = GithubUtils.loadToken(configFileName);
        }

        return GithubUtils.updateStatus(token, owner, repo, sha, state, targetUrl, description, context);
    }
}
