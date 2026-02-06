package org.example.payload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 
 A class for the payload of the push commit, can be expanded based on needs.
*/
@JsonIgnoreProperties(ignoreUnknown = true) // ignores unknown/undifined attributes
public class PushPayload {
    public String ref; // contains the branch name
    public String after; // contains the SHA (Secure Hash Algorithm), is gonna be used when notifying github, is an authentication of what commit has been handled
    public Repository repository;
}