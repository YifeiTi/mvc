import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

// Complete, no changes needed until Part 3a
public class KitchenApp {
    public static void main(String[] args) throws IOException {
        KitchenModel model = new KitchenModel();
        KitchenView view = new KitchenView();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 1530), 0);
        server.createContext("/", new KitchenController(model, view));
        server.start();
        System.out.println("Cook N'At is running. Open http://localhost:1530 in your browser.");
        // TODO 3a.1: Start the console here (see Part 3a)
        new Thread(new KitchenConsole(model)).start();
    }
}
