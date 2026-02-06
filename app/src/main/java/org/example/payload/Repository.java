package org.example.payload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 
 A class for the repository information of the push commit, can be expanded based on needs.
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository {
    public String clone_url; // the url to be used if the project needs to be cloned
    public String full_name; // the full name of the repo, will be used when notifying github
}