import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

// TODO 1: Implement a new requirement (the oven and the dishwasher share a power circuit, see Part 1)

public class Kitchen {
    static boolean ovenOn = false;
    static int ovenTemperature = 350;
    static boolean dishwasherOn = false;
    static String dishwasherCycle = "Normal";
    static boolean fridgeOn = true;
    static int fridgeTemperature = 37;
    static List<String> notifications = new ArrayList<>();
    static Timer timer = new Timer(true);

    static final String STYLE = """
            <style>
              * { box-sizing: border-box; }
            body { font-family: system-ui, -apple-system, "Segoe UI", sans-serif; background: #f3f1ec; color: #1c1c1c;
                max-width: 920px; margin: 0 auto; padding: 24px 16px; }
            header { background: #1c1c1c; border-radius: 16px; padding: 20px 24px; margin: 0 0 20px; }
            h1 { margin: 0; color: #ffb81c; letter-spacing: 0.5px; }
            .subtitle { color: #c9c4b8; margin: 4px 0 0; }
            h2 { margin: 28px 0 12px; }
            .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 16px; }
            .card { background: #fff; border-radius: 16px; padding: 18px 20px; }
            .card h3 { display: flex; justify-content: space-between; align-items: center; margin: 0 0 10px; }
            .status { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #6b6b6b; }
            .dot { width: 12px; height: 12px; border-radius: 50%; background: #d64545; }
            .on .dot { background: #2fb36d; }
            .value { font-size: 30px; font-weight: 700; margin: 0 0 14px; }
            form { display: flex; gap: 8px; margin: 0 0 12px; }
            input, select { flex: 1; min-width: 0; padding: 8px 10px; border: none; border-radius: 10px; background: #f3f1ec;
                        color: #1c1c1c; font: inherit; }
            button, .btn { display: inline-block; padding: 8px 16px; border: none; border-radius: 10px; background: #ebe8e1;
                        color: #1c1c1c; font: inherit; font-weight: 600; text-decoration: none; cursor: pointer; }
            button:hover, .btn:hover { background: #dedad0; }
            .btn.primary { background: #ffb81c; }
            .btn.primary:hover { background: #f0a800; }
            .btn.dark { background: #1c1c1c; color: #ffb81c; }
            .btn.dark:hover { background: #333; }
            .toolbar { margin: 20px 0 0; }
            .error { display: flex; justify-content: space-between; align-items: center; gap: 12px; background: #fde4e4;
                color: #8a1f1f; padding: 12px 16px; border-radius: 12px; margin: 0 0 16px; }
            .notifications { background: #fff; border-radius: 16px; padding: 6px 20px; list-style: none; margin: 0; }
            .notifications li { padding: 10px 0; }
            .notifications li + li { border-top: 1px solid #f0ede6; }
            </style>
            """;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 1530), 0);
        server.createContext("/", Kitchen::handle);
        server.start();
        System.out.println("Cook N'At is running. Open http://localhost:1530 in your browser.");
    }

    static synchronized void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();

        if (path.equals("/toggle")) {
            String name = param(query, "name");
            if (name.equals("oven")) {
                if(dishwasherOn){
                    notifications.add("Oven can't be turned on the dishwasher is also on.");
                }
                else{
                    ovenOn = !ovenOn;
                    notifications.add("Oven turned " + (ovenOn ? "on" : "off"));
                    if (ovenOn) {
                        timer.schedule(new TimerTask() {
                            public void run() {
                                synchronized (Kitchen.class) {
                                    if (ovenOn)
                                        notifications.add("Oven pre-heated to " + ovenTemperature + " F");
                                }
                            }
                        }, 1500);
                    }
                }
            }
            if (name.equals("dishwasher")) {
                if(ovenOn){
                    notifications.add("Can't turn the dishwasher on right now the oven is also on");
                }
                else{
                    dishwasherOn = !dishwasherOn;
                    notifications.add("Dishwasher turned " + (dishwasherOn ? "on" : "off"));
                    if (dishwasherOn) {
                        timer.schedule(new TimerTask() {
                            public void run() {
                                synchronized (Kitchen.class) {
                                    if (dishwasherOn) {
                                        dishwasherOn = false;
                                        notifications.add("Dishwasher finished the " + dishwasherCycle + " cycle");
                                    }
                                }
                            }
                        }, 3000);
                    }
                }
            }
            if (name.equals("fridge")) {
                if (fridgeOn && !param(query, "confirmed").equals("yes")) {
                    send(exchange,
                            "<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'><title>Cook N'At Kitchen</title>"
                                    + STYLE
                                    + "</head><body><div class='card'><h3>Turn off the fridge?</h3><p>Your food may spoil.</p>"
                                    + "<a class='btn primary' href='/toggle?name=fridge&confirmed=yes'>Yes, turn it off</a> "
                                    + "<a class='btn' href='/'>Cancel</a></div></body></html>");
                    return;
                }
                fridgeOn = !fridgeOn;
                notifications.add("Fridge turned " + (fridgeOn ? "on" : "off"));
            }
            redirect(exchange, "/");
            return;
        }

        if (path.equals("/temperature")) {
            int value;
            try {
                value = Integer.parseInt(param(query, "value"));
            } catch (NumberFormatException e) {
                value = -1;
            }
            if (value < 170 || value > 550) {
                redirect(exchange, "/?error=" + URLEncoder.encode("Please enter a temperature between 170 and 550 F",
                        StandardCharsets.UTF_8));
                return;
            }
            ovenTemperature = value;
            notifications.add("Oven set to " + value + " F");
            redirect(exchange, "/");
            return;
        }

        if (path.equals("/cycle")) {
            if (dishwasherOn) {
                redirect(exchange, "/?error=" + URLEncoder
                        .encode("You cannot change the cycle while the dishwasher is running", StandardCharsets.UTF_8));
                return;
            }
            dishwasherCycle = param(query, "value");
            notifications.add("Dishwasher cycle set to " + dishwasherCycle);
            redirect(exchange, "/");
            return;
        }

        if (path.equals("/alloff")) {
            if (ovenOn) {
                ovenOn = false;
                notifications.add("Oven turned off");
            }
            if (dishwasherOn) {
                dishwasherOn = false;
                notifications.add("Dishwasher turned off");
            }
            redirect(exchange, "/");
            return;
        }

        String error = param(query, "error");
        StringBuilder html = new StringBuilder();
        html.append(
                "<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'><title>Cook N'At Kitchen</title>"
                        + STYLE + "</head><body>");
        html.append(
                "<header><h1>Cook N'At Kitchen</h1><p class='subtitle'>Your smart kitchen at a glance</p></header>");

        if (!error.isEmpty()) {
            html.append("<div class='error'><span>" + error.replace("<", "&lt;")
                    + "</span><a class='btn' href='/'>OK</a></div>");
        }

        html.append("<div class='grid'>");
        html.append("<div class='card " + (ovenOn ? "on" : "off")
                + "'><h3>Oven <span class='status'><span class='dot'></span>" + (ovenOn ? "ON" : "OFF")
                + "</span></h3>");
        html.append("<p class='value'>" + ovenTemperature + " F</p>");
        html.append("<form action='/temperature'><input name='value' type='number' value='" + ovenTemperature + "'>");
        html.append("<button>Set</button></form>");
        html.append("<a class='btn" + (ovenOn ? "" : " primary") + "' href='/toggle?name=oven'>"
                + (ovenOn ? "Turn off" : "Turn on") + "</a></div>");

        html.append("<div class='card " + (dishwasherOn ? "on" : "off")
                + "'><h3>Dishwasher <span class='status'><span class='dot'></span>" + (dishwasherOn ? "ON" : "OFF")
                + "</span></h3>");
        html.append("<p class='value'>" + dishwasherCycle + "</p><form action='/cycle'><select name='value'>");
        html.append("<option" + (dishwasherCycle.equals("Normal") ? " selected" : "") + ">Normal</option>");
        html.append("<option" + (dishwasherCycle.equals("Eco") ? " selected" : "") + ">Eco</option>");
        html.append("<option" + (dishwasherCycle.equals("Heavy") ? " selected" : "") + ">Heavy</option>");
        html.append("</select><button>Set</button></form>");
        html.append("<a class='btn" + (dishwasherOn ? "" : " primary") + "' href='/toggle?name=dishwasher'>"
                + (dishwasherOn ? "Turn off" : "Turn on") + "</a></div>");

        html.append("<div class='card " + (fridgeOn ? "on" : "off")
                + "'><h3>Fridge <span class='status'><span class='dot'></span>" + (fridgeOn ? "ON" : "OFF")
                + "</span></h3>");
        html.append("<p class='value'>" + fridgeTemperature + " F</p>");
        html.append("<a class='btn" + (fridgeOn ? "" : " primary") + "' href='/toggle?name=fridge'>"
                + (fridgeOn ? "Turn off" : "Turn on") + "</a></div>");
        html.append("</div>");

        html.append("<p class='toolbar'><a class='btn dark' href='/alloff'>Turn everything off</a></p>");
        html.append("<h2>Notifications</h2><ul class='notifications'>");
        for (int i = notifications.size() - 1; i >= 0; i--) {
            html.append("<li>" + notifications.get(i) + "</li>");
        }
        html.append("</ul>");

        if (error.isEmpty()) {
            html.append(
                    "<script>setInterval(function () { if (document.activeElement == document.body) location.href = '/'; }, 1000);</script>");
        }
        html.append("</body></html>");
        send(exchange, html.toString());
    }

    static String param(String query, String key) {
        if (query == null)
            return "";
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals(key) && parts.length == 2)
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        }
        return "";
    }

    static void send(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }
}
