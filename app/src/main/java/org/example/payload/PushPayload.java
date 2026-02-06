package org.example.payload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PushPayload {
    public String ref;
    public String after;
    public Repository repository;
}