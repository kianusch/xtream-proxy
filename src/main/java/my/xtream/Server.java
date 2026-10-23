package my.xtream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import helper.Logger;
import org.json.JSONObject;

public class Server {
    private final Map<String, Uplink> xtreams=new HashMap<>();
    private volatile HttpServer httpServer;
    private final String cfgFile;
    private int port;

    private JSONObject _cfg;
    private Filter _filter;

    Server(String cfgFile) throws IOException {
        if (cfgFile.endsWith(".cfg")) {
            this.cfgFile = cfgFile.substring(0,cfgFile.length()-4);
            readCfg();
            readFilter();
        } else {
            throw new RuntimeException("Config File must end with .cfg");
        }
    }

    void readFilter() throws IOException {
        _filter = new Filter(Path.of(this.cfgFile + ".filter"));
    }

    void readCfg() throws IOException {
        _cfg = new JSONObject(HttpClient.get(this.cfgFile+".cfg"));
        // _rootCfg = new YAML(cfgPath+".yml");

        URI uri = URI.create(_cfg.getString("me"));

        String protocol = uri.getScheme();

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
            if (!isResponseClosed(exchange)) {
                sendStatus(exchange, 500);
            }
            Logger.error("Error: {}",e.getMessage());
            e.printStackTrace();
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
            reset();
            sendStatus(exchange, 200);
            return;
        }

        URI uri = exchange.getRequestURI();
        String rawQuery = uri.getRawQuery();

        Map<String, String> params = parseQuery(rawQuery);
        String username = params.get("username");
        String password = params.get("password");
        String action = params.get("action");

        Uplink xtream;

        if (username == null || password == null) {
            if ("/get.epg".equals(path)) {
                Logger.info("Redirect -> {}", _cfg.getString("epg."+rawQuery));
                redirect(exchange, _cfg.getString("epg."+rawQuery));
            } else {
                String r = _cfg.getString("provider");
                Logger.info("Redirect -> {} ", redirectTarget(r, path, rawQuery));
                redirect(exchange, redirectTarget(r, path, rawQuery));
            }
        } else {
            if (xtreams.containsKey(username)) {
                xtream = xtreams.get(username);
            } else {
                xtream = new Uplink(_cfg, username, password, _filter);
                xtreams.put(username, xtream);
            }

            switch (path) {
                case "/xmltv.php" -> {
                    String epg = xtream.getEPG();
                    Logger.info("Redirect -> {} ",epg!=null?epg:xtream.redirectTarget(path, rawQuery));
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
    private void reset() throws IOException {
        readFilter();
        for (Uplink x: xtreams.values()) {
            x.reset(_filter);
        }
    }
}
