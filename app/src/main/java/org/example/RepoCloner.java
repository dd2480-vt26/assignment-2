package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for ensuring a repository is cloned locally.
 */
public class RepoCloner {
    private static final String DEFAULT_WORKSPACE_RELATIVE = "workspace/repos";

    /**
     * Resolve workspace root from system property or environment variable.
     *
     * @return workspace root path
     */
    public static Path resolveWorkspaceRoot() {
        String fromProp = System.getProperty("ci.workspace");
        if (fromProp != null && !fromProp.isBlank()) {
            return Paths.get(fromProp);
        }
        String fromEnv = System.getenv("CI_WORKSPACE");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Paths.get(fromEnv);
        }
        return Paths.get(DEFAULT_WORKSPACE_RELATIVE);
    }

    /**
     * Ensure a repository is cloned locally if already exists and contains .git do nothing.
     *
     * @param workspaceRoot base workspace path
     * @param cloneUrl clone URL
     * @param repoName repository name (for instance owner/repo)
     * @return path to local repository
     * @throws IOException if cloning fails or target exists but is not a git repo
     */
    public Path ensureRepoCloned(Path workspaceRoot, String cloneUrl, String repoName) throws IOException {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }
        if (cloneUrl == null || cloneUrl.isBlank()) {
            throw new IllegalArgumentException("cloneUrl cannot be blank");
        }
        if (repoName == null || repoName.isBlank()) {
            throw new IllegalArgumentException("repoName cannot be blank");
        }

        Path safeRepoDir = resolveSafeRepoDir(workspaceRoot, repoName);
        Files.createDirectories(workspaceRoot);

        if (!Files.exists(safeRepoDir)) {
            Files.createDirectories(safeRepoDir.getParent());
            runGitClone(cloneUrl, safeRepoDir);
            return safeRepoDir;
        }

        if (isGitRepository(safeRepoDir)) {
            return safeRepoDir;
        }

        throw new IOException("Repository directory exists but is not a git repository: " + safeRepoDir);
    }

    /**
     * Run a shallow {@code git clone} into the target directory.
     *
     * @param cloneUrl clone URL
     * @param targetDir target directory for the repository
     * @throws IOException if the git command fails or is interrupted
     */
    protected void runGitClone(String cloneUrl, Path targetDir) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("clone");
        command.add(cloneUrl);
        command.add(targetDir.toString());

        System.out.println("Executing: " + String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
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
                throw new IOException("git clone failed with exit code " + exitCode);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("git clone interrupted", ex);
        }
    }

    /**
     * Check whether the given directory looks like a git repository.
     *
     * @param repoDir repository directory
     * @return true when a {@code .git} directory exists
     */
    private boolean isGitRepository(Path repoDir) {
        Path gitPath = repoDir.resolve(".git");
        return Files.exists(gitPath);
    }

    /**
     * Resolve a safe repository path under the workspace by validating name parts.
     *
     * @param workspaceRoot workspace root
     * @param repoName repository name (for instance owner/repo)
     * @return normalized repository path under the workspace
     * @throws IOException if the repo name is invalid
     */
    private Path resolveSafeRepoDir(Path workspaceRoot, String repoName) throws IOException {
        if (repoName.contains("\\") || repoName.contains(":")) {
            throw new IOException("Invalid repository name: " + repoName);
        }
        String[] segments = repoName.split("/");
        List<String> safeSegments = new ArrayList<>();
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IOException("Invalid repository name part: " + repoName);
            }
            if (!segment.matches("[A-Za-z0-9._-]+")) {
                throw new IOException("Invalid repository name part: " + repoName);
            }
            safeSegments.add(segment);
        }
        Path resolved = workspaceRoot;
        for (String segment : safeSegments) {
            resolved = resolved.resolve(segment);
        }
        return resolved.normalize();
    }
}
