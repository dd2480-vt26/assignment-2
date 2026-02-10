package org.example;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility for deleting the locally cloned repository.
 */
public class RepoCleanup {

    /**
     * Ensures input is valid, that the directory exists and triggers deletion of directory.
     *
     * @param repoDir the directory of the repository
     * @throws IOException if delition fails
     */
    public void deleteRepo(Path repoDir) throws IOException {
        validateInput(repoDir);
        ensureDirectoryExists(repoDir);
        deleteRecursively(repoDir);
    }

    /**
     * Checks if the given directory is valid.
     *
     * @param repoDir the directory of the repository
     * @throws IllegalArgumentException if {@code repoDir} is invalid.
     */
    private void validateInput(Path repoDir) {
        if (repoDir == null) {
            throw new IllegalArgumentException("repoDir cannot be null");
        }
    }

    /**
     * Ensures that the given directory exists and is a directory
     * 
     * @param repoDir the directory of the repository
     * @throws IOException if {@code repoDir} dosnt exist or isnt a directory
     */
    private void ensureDirectoryExists(Path repoDir) throws IOException {
        if (!Files.exists(repoDir)) {
            throw new IOException("Repository directory does not exist: " + repoDir);
        }
        if (!Files.isDirectory(repoDir)) {
            throw new IOException("Path is not a directory: " + repoDir);
        }
    }

    /**
     * Deletes fist the files in the directory than the directory itself, does it recursievly.
     * inspiration for this found at: https://stackoverflow.com/questions/779519/delete-directories-recursively-in-java
     * @param repoDir the directory of the repository.
     * @throws IOException if delition of files or directory fails.
     */
    protected void deleteRecursively(Path repoDir) throws IOException {
        Files.walkFileTree(repoDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
