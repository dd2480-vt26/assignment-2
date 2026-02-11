package org.example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * Utility class for interacting with the GitHub REST API.
 *
 * This class provides helper methods for:
 * Loading a GitHub Personal Access Token (PAT) from a configuration file
 * Building JSON payloads and URIs for the GitHub commit status endpoint
 * Creating and sending HTTP requests to update commit statuses
 *
 * Commit states are represented using the {@code CommitState} enum to ensure that only valid
 * GitHub API status values can be used.
 */
public class GithubUtils {

    /**
     * Represents the possible commit status states supported by the GitHub API.
     */
    public enum CommitState {
        ERROR("error"),
        FAILURE("failure"),
        PENDING("pending"),
        SUCCESS("success");

        private final String state;

        CommitState(String state) {
            this.state = state;
        }

        public String getStateString() {
            return state;
        }
    }
    
    /**
     * Builds the JSON body for a commit status request.
     *
     * @param state The state of the commit status. Valid values: "error", "failure", "pending", "success".
     * @param targetUrl Optional URL linking to more details about the status
     * @param description Optional short description of the status
     * @param context Optional context name to differentiate this status from others
     * @return A JSON string representing the commit status payload
     */
    public static String buildJsonBody(CommitState state, String targetUrl, String description, String context) {
        String jsonBody = String.format("""
            {
                "state": "%s",
                "target_url": "%s",
                "description": "%s",
                "context": "%s"
            }
            """, 
            state.getStateString(),
            targetUrl != null ? targetUrl : "",
            description != null ? description : "",
            context != null ? context : ""
        );

        return jsonBody;
    }

    /**
     * Builds the URI for sending a commit status request to GitHub.
     *
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @param sha Commit SHA to update the status for
     * @return URI object pointing to the commit status endpoint for the given repository and commit
     */
    public static URI buildURI(String owner, String repo, String sha) {
        return URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/statuses/" + sha);
    }

    /**
     * Builds an {@link HttpRequest} object for updating a commit status.
     *
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @param sha Commit SHA to update the status for
     * @param jsonBody JSON payload to send as the request body
     * @return {@link HttpRequest} object ready to be sent to GitHub
     */
    public static HttpRequest buildRequest(String token, String owner, String repo, String sha, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(buildURI(owner, repo, sha))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + token)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();   

        return request;
    }

    /**
     * Sends a commit status update to GitHub.
     *
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @param sha Commit SHA to update the status for
     * @param state The state of the commit status ("pending", "success", "failure" or "error")
     * @param targetUrl Optional URL linking to more details about the status
     * @param description Optional short description of the status
     * @param context Optional context name to differentiate this status from others
     * @return {@link HttpResponse} containing the response from GitHub
     * @throws IOException if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public static HttpResponse<String> updateStatus(String token,
                             String owner, 
                             String repo, 
                             String sha, 
                             CommitState state, 
                             String targetUrl,
                             String description,
                             String context) 
                             throws IOException, InterruptedException {

        String jsonBody = buildJsonBody(state, targetUrl, description, context);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = buildRequest(token, owner, repo, sha, jsonBody);

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Retrieves the Personal Access Token (PAT) specified in the file {@code configFile} 
     * 
     * @param configFile the config file
     * @return the PAT
     * @throws IOException if the token isn't set 
     * @throws FileNotFoundException if the config file doesn't exist
     */
    public static String loadToken(String configFile) throws IOException, FileNotFoundException {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            System.out.println("The config file was not found");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String t = props.getProperty("GITHUB_TOKEN");
        if (t == null || t.isBlank()) {
            throw new IOException("GITHUB_TOKEN is not set in config.properties");
        }
        return t;
    }
}
