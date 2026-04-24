package simpleblockchain;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import network.protocol.kad.KBucket;
import network.protocol.kad.RoutingTable;

public class BlockchainServer {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainServer.class);

    /**
     * Starts the Javalin web server on port 7070.
     * Serves the dashboard from the resources folder and exposes the blockchain API.
     */
    
    public static void start(ArrayList<Block> blockchain, RoutingTable routingTable) {

        // Create and start the Javalin application
        Javalin app = Javalin.create(config -> {
            
            // Configure static file serving from the classpath (src/main/resources/public)
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";                   // change to host files on a subpath, like /assets
                staticFiles.directory = "/public";              // the directory where your files are located
                staticFiles.location = Location.CLASSPATH;      // Location.CLASSPATH (jar) or Location.EXTERNAL (file system)
                staticFiles.precompress = false;                // if the files should be pre-compressed
            });
            
            // Enable CORS for ease of development (allows web dashboard to access API)
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(7070); 

        // Register a shutdown hook to clean up resources and release port 7070
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Deteniendo servidor web Javalin...");
            app.stop();
        }));

        // API Endpoints

        // API para obtener la blockchain completa
        app.get("/api/blockchain", ctx -> {
            ctx.contentType("application/json").result(StringUtil.getJson(blockchain));
        });

        // API para obtener la tabla de rutas Kademlia, muestra el ID local, 
        // el número total de nodos y el tamaño de cada bucket.
        app.get("/api/kad", ctx -> {
            Map<String, Object> stats = new HashMap<>(); 
            stats.put("localId", routingTable.getLocalId().toString()); 
            stats.put("totalNodes", routingTable.getTotalNodes());
            
            List<Integer> bucketSizes = new ArrayList<>();
            KBucket[] buckets = routingTable.getBuckets();
            for (KBucket b : buckets) {
                bucketSizes.add(b.size());
            }
            stats.put("buckets", bucketSizes);
            ctx.json(stats);
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
