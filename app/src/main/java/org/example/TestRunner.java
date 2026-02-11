package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;


/**
 * Service responsible for running project tests.
 * This class executes Gradle tests in a given directory and captures the results.
 */

public class TestRunner {

    /**
     * Runs the tests in the specified project directory.
     * @param projectDirectory The root directory of the project.
     * @return a BuildResult object containing the status and execution logs.
     */

    public static BuildResult runTests(File projectDirectory) {
        if(projectDirectory == null || !projectDirectory.exists() || !projectDirectory.isDirectory()) {
            BuildResult result = new BuildResult(BuildResult.Status.ERROR);
            result.errorMessage = "Project directory does not exist: " + (projectDirectory != null ? projectDirectory.getAbsolutePath() : "null");
            return result;
        }

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String gradleCmd = isWindows ? "gradlew.bat" : "./gradlew";

            if (!isWindows) {
                File gradlew = new File(projectDirectory, "gradlew");
                if (gradlew.exists()) {
                    gradlew.setExecutable(true);
                }
            }

            ProcessBuilder pb = new ProcessBuilder(gradleCmd, "test");
            pb.directory(projectDirectory);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            BuildResult.Status status = (exitCode == 0) ? BuildResult.Status.SUCCESS : BuildResult.Status.FAILURE;
            BuildResult result = new BuildResult(status);
            result.logs = output.toString();

            return result;

        } catch (Exception e) {
            BuildResult result = new BuildResult(BuildResult.Status.ERROR);
            result.errorMessage = "Exception running tests: " + e.getMessage();
            result.logs = e.getMessage();
            return result;
        }
    }

}