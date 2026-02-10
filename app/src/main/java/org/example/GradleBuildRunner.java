package org.example;
import java.io.IOException;
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
    public static BuildResult.Status run(Path repoDir) {
        if (repoDir == null) {
            return BuildResult.Status.ERROR;
        }
        if (!Files.isDirectory(repoDir)) {
            return BuildResult.Status.ERROR;
        }

        Path wrapperPath = resolveWrapperPath(repoDir);
        if (!Files.isRegularFile(wrapperPath)) {
            return BuildResult.Status.ERROR;
        }

        if (!IS_WINDOWS && !Files.isExecutable(wrapperPath)) {
            wrapperPath.toFile().setExecutable(true);
        }

        List<String> command = new ArrayList<>();
        if (IS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
        }
        command.add(wrapperPath.toString());
        command.add("build");
        command.add("-x");
        command.add("test");
        command.add("--no-daemon");

        System.out.println("Repo: " + repoDir);
        System.out.println("Executing: " + String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoDir.toFile());
        builder.redirectErrorStream(true);
        Path outputPath = repoDir.resolve("gradle-build-output.log");
        builder.redirectOutput(outputPath.toFile());

        try {
            Process process = builder.start();

            boolean finished = process.waitFor(BUILD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return BuildResult.Status.ERROR;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return BuildResult.Status.FAILURE;
            }

            return BuildResult.Status.SUCCESS;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return BuildResult.Status.ERROR;
        } catch (IOException ex) {
            return BuildResult.Status.ERROR;
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
