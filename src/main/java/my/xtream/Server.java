package my.xtream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Server {

    private final Xtream xtream;

    Server(Xtream xtream) {
        this.xtream = xtream;
    }

    public void run(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("HEAD".equals(method)) {
                processHead(exchange, path);
                return;
            }

            if ("GET".equals(method)) {
                processGet(exchange, path);
                return;
            }

            sendStatus(exchange, 405);
        } catch (Exception e) {
            e.printStackTrace();
            sendStatus(exchange, 500);
        }
    }

    private void processHead(HttpExchange exchange, String path) throws IOException {
        if ("/generate_204".equals(path)) {
            String ip = clientIp(exchange);
            System.err.println("CONNECTIVITY CHECK: " + ip + " -> " + path);
            sendStatus(exchange, 204);
        } else {
            sendStatus(exchange, 404);
        }
    }

    private void processGet(HttpExchange exchange, String path) throws IOException {
        URI uri = exchange.getRequestURI();
        Map<String, String> params = parseQuery(uri.getRawQuery());
        String action = params.get("action");

        String result = null;

        if ("/player_api.php".equals(path)) {
            result = xtream.apiCmd(action);
        }

        if (path.startsWith("/get.php")) {
            result = xtream.apiCmd("getM3U");
        }

        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();

        if ("/xmltv.php".equals(path) && xtream.getEPG() != null) {
            String target = xtream.getEPG();
            System.err.println("Redirect: " + clientIp(exchange) + " -> " + target);
            redirect(exchange, target);
            return;
        }

        if (result == null) {
            if (!"/".equals(path)) {
                String target = xtream.getRedirectURL() + path + query;
                System.err.println("Redirect: " + clientIp(exchange) + " -> " + target);
                redirect(exchange, target);
            } else {
                sendStatus(exchange, 403);
            }
            return;
        }

        System.err.println("Cache: " + clientIp(exchange) + " -> " + path + query);
        sendJson(exchange, result);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        Map<String, String> params = new java.util.LinkedHashMap<>();

        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);

            params.put(
                    urlDecode(key),
                    urlDecode(value)
            );
        }

        return params;
    }

    private static String urlDecode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String clientIp(HttpExchange exchange) {
        var address = exchange.getRemoteAddress().getAddress();
        return address == null ? exchange.getRemoteAddress().toString() : address.getHostAddress();
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        sendStatus(exchange, 302);
    }

    private static void sendStatus(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}