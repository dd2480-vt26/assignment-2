package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GithubUtilsTest {
    @TempDir
    Path tempDir;

    /**
     * Positive test: {@code loadToken} returns the token when the config file contains a valid value.
     * Test case: config.properties contains {@code GITHUB_TOKEN=dummy-token}.
     * Expected: The returned token equals {@code dummy-token}.
     */
    @Test
    void loadToken_shouldReturnTokenWhenPresent() throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "GITHUB_TOKEN=dummy-token\n");

        String token = GithubUtils.loadToken(configFile.toString());

        assertEquals("dummy-token", token);
    }

    /**
     * Negative test: {@code loadToken} throws an exception when the token is missing.
     * Test case: config.properties does not contain {@code GITHUB_TOKEN}.
     * Expected: The method throws an {@code IOException}.
     */
    @Test
    void loadToken_shouldThrowWhenTokenMissing() throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "GITHUB_TOKEN=\n");

        assertThrows(IOException.class, () -> GithubUtils.loadToken(configFile.toString()));
    }

    /**
     * Positive test: {@code buildJsonBody} builds JSON with all fields specified.
     * Test case: JSON with all fields specified.
     * Expected: The JSON string contains the state, target_url, description, and context as provided.
     */
    @Test
    void buildJsonBody_shouldContainStateURlDescriptionAndContext() {
        String json = GithubUtils.buildJsonBody(GithubUtils.CommitState.SUCCESS,
                                            "https://example.com", 
                                            "Tests passed", 
                                            "ci/test");

        assertTrue(json.contains("\"state\": \"success\""));
        assertTrue(json.contains("\"target_url\": \"https://example.com\""));
        assertTrue(json.contains("\"description\": \"Tests passed\""));
        assertTrue(json.contains("\"context\": \"ci/test\""));
    }

    /**
     * Positive test: {@code buildJsonBody} builds JSON with empty fields for targetUrl, description, and context.
     * Test case: JSON with null fields.
     * Expected: The JSON string contains empty strings for target_url, description, and context.
     */
    @Test
    void buildJsonBody_fieldsLeftEmptyWhenNotSpecified() {
        String json = GithubUtils.buildJsonBody(GithubUtils.CommitState.SUCCESS,
                                            null, 
                                            null, 
                                            null);

        assertTrue(json.contains("\"state\": \"success\""));
        assertTrue(json.contains("\"target_url\": \"\""));
        assertTrue(json.contains("\"description\": \"\""));
        assertTrue(json.contains("\"context\": \"\""));
    }

    /**
     * Positive test: {@code buildURI} builds URI for a given owner, repo, and commit SHA.
     * Test case: {@code owner = dd2480-vt26}, {@code repo = assignment-2} and {@code sha = abc123}.
     * Expected: The URI matches the GitHub API endpoint format.
     */
    @Test 
    void buildURI_shouldBeCorrect() {
        URI uri = GithubUtils.buildURI("dd2480-vt26", "assignment-2", "abc123");

        assertEquals("https://api.github.com/repos/dd2480-vt26/assignment-2/statuses/abc123", uri.toString());
    }

    /**
     * Positive test: {@code buildRequest} builds HttpRequest with correct URI, method, and headers.
     * Test case: Construct a POST request for GitHub commit status with a given owner, repo, SHA, token, and JSON body.
     * Expected: The request uses POST, has the correct Authorization, Accept, and X-GitHub-Api-Version headers, and the URI is correct.
     */
    @Test
    void buildRequest_shouldHaveCorrectFields() {
        String token = "dummy-token";
        GithubUtils updater = new GithubUtils(token);

        String owner = "dd2480-vt26";
        String repo = "assignment-2";
        String sha = "abc123";
        String jsonBody = "{ \"state\": \"success\" }";

        HttpRequest request = updater.buildRequest(owner, repo, sha, jsonBody);

        // Check that URI is correct
        assertEquals("https://api.github.com/repos/dd2480-vt26/assignment-2/statuses/abc123",
                     request.uri().toString());

        // Check that the method used is POST
        assertEquals("POST", request.method());

        // Check correct headers
        assertTrue(request.headers().firstValue("Authorization").isPresent());
        assertEquals("Bearer " + token, request.headers().firstValue("Authorization").get()); // Use get() since we require authorization
        assertEquals("application/vnd.github+json",
                     request.headers().firstValue("Accept").orElse(""));
        assertEquals("2022-11-28",
                     request.headers().firstValue("X-GitHub-Api-Version").orElse(""));
    }
}
