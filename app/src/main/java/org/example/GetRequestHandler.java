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

    private static final String BUILD_LOG_ROUTE = "/custom-build-logs";
    private static final Path BUILD_LOG_DIR = Path.of("custom-build-logs");       // Note: Placed in project root (not repo root, not src)

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
        """);
        String bodyCss = "font-family: monospace";
        sb.append("<body style=\"").append(bodyCss).append("\">");

        target = normalizeURL(target);

        if (target.equals(BUILD_LOG_ROUTE)) {
            handleListAllBuilds(sb);
        }
        else if (target.startsWith(BUILD_LOG_ROUTE)) {
            handleListSpecificBuild(sb, target);
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
     * Normalizes a URL path by replacing multiple consecutive slashes with
     * a single slash and removing trailing slashes.
     * @param str the URL path to normalize
     * @return a normalized string
     */
    private static String normalizeURL(String str) {       
        if (str == null || str.isEmpty()) {
            return str;
        }
        str = str.replaceAll("/+", "/");
        if (str.charAt(str.length()-1) == '/') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * Generates an HTML list of all build logs in the {@link #BUILD_LOG_DIR} directory.
     * <p>
     * Each build log is a clickable link to its specific URL.
     * </p>
     * @param sb the StringBuilder to append HTML content to
     */
    private static void handleListAllBuilds(StringBuilder sb) {
        sb.append("<h1>All Build Logs</h1>");
        
        File dir = BUILD_LOG_DIR.toFile();
        File[] files = dir.listFiles();

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
                    sb.append("<li><a href=\"")
                        .append(BUILD_LOG_ROUTE).append("/").append(fileNameEscaped)
                        .append("\">")
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
     * @param target the URL path pointing to the specific build log
     */
    private static void handleListSpecificBuild(StringBuilder sb, String target) {
        String _fileName = target.substring(BUILD_LOG_ROUTE.length() + 1);       // Extract content after prefix
        String fileNameEscaped = StringEscapeUtils.escapeHtml4(_fileName);

        Path filePath = BUILD_LOG_DIR.resolve(_fileName);

        if (!Files.exists(filePath)) {
            sb.append("<p>Build \"").append(fileNameEscaped).append("\" doesn't exist</p>");
            return;
        }
        
        BuildLog buildLog;
        try {
            buildLog = new ObjectMapper().readValue(filePath.toFile(), BuildLog.class);
        } catch (IOException e) {
            sb.append("<p>Error reading build log \"").append(fileNameEscaped).append("\"</p>");
            return;
        }

        String sanitizedLog = StringEscapeUtils.escapeHtml4(buildLog.log);
        String logCss = "background-color: #f4f4f4; white-space: pre-wrap; word-wrap: break-word";

        sb.append("<h1>Build: ").append(fileNameEscaped).append("</h1>");
        sb.append("<ul>");
        sb.append("  <li>timestamp: ").append(buildLog.timestamp).append("</li>");
        sb.append("  <li>success: ").append(buildLog.success).append("</li>");
        sb.append("  <li>commit identifier: ").append(buildLog.commitIdentifier).append("</li>");
        sb.append("</ul>");
        sb.append("<h2>Log:</h2>");
        sb.append("<pre style=\"").append(logCss).append("\">").append(sanitizedLog).append("</pre>");
    }

}
