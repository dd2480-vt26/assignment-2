package org.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
 
import java.io.IOException;
import java.util.stream.Collectors;
 
import org.eclipse.jetty.server.Server;
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

            case "GET":
                handleGET(target, response);
                break;

            default:
                break;
        }
    }
 
    public void handlePUT(HttpServletRequest request) throws IOException {
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

    public void handleGET(String target, HttpServletResponse response) throws IOException {
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