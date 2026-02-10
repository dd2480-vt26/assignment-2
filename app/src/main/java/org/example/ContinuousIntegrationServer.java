package org.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
 
import org.eclipse.jetty.server.Server;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.payload.PushPayload;

/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/
public class ContinuousIntegrationServer extends AbstractHandler
{
    final String BUILD_LOG_ROUTE = "/custom-build-logs";
    final Path BUILD_LOG_DIR = Path.of("custom-build-logs");       // Note: Placed in project root (not repo root, not src)

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        switch (request.getMethod()) {
            case "PUT":
                handlePUT(request);
                break;

            case "POST":
                handlePOST(request);
                break;

            case "GET":
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

                handleGET(target, response, sb);

                sb.append("""
                    </body>
                    </html>     
                """);

                response.getWriter().print(sb);
                break;

            default:
                break;
        }
    }
 
    public void handlePUT(HttpServletRequest request) throws IOException {
        // TODO: Can be removed if unused.
    }

    public void handlePOST(HttpServletRequest request) throws IOException {
        String jsonString = request.getReader().lines().collect(Collectors.joining("\n")); // takes the request and stringafies it into a json structure
        ObjectMapper mapper = new ObjectMapper(); // maps JSON structure to existing class

        if (!jsonString.isBlank()) { // to ignore empty messages, seems like it can be solved by checking headers for push
            PushPayload payload = mapper.readValue(jsonString, PushPayload.class); // maps the JSON to the class PushPayload

            // Bellow is just example usage and for testing
            String branch = payload.ref.replace("refs/heads/", ""); // replace refs/heads/branchName with the just branchName
            String cloneUrl = payload.repository.clone_url;
            String repoName = payload.repository.full_name;
            String commitSha = payload.after;

            System.out.println("branch: " + branch);
            System.out.println("clone URL: " + cloneUrl);
            System.out.println("repository: " + repoName);
            System.out.println("commit sha: " + commitSha);
        }
    }

    private static String normalizeString(String str) {       
        if (str == null || str.isEmpty()) {
            return str;
        }
        str = str.replaceAll("/+", "/");
        if (str.charAt(str.length()-1) == '/') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    // Examples of valid URLs:
    //     - "localhost:8080/custom-build-logs" to list all build logs.
    //     - "localhost:8080/custom-build-logs/2026-02-09T13:17:20+01:00.json" to list info from this specific build
    public void handleGET(String target, HttpServletResponse response, StringBuilder sb) {
        
        target = normalizeString(target);

        // Route: List all logs
        if (target.equals(BUILD_LOG_ROUTE)) {
            sb.append("<h1>All Build Logs</h1>");
            // TODO
            return;
        }

        // Route: List specific log file
        if (target.startsWith(BUILD_LOG_ROUTE)) {

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
            return;
        }

        sb.append("<p>Invalid GET request</p>");
    }

    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}