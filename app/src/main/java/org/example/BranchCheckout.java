package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for checking out a branch in the git repository.
 */
public class BranchCheckout {

    /**
     * Ensures all inputs are valid, that the repo exists and runs checkout command.
     *
     * @param repoDir the directory of the repository
     * @param branchName the name of the branch to which the commit was made
     * @throws IOException if checkout fails
     */
    public void checkoutBranch(Path repoDir, String branchName) throws IOException {
        validateInputs(repoDir, branchName);
        ensureGitRepo(repoDir);
        runGitCheckout(repoDir, branchName);
    }

    /**
     * Run {@code git checkout <branch>} in the target directory in order to switch to that branch.
     *
     * @param repoDir the directory of the repository
     * @param branchName the name of the branch to which the commit was made
     * @throws IOException if the git command fails or is interrupted
     */
    protected void runGitCheckout(Path repoDir, String branchName) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("checkout");
        command.add(branchName);

        System.out.println("Executing: " + String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("git checkout failed with exit code " + exitCode);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("git checkout interrupted", ex);
        }
    }

    /**
     * Check whether the given directory looks like a git repository.
     *
     * @param repoDir repository directory
     * @throws IOException if {@code repoDir} is not a git repository
     */
    private void ensureGitRepo(Path repoDir) throws IOException {
        // same logic as isGitRepository, but throwing exception
        Path gitPath = repoDir.resolve(".git");
        if(!Files.exists(gitPath)){
            throw new IOException("Directory is not a git repository: " + repoDir);
        }
    }

    /**
     * Checks if the given directory or the branch name are valid.
     *
     * @param repoDir repository directory
     * @param branchName name of the branch
     * @throws IllegalArgumentException if {@code branchName} or {@code repoDir} are invalid inputs.
     */
    private void validateInputs(Path repoDir, String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("branchName cannot be blank");
        }
        if (repoDir == null) {
            throw new IllegalArgumentException("repoDir cannot be null");
        }
    }
}