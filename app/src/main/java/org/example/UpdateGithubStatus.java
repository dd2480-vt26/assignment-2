package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Class for updating the status of a commit on GitHub using the REST API.
 * 
 * This class allows building the JSON payload, constructing the HTTP request, 
 * and sending it to GitHub to set commit statuses such as "pending", "success", or "failure".
 */
public class UpdateGithubStatus {
    private final String token; // Personal access token for GitHub

    /**
     * Constructs an {@code UpdateGithubStatus} instance with a GitHub personal access token.
     *
     * @param token GitHub personal access token with "Commit statuses" read and write permission
     */
    public UpdateGithubStatus(String token) {
        this.token = token;
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
    public String buildJsonBody(String state, String targetUrl, String description, String context) {
        
        // If state is something other than error, failure, pending or success
        if (!state.equals("error") && !state.equals("failure") 
            && !state.equals("pending") && !state.equals("success")) {
            throw new IllegalArgumentException("Invalid state: " + state + 
                ". Must be one of: error, failure, pending, success");
        }

        String jsonBody = String.format("""
            {
                "state": "%s",
                "target_url": "%s",
                "description": "%s",
                "context": "%s"
            }
            """, 
            state,
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
    public URI buildURI(String owner, String repo, String sha) {
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
    public HttpRequest buildRequest(String owner, String repo, String sha, String jsonBody) {
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
    public HttpResponse<String> updateStatus(String owner, 
                             String repo, 
                             String sha, 
                             String state, 
                             String targetUrl,
                             String description,
                             String context) 
                             throws IOException, InterruptedException {

        String jsonBody = buildJsonBody(state, targetUrl, description, context);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = buildRequest(owner, repo, sha, jsonBody);

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
