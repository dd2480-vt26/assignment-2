package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GradleBuildRunnerTest {

    /**
     * Positive test: Runs a build using a fake Gradle wrapper that exits with code 0.
     * Test case: Temporary repo directory contains a wrapper script printing "build ok".
     * Expected: run returns SUCCESS.
     */
    @Test
    void run_withValidWrapper_shouldReturnSuccess(@TempDir Path repoDir) throws Exception {
        createWrapper(repoDir, 0, "build ok");

        BuildResult.Status result = GradleBuildRunner.run(repoDir);

        assertEquals(BuildResult.Status.SUCCESS, result);
    }

    /**
     * Negative test: Runs a build using a fake Gradle wrapper that exits with a non zero code.
     * Test case: Temporary repo directory contains a wrapper script exiting with code 7.
     * Expected: run returns FAILURE.
     */
    @Test
    void run_withFailingWrapper_shouldReturnFailure(@TempDir Path repoDir) throws Exception {
        createWrapper(repoDir, 7, "build failed");

        BuildResult.Status result = GradleBuildRunner.run(repoDir);

        assertEquals(BuildResult.Status.FAILURE, result);
    }

    /**
     * Negative test: Missing Gradle wrapper file.
     * Test case: Repo directory exists but contains no gradlew/gradlew.bat.
     * Expected: run returns ERROR about missing wrapper.
     */
    @Test
    void run_withoutWrapper_shouldReturnError(@TempDir Path repoDir) {
        BuildResult.Status result = GradleBuildRunner.run(repoDir);

        assertEquals(BuildResult.Status.ERROR, result);
    }

    /**
     * Negative test: repoDir points to a file instead of a directory.
     * Test case: Temp file is passed as the repository path.
     * Expected: run returns ERROR.
     */
    @Test
    void run_withNonDirectory_shouldReturnError(@TempDir Path tempDir) throws Exception {
        Path repoFile = Files.createTempFile(tempDir, "repo", ".txt");

        BuildResult.Status result = GradleBuildRunner.run(repoFile);
        assertEquals(BuildResult.Status.ERROR, result);
    }

    /**
     * Negative test: repoDir is null.
     * Test case: Null input is passed.
     * Expected: run returns ERROR.
     */
    @Test
    void run_withNullRepoDir_shouldReturnError() {
        BuildResult.Status result = GradleBuildRunner.run(null);

        assertEquals(BuildResult.Status.ERROR, result);
    }

    /**
     * Create a minimal Gradle wrapper script with a fixed exit code and output.
     *
     * The real runner calls: gradlew build -x test --no-daemon
     * So the fake wrapper must ignore extra args and still exit with the given code.
     *
     * @param repoDir repository directory where the wrapper should be placed
     * @param exitCode exit code the wrapper should return
     * @param output output line printed by the wrapper
     * @return path to the created wrapper script
     * @throws IOException if writing the wrapper file fails
     */
    private Path createWrapper(Path repoDir, int exitCode, String output) throws IOException {
        Path wrapperPath = repoDir.resolve(isWindows() ? "gradlew.bat" : "gradlew");

        StringBuilder script = new StringBuilder();
        if (isWindows()) {
            script.append("@echo off").append("\r\n");
            if (output != null && !output.isBlank()) {
                script.append("echo ").append(output).append("\r\n");
            }
            script.append("exit /b ").append(exitCode).append("\r\n");
        } else {
            script.append("#!/usr/bin/env sh").append("\n");
            if (output != null && !output.isBlank()) {
                script.append("echo ").append(output).append("\n");
            }
            script.append("exit ").append(exitCode).append("\n");
        }

        Files.writeString(wrapperPath, script.toString(), StandardCharsets.UTF_8);
        if (!isWindows()) {
            wrapperPath.toFile().setExecutable(true);
        }
        return wrapperPath;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
