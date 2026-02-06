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
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        System.out.println(target);
        String jsonString = request.getReader().lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = new ObjectMapper();

        if (!jsonString.isBlank()) {
            PushPayload payload = mapper.readValue(jsonString, PushPayload.class);
            
            String branch = payload.ref.replace("refs/heads/", "");
            String cloneUrl = payload.repository.clone_url;
            String repoName = payload.repository.full_name;
            String commitSha = payload.after;

            System.out.println("=== Incoming GitHub Push ===");
            System.out.println("Branch: " + branch);
            System.out.println("Clone URL: " + cloneUrl);
            System.out.println("Repository: " + repoName);
            System.out.println("Commit SHA: " + commitSha);
            System.out.println("============================");
        } else {
            System.out.println("Received empty payload; skipping");
        }

        // here you do all the continuous integration tasks
        // for example
        // 1st clone your repository
        // 2nd compile the code

        response.getWriter().println("CI job done");
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