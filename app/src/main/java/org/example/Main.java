package org.example;

import org.eclipse.jetty.server.Server;

public class Main {
    /**
     * Start the CI webhook server on port 8080.
     *
     * @param args command line arguments
     * @throws Exception if server startup fails
     */
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}
