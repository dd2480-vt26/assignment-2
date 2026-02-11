package org.example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class GithubTokenProvider {
    /**
     * Retrieves the Personal Access Token (PAT) specified in the file {@code config.properties} 
     * 
     * @return the PAT
     * @throws IOException if the token isn't set 
     * @throws FileNotFoundException if the config file doesn't exist
     */
    public String loadToken(String configFile) throws IOException, FileNotFoundException {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            System.out.println("The config file was not found");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String t = props.getProperty("GITHUB_TOKEN");
        if (t == null || t.isBlank()) {
            throw new IOException("GITHUB_TOKEN is not set in config.properties");
        }
        return t;
    }
}
