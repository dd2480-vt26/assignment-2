package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for handling HTTP GET requests for build logs.
 * <p>
 * Features:
 *   - Serve an HTML that lists all build logs as clickable URLs.
 *   - Serve an HTML that shows details of a specific build.
 * The response is written directly to a {@link HttpServletResponse}.
 * </p>
 */
public class GetRequestHandler {

    /**
     * Handles a GET request for build logs.
     * <p>
     * If {@code target} matches {@link #BUILD_LOG_ROUTE}, it lists available build logs .
     * If {@code target} starts with {@link #BUILD_LOG_ROUTE} and the log exists on disk, it displays that log.
     * Otherwise, it returns an "Invalid GET request" message.
     * 
     * Examples:
     *   - "localhost:8080/custom-build-logs"
     *   - "localhost:8080/custom-build-logs/2026-02-09T13:17:20+01:00.json"
     * </p>
     * @param target the URL path requested by the client  
     * @param response the HttpServletResponse to write HTML output to
     * @throws IOException if writing to the response fails
     */
    public static void handle(String target, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style=\"font-family: monospace\">
        """);

        target = target.replaceAll("^/", "");       // Remove leading slashes
        Path targetAsPath = Path.of(target);

        if (!targetAsPath.startsWith(Utils.LOGS_DIR) || targetAsPath.toString().contains("..")) {
            sb.append("<p>Invalid GET request</p>");
        }
        else if (Files.isDirectory(targetAsPath)) {
            handleListAllBuilds(targetAsPath, sb);
        }
        else if (Files.isRegularFile(targetAsPath)) {
            handleListSpecificBuild(targetAsPath, sb);
        }
        else {
            sb.append("<p>Invalid GET request</p>");
        }

        sb.append("""
            </body>
            </html>     
        """);

        response.getWriter().print(sb);
    }

    /**
     * Generates an HTML list of all build logs in the {@link #BUILD_LOG_DIR} directory.
     * <p>
     * Each build log is a clickable link to its specific URL.
     * </p>
     * @param sb the StringBuilder to append HTML content to
     */
    private static void handleListAllBuilds(Path dir, StringBuilder sb) {
        sb.append("<h1>All Build Logs</h1>");
        
        System.out.println("DIR: " + dir);

        File dirFile = dir.toFile();
        File[] files = dirFile.listFiles();

        if (files == null) {
            return;
        }
        else if (files.length == 0) {
            sb.append("(empty)");
        }
        else {
            Arrays.sort(files);
            sb.append("<ul>");
            for (File file : files) {
                if (file.isFile()) {
                    String fileNameEscaped = StringEscapeUtils.escapeHtml4(file.getName());
                    sb.append("<li><a href=\"").append(fileNameEscaped).append("\">")
                        .append(fileNameEscaped)
                        .append("</a></li>");
                }
            }
            sb.append("</ul>");
        }
    }

    /**
     * Generates HTML content showing details of a specific build log.
     * <p>
     * Resolves the file based on the {@code target} path,
     * reads it as a {@code BuildLog} JSON object, and
     * displays the JSON content.
     * </p>
     * @param sb the StringBuilder to append HTML content to
     * @param path the URL path pointing to the specific build log
     */
    private static void handleListSpecificBuild(Path path, StringBuilder sb) {
        String fileNameEscaped = StringEscapeUtils.escapeHtml4(path.toString());
        
        LogInfo logInfo;
        try {
            logInfo = new ObjectMapper().readValue(path.toFile(), LogInfo.class);
        } catch (IOException e) {
            e.printStackTrace();
            sb.append("<p>Error reading build log \"").append(fileNameEscaped).append("\"</p>");
            return;
        }

        String logCss = "background-color: #f4f4f4; white-space: pre-wrap; word-wrap: break-word";

        sb.append("<h1>Log Info: ").append(fileNameEscaped).append("</h1>");
        sb.append("<ul>");
        sb.append("  <li>timestamp: ").append(logInfo.timestamp).append("</li>");
        sb.append("  <li>commitIdentifier: ").append(logInfo.commitIdentifier).append("</li>");
        sb.append("</ul>");
        
        sb.append("<h2>Build</h2>");
        sb.append("<ul>");
        sb.append("  <li>buildStatus: ").append(logInfo.buildStatus).append("</li>");
        sb.append("</ul>");
        sb.append("<pre style=\"").append(logCss).append("\">").append(StringEscapeUtils.escapeHtml4(logInfo.buildLog)).append("</pre>");

        sb.append("<h2>Test</h2>");
        sb.append("<ul>");
        sb.append("  <li>testStatus: ").append(logInfo.testStatus).append("</li>");
        sb.append("</ul>");
        sb.append("<pre style=\"").append(logCss).append("\">").append(StringEscapeUtils.escapeHtml4(logInfo.testLog)).append("</pre>");
    }

}
