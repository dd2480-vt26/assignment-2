package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GithubTokenProviderTest {
    
    @TempDir
    Path tempDir;

    /**
     * Positive test: {@code loadToken} returns the token when the config file contains a valid value.
     * Test case: config.properties contains {@code GITHUB_TOKEN=dummy-token}.
     * Expected: The returned token equals {@code dummy-token}.
     */
    @Test
    void loadToken_shouldReturnTokenWhenPresent() throws IOException {
        GithubTokenProvider tokenProvider = new GithubTokenProvider();

        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "GITHUB_TOKEN=dummy-token\n");

        String token = tokenProvider.loadToken(configFile.toString());

        assertEquals("dummy-token", token);
    }

    /**
     * Negative test: {@code loadToken} throws an exception when the token is missing.
     * Test case: config.properties does not contain {@code GITHUB_TOKEN}.
     * Expected: The method throws an {@code IOException}.
     */
    @Test
    void loadToken_shouldThrowWhenTokenMissing() throws IOException {
        GithubTokenProvider tokenProvider = new GithubTokenProvider();

        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "GITHUB_TOKEN=\n");

        assertThrows(IOException.class, () -> tokenProvider.loadToken(configFile.toString()));
    }
}
