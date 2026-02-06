package org.example;

import org.example.payload.PushPayload;
import org.example.payload.Repository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PushPayloadTest {

    /**
     * Positive test: Deserializes a JSON string representing a GitHub push webhook payload.
     * Test case: JSON includes fields "ref", "after", "repository" with clone_url and full_name.
     * Expected: JSON is correctly parsed into a PushPayload object, the known fields are 
     * mapped correctly.
     */
    @Test
    void deserialization_shouldSucceed() throws Exception {
        String json = """
            {
                "ref": "refs/heads/main",
                "after": "12345678",
                "repository": {
                    "clone_url": "https://github.com/user/repo.git",
                    "full_name": "user/repo"
                }
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        PushPayload payload = mapper.readValue(json, PushPayload.class);

        assertEquals("refs/heads/main", payload.ref);
        assertEquals("12345678", payload.after);
        assertEquals("https://github.com/user/repo.git", payload.repository.clone_url);
        assertEquals("user/repo", payload.repository.full_name);
    }

    /**
     * Negative test: Ensures that unknown fields in the JSON payload do not break parsing.
     * Test case: JSON includes a completely unexpected field "unexpected_field".
     * Expected: JSON parsing ignores the unknown field and correctly maps the known fields.
     */
    @Test
    void deserialization_withExtraUnknownField_shouldIgnoreIt() throws Exception {
        String json = """
            {
                "ref": "refs/heads/main",
                "after": "12345678",
                "repository": {
                    "clone_url": "https://github.com/user/repo.git",
                    "full_name": "user/repo"
                },
                "unexpected_field": "ignored"
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        PushPayload payload = mapper.readValue(json, PushPayload.class);

        assertEquals("refs/heads/main", payload.ref);
    }
}
