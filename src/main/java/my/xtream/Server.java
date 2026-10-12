package my.xtream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import helper.Logger;

public class Server {
    private final Map<String, Xtream> xtreams=new HashMap<>();
    private volatile HttpServer httpServer;
    private JSONObject cfg;
    private final String cfgFile;
    private int port;

    Server(String cfgFile) throws IOException {
        this.cfgFile = cfgFile;
        readCfg(cfgFile);
    }

    void readCfg(String cfgPath) throws IOException {
        JSONObject map = new JSONObject(HttpClient.get(cfgPath));

        URI uri = URI.create(map.getString("me"));

        String protocol = uri.getScheme();
        // String host = uri.getHost();

        this.port = uri.getPort();

        if (this.port == -1) {
            this.port = switch (protocol.toLowerCase()) {
                case "http" -> 80;
                case "https" -> 443;
                default -> throw new IllegalArgumentException(
                        "No default port known for scheme: " + protocol
                );
            };
        }
        this.cfg=map;
    }

    public synchronized void run() throws IOException {
        if (httpServer != null) {
            throw new IllegalStateException("Server has already been started");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(this.port), 0);

        server.createContext("/", this::handle);

        httpServer = server;
        server.start();
        Logger.info("Server started on port {}",this.port);
    }

    public synchronized void stop() {
        stop(5);
    }

    public synchronized void stop(int delaySeconds) {
        HttpServer server = httpServer;

        if (server == null) {
            return;
        }

        httpServer = null;
        server.stop(delaySeconds);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "HEAD" -> processHead(exchange, path);

                case "GET" -> processGet(exchange, path);

                default -> sendStatus(exchange, 405);
            }
        } catch (Exception e) {
            e.printStackTrace();

            if (!isResponseClosed(exchange)) {
                sendStatus(exchange, 500);
            }
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
        if ( "/reload".equals(path)) {
            readCfg(this.cfgFile);
            sendStatus(exchange, 200);
            xtreams.clear();
            return;
        }

        URI uri = exchange.getRequestURI();
        String rawQuery = uri.getRawQuery();

        Map<String, String> params = parseQuery(rawQuery);
        String username = params.get("username");
        String password = params.get("password");
        String action = params.get("action");

        Xtream xtream;
        // System.out.println("Inbound -> " + path + " ? " + rawQuery);

        if (username == null || password == null) {
            String r = cfg.getString("provider");
            Logger.info("Redirect -> {} ",redirectTarget(r, path, rawQuery));
            redirect(exchange, redirectTarget(r, path, rawQuery));
        } else {
            if (xtreams.containsKey(username)) {
                xtream = xtreams.get(username);
            } else {
                xtream = new Xtream(cfg, username, password);
                xtreams.put(username, xtream);
            }

            switch (path) {
                case "/xmltv.php" -> {
                    String epg = xtream.getEPG();
                    Logger.info("Redirect -> {} ",xtream.redirectTarget(path, rawQuery));
                    redirect(exchange, epg != null ? epg : xtream.redirectTarget(path, rawQuery));
                }

                case "/player_api.php" -> {
                    Logger.info("Action -> {} ? {}",path,rawQuery);
                    if (action == null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    String list = xtream.apiCmd(action);
                    if (list==null) {
                        redirect(exchange, xtream.redirectTarget(path, rawQuery));
                    } else {
                        sendJson(exchange, xtream.apiCmd(action));
                    }
                }

                case "/get.php" -> {
                    Logger.info("Cache -> {} ? {}",path,rawQuery);
                    sendJson(exchange, xtream.getM3U());
                }

                case "/" -> sendStatus(exchange, 401);

                default -> {
                    Logger.info("Redirect -> ",xtream.redirectTarget(path,rawQuery));
                    redirect(exchange, xtream.redirectTarget(path, rawQuery));
                }
            }
        }
    }

    private String redirectTarget(String host, String path, String rawQuery) {
        String query = rawQuery == null ? "" : "?" + rawQuery;
        String t = (!(host.endsWith("/") || path.startsWith("/"))) ? "/" : "";
        return host + t + path + query;
    }

    private static Map<String, String> parseQuery(
            String rawQuery
    ) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        Map<String, String> params =
                new java.util.LinkedHashMap<>();

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
        return java.net.URLDecoder.decode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private static String clientIp(
            HttpExchange exchange
    ) {
        var address = exchange
                .getRemoteAddress()
                .getAddress();

        return address == null
                ? exchange.getRemoteAddress().toString()
                : address.getHostAddress();
    }

    private static void redirect(
            HttpExchange exchange,
            String location
    ) throws IOException {
        exchange.getResponseHeaders().set(
                "Location",
                location
        );

        sendStatus(exchange, 302);
    }

    private static void sendStatus(
            HttpExchange exchange,
            int status
    ) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static void sendJson(
            HttpExchange exchange,
            String json
    ) throws IOException {
        byte[] body = json.getBytes(
                StandardCharsets.UTF_8
        );

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8"
        );

        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream out =
                     exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static boolean isResponseClosed(
            HttpExchange exchange
    ) {
        /*
         * HttpExchange does not expose a standard
         * "is response closed" method. This helper is
         * intentionally conservative.
         */
        return false;
    }
}