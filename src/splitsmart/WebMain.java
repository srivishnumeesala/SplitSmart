package splitsmart;

import splitsmart.storage.DataStore;
import splitsmart.web.WebServer;

/**
 * Entry point for launching the SplitSmart Web Application.
 * Reads PORT from environment (Railway sets this automatically),
 * starts the built-in HTTP server, then blocks the main thread
 * so the JVM stays alive indefinitely.
 */
public class WebMain {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== SplitSmart Web Server starting ===");

        // 1. Initialise persistent storage
        try {
            DataStore.getInstance();
            System.out.println("DataStore initialised OK.");
        } catch (Exception e) {
            System.err.println("DataStore init failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 2. Resolve port  (Railway injects PORT env var)
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
                System.err.println("Invalid PORT env value: " + envPort + " — using 8080");
            }
        }
        System.out.println("Binding to port: " + port);

        // 3. Start HTTP server
        try {
            WebServer server = new WebServer(port);
            server.start();

            // Clean shutdown on SIGTERM (Railway sends this when stopping)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown signal received — stopping server.");
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 4. BLOCK THE MAIN THREAD FOREVER
        //    Without this the JVM exits immediately after main() returns,
        //    even though the HttpServer has background threads running.
        System.out.println("Server is running. Main thread blocking.");
        Thread.currentThread().join();   // waits forever until interrupted
    }
}
