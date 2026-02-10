package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepoClonerTest {

    @TempDir
    Path tempDir;

    /**
     * Negative test: If a repository folder already contains a {@code .git} directory,
     * {@code ensureRepoCloned} should skip cloning.
     * Test case: Workspace has {@code user/repo/.git} created beforehand.
     * Expected: {@code runGitClone} is not called.
     */
    @Test
    void repoAlreadyExists_cloneSkipped() throws IOException {
        Path workspaceRoot = tempDir.resolve("workspace");
        Path repoDir = workspaceRoot.resolve("user").resolve("repo");
        Files.createDirectories(repoDir.resolve(".git"));

        TestableRepoCloner cloner = new TestableRepoCloner();
        assertDoesNotThrow(() -> cloner.ensureRepoCloned(workspaceRoot, "https://example.com/repo.git", "user/repo"));
        assertFalse(cloner.cloneCalled, "clone should not be called when repo exists");
    }

    /**
     * Positive test: If the repository folder is missing,
     * {@code ensureRepoCloned} should invoke cloning.
     * Test case: Workspace does not have {@code user/repo} yet.
     * Expected: {@code runGitClone} is called and {@code .git} exists afterward.
     */
    @Test
    void repoMissing_cloneRuns() throws IOException {
        Path workspaceRoot = tempDir.resolve("workspace");

        TestableRepoCloner cloner = new TestableRepoCloner();
        assertDoesNotThrow(() -> cloner.ensureRepoCloned(workspaceRoot, "https://example.com/repo.git", "user/repo"));
        assertTrue(cloner.cloneCalled, "clone should be called when repo is missing");
        assertTrue(Files.exists(workspaceRoot.resolve("user").resolve("repo").resolve(".git")));
    }

    /**
     * Negative test: If the repository name is invalid,
     * {@code ensureRepoCloned} should reject it.
     * Test case: Repo name contains invalid characters.
     * Expected: {@code ensureRepoCloned} throws.
     */
    @Test
    void invalidRepoName_throws() {
        TestableRepoCloner cloner = new TestableRepoCloner();
        assertThrows(IOException.class,
                () -> cloner.ensureRepoCloned(tempDir, "https://example.com/repo.git", "user/repo!"));
    }

    /**
     * Negative test: If the repository name is blank,
     * {@code ensureRepoCloned} should reject it.
     * Test case: Repo name is blank.
     * Expected: {@code ensureRepoCloned} throws.
     */
    @Test
    void blankRepoName_throws() {
        TestableRepoCloner cloner = new TestableRepoCloner();
        assertThrows(IllegalArgumentException.class,
                () -> cloner.ensureRepoCloned(tempDir, "https://example.com/repo.git", " "));
    }

    /**
     * Negative test: If the clone URL is blank,
     * {@code ensureRepoCloned} should reject it.
     * Test case: Clone URL is blank.
     * Expected: {@code ensureRepoCloned} throws.
     */
    @Test
    void blankCloneUrl_throws() {
        TestableRepoCloner cloner = new TestableRepoCloner();
        assertThrows(IllegalArgumentException.class,
                () -> cloner.ensureRepoCloned(tempDir, "  ", "user/repo"));
    }

    private static class TestableRepoCloner extends RepoCloner {
        boolean cloneCalled = false;

        /**
         * Test hook: Simulate a successful clone without invoking real Git.
         * Expected: Mark {@code cloneCalled} true and create a {@code .git} directory.
         */
        @Override
        protected void runGitClone(String cloneUrl, Path targetDir) throws IOException {
            cloneCalled = true;
            Files.createDirectories(targetDir.resolve(".git"));
        }
    }
}
