package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepoCleanupTest {

    @TempDir
    Path tempDir;

    /**
     * Positive test: Delete a directory that contains files.
     * Test case: A simple repository folder with a file inside.
     * Expected: Directory and its contents are fully deleted.
     */
    @Test
    void deleteSimpleDirectory() throws IOException {
        Path repoDir = tempDir.resolve("repo");
        Files.createDirectories(repoDir);
        Files.createFile(repoDir.resolve("file.txt"));

        RepoCleanup cleanup = new RepoCleanup();
        cleanup.deleteRepo(repoDir);

        assertFalse(Files.exists(repoDir), "Directory should be deleted");
    }

    /**
     * Negative test: Passing null as repository path.
     * Test case: {@code repoDir} is null.
     * Expected: {@code deleteRepo} is throws.
     */
    @Test
    void deleteNullPath_throws() {
        RepoCleanup cleanup = new RepoCleanup();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cleanup.deleteRepo(null));
        assertEquals("repoDir cannot be null", ex.getMessage());
    }

    /**
     * Negative test: Repository directory does not exist.
     * Test case: {@code repoDir} points to a non existent directory.
     * Expected: {@code deleteRepo} is throws.
     */
    @Test
    void deleteNonExistentDirectory_throws() {
        Path missingDir = tempDir.resolve("missing");
        RepoCleanup cleanup = new RepoCleanup();
        IOException ex = assertThrows(IOException.class, () -> cleanup.deleteRepo(missingDir));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    /**
     * Negative test: Path exists but is a file, not a directory.
     * Test case: {@code repoDir} points to a file instead of a folder.
     * Expected: {@code deleteRepo} is throws.
     */
    @Test
    void deleteFilePath_throws() throws IOException {
        Path filePath = tempDir.resolve("file.txt");
        Files.createFile(filePath);
        RepoCleanup cleanup = new RepoCleanup();
        IOException ex = assertThrows(IOException.class, () -> cleanup.deleteRepo(filePath));
        assertTrue(ex.getMessage().contains("not a directory"));
    }
}