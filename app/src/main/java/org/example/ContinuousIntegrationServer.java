package org.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
 
import java.io.IOException;
import java.util.stream.Collectors;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.net.http.HttpResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.payload.PushPayload;

/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/
public class ContinuousIntegrationServer extends AbstractHandler
{
    private String configFileName = "config.properties";
    private String token; // Personal access token for GitHub
    
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
 
    public void handlePUT(HttpServletRequest request) throws IOException {
        // TODO: Can be removed if unused.
    }

    public void handlePOST(HttpServletRequest request) throws IOException, InterruptedException {
        String jsonString = request.getReader().lines().collect(Collectors.joining("\n")); // takes the request and stringafies it into a json structure
        ObjectMapper mapper = new ObjectMapper(); // maps JSON structure to existing class

        if (!jsonString.isBlank()) { // to ignore empty messages, seems like it can be solved by checking headers for push
            PushPayload payload = mapper.readValue(jsonString, PushPayload.class); // maps the JSON to the class PushPayload

            // Bellow is just example usage and for testing
            String branch = payload.ref.replace("refs/heads/", ""); // replace refs/heads/branchName with the just branchName
            String cloneUrl = payload.repository.clone_url;
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

    public void handleGET(String target, HttpServletResponse response) throws IOException {
    }
    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}