package splitsmart;

import splitsmart.storage.DataStore;
import splitsmart.web.WebServer;

/**
 * Entry point for launching the SplitSmart Web Application.
 * Configures the port from the environment (defaulting to 8080) and starts
 * the built-in HTTP server serving the REST API and modern web user interface.
 */
public class WebMain {

    public static void main(String[] args) {
        // Initialize persistent DataStore
        DataStore.getInstance();

        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
            }
        } else {
            String propPort = System.getProperty("port");
            if (propPort != null && !propPort.trim().isEmpty()) {
                try {
                    port = Integer.parseInt(propPort.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        try {
            WebServer server = new WebServer(port);
            server.start();

            // Keep main thread alive for CLI/Docker execution
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Stopping SplitSmart Web Server...");
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
