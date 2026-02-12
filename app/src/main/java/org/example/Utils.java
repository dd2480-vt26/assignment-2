package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Utils {

    protected static final Path LOGS_DIR = Path.of("logs");

    public static CmdResult execGradleCommandInRepo(Path repoDir, String... args) {

        if(repoDir == null || !Files.isDirectory(repoDir)) {
            // throw new FileNotFoundException("Repo dir \"" + repoDir + "\" not found");
            System.out.println("Can't exec \"" + Arrays.toString(args) + "\" because repoDir \"" + repoDir + "\" doesnt exist");
            return new CmdResult(CmdResult.Type.ERROR);
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String gradleCmd = isWindows ? "gradlew.bat" : "./gradlew";

        if (!isWindows) {
            File gradlew = repoDir.resolve("gradlew").toFile();
            if (gradlew.exists()) {
                gradlew.setExecutable(true);
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(gradleCmd);
        cmd.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(cmd);

        System.out.println("Executing command: " + String.join(" ", cmd));
        pb.directory(repoDir.toFile());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            CmdResult.Type status = (exitCode == 0) ? CmdResult.Type.SUCCESS : CmdResult.Type.FAILURE;
            CmdResult result = new CmdResult(status, output.toString());

            return result;

        } catch (IOException e) {
            System.out.println("ERRORHERE");
            return new CmdResult(CmdResult.Type.ERROR, e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("INTERRUPT ERROR");
            return new CmdResult(CmdResult.Type.ERROR, e.getMessage());
        }
    }

    protected static Path saveLogToFile(Path logDir, CmdResult buildResult, CmdResult testResult, String commitSha) throws IOException {
        String timeNow = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        // Path outputDir = ALL_BUILDS_DIR.resolve(payload.repository.full_name);
        Path filePath = logDir.resolve(timeNow + ".json");

        Files.createDirectories(logDir);

        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));

        // Create log and add info
        ObjectMapper logMapper = new ObjectMapper();
        ObjectNode jsonObject = logMapper.createObjectNode();

        jsonObject.put("timestamp", timeNow);
        jsonObject.put("commitIdentifier", commitSha);
        jsonObject.put("buildStatus", buildResult.status.toString());
        jsonObject.put("buildLog", buildResult.log);
        jsonObject.put("testStatus", testResult.status.toString());
        jsonObject.put("testLog", testResult.log);

        writer.write(jsonObject.toPrettyString());
        writer.close();

        return filePath;
    }
}
