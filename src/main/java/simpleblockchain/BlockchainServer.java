package simpleblockchain;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class BlockchainServer {

    /**
     * Starts the Javalin web server on port 7070.
     * Serves the dashboard from the resources folder and exposes the blockchain API.
     */
    
    public static void start(ArrayList<Block> blockchain) {
        // Create and start the Javalin application
        Javalin app = Javalin.create(config -> {
            // Configure static file serving from the classpath (src/main/resources/public)
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";                   // change to host files on a subpath, like /assets
                staticFiles.directory = "/public";              // the directory where your files are located
                staticFiles.location = Location.CLASSPATH;      // Location.CLASSPATH (jar) or Location.EXTERNAL (file system)
                staticFiles.precompress = false;                // if the files should be pre-compressed
            });
            
            // Enable CORS for ease of development
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(7070);

        // Register a shutdown hook to clean up resources and release port 7070
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[ SERVER ] Stopping Javalin...");
            app.stop();
        }));

        // API Endpoint
        app.get("/api/blockchain", ctx -> {
            ctx.contentType("application/json").result(StringUtil.getJson(blockchain));
        });

        System.out.println("\n---------------------------------------------------------");
        System.out.println("[ SERVER ] Web Dashboard ACTIVE!");
        System.out.println("[ SERVER ] Local Explorer: http://localhost:7070");
        System.out.println("---------------------------------------------------------\n");

        // Automatically open the browser
        openBrowser("http://localhost:7070");
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                }
            }
        } catch (java.io.IOException | java.net.URISyntaxException e) {
            System.out.println("[ INFO ] Could not open browser automatically: " + e.getMessage());
        }
    }
}
