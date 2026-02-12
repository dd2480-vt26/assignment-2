package org.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs a Gradle build (compile step) in a checked-out repository.
 *
 * This runner returns a status enum on completion so callers can handle outcome.
 */
public class GradleBuildRunner {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private static final int BUILD_TIMEOUT_MINUTES = 2;

    /**
     * Runs {@code gradlew build -x test --no-daemon} in the given repo directory.
     *
     * @param repoDir local repository directory (already cloned and checked out)
     * @return build status
     */
    public static BuildResult run(Path repoDir) {
        if (repoDir == null) {
            return new BuildResult(BuildResult.Status.ERROR);
        }
        if (!Files.isDirectory(repoDir)) {
            return new BuildResult(BuildResult.Status.ERROR);
        }

        Path wrapperPath = resolveWrapperPath(repoDir);
        if (!Files.isRegularFile(wrapperPath)) {
            return new BuildResult(BuildResult.Status.ERROR);
        }

        if (!IS_WINDOWS && !Files.isExecutable(wrapperPath)) {
            wrapperPath.toFile().setExecutable(true);
        }

        List<String> command = new ArrayList<>();
        if (IS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
        }
        command.add(wrapperPath.toAbsolutePath().toString());
        command.add("build");
        command.add("-x");
        command.add("test");
        command.add("--no-daemon");

        System.out.println("Repo: " + repoDir);
        System.out.println("Executing: " + String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoDir.toFile());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();

            int exitCode = process.waitFor();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            if (exitCode != 0) {
                BuildResult result = new BuildResult(BuildResult.Status.FAILURE, output.toString());
                return result;
            }
            
            BuildResult result = new BuildResult(BuildResult.Status.SUCCESS, output.toString());
            return result;
        } catch (IOException | InterruptedException ex) {
            BuildResult result = new BuildResult(BuildResult.Status.ERROR);
            return result;
        }
    }

    /**
     * Resolve the Gradle wrapper path for the repository.
     *
     * @param repoDir local repository directory
     * @return path to gradle wrapper file
     */
    private static Path resolveWrapperPath(Path repoDir) {
        return IS_WINDOWS ? repoDir.resolve("gradlew.bat") : repoDir.resolve("gradlew");
    }

}
