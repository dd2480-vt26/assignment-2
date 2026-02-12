package org.example;

import org.eclipse.jetty.server.Server;

public class Main {

    protected static final int PORT = 8019;

    /**
     * Start the CI webhook server on port {@link PORT}.
     *
     * @param args command line arguments
     * @throws Exception if server startup fails
     */
    public static void main(String[] args) throws Exception {
        Server server = new Server(PORT);
        server.setHandler(new HttpHandler()); 
        server.start();
        server.join();
    }
}
