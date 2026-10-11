package my.xtream;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Main {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    static void main(String[] args) throws IOException {
        String cfgPath = args.length < 1 ? "xtream.json" : args[0];
        Server server = new Server(cfgPath);
        server.run();
    }
}
