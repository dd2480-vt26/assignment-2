package org.example;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetRequestHandlerTest {
    
    private HttpServletResponse response;
    private StringWriter stringWriter;

    /**
     * Create a mockup of an HttpServletResponse instance in order to capture the
     * HTML output produced by GetRequestHandler.handle().
     * <p>
     * This works because {@code HttpServletResponse.getWriter()} returns a {@code PrintWriter}.
     * Before it is returned, we inject our own writer, so the method call returns our writer instead.
     * Our own writer is connected to our own {@code StringWriter}, which can return the string we want to capture.
     * </p>
     */
    @BeforeEach
    void setUp() throws IOException {
        response = mock(HttpServletResponse.class);
        stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(writer);
    }
    
    /**
     * Negative test: Ensures an invalid GET request renders an error message in HTML format.
     * Test case: GET "/invalid"
     * Expected: "Invalid GET request" message
     */
    @Test
    void handle_invalidRoute_returnsInvalidMessage() throws IOException {
        GetRequestHandler.handle("/invalid", response);
        response.getWriter().flush();
        String htmlOutput = stringWriter.toString();
        assertTrue(htmlOutput.contains("<p>Invalid GET request</p>"));
    }

    /**
     * Positive test: Ensures all builds are listed, given the valid request.
     * Test case: GET "/custom-build-logs"
     * Expected: All build logs to be listed
     */
    @Test
    void handle_requestAllBuilds_returnsListOfBuilds() throws IOException {
        Path dir = Path.of("logs/dd2480-vt26/assignment-2");
        Files.createDirectories(dir);
        
        Path filePath = dir.resolve("temp.json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));
        writer.write("""
            {
            "timestamp" : "2026-02-12T14:40:59.905714443+01:00",
            "commitIdentifier" : "9d471bf817f2c0e4d189f76c593aa500619e0b21",
            "buildStatus" : "SUCCESS",
            "buildLog" : "abc",
            "testStatus" : "SUCCESS",
            "testLog" : "xyz"
            }     
        """);
        writer.close();

        GetRequestHandler.handle("/logs/dd2480-vt26/assignment-2/temp.json", response);
        Files.delete(filePath);

        response.getWriter().flush();
        String htmlOutput = stringWriter.toString();

        assertTrue(htmlOutput.contains("<h2>Build</h2>"));
        assertTrue(htmlOutput.contains("<h2>Test</h2>"));
    }

}
