package my.xtream;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class Main {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @SuppressWarnings("unchecked")
    static void main(String[] args) throws IOException {
        String cfgPath = (args.length < 1)? "xtream.json" : args[0];
        JSONObject map = new JSONObject(Get.get(cfgPath));
        JSONObject cfg = new JSONObject(map.get("cfg"));

        Xtream xtream = new Xtream(map);
        for (String file : Xtream.getFiles()) {
            if (!cfg.has(file))
                continue;
            for (String cfgEntry : ((List<String>) cfg.get(file))) {
                xtream.cmd(file, cfgEntry);
            }
        }

        int port=8080;
        if (map.has("my_port")) {
            port = Integer.parseInt((String) map.get("my_port"));
        }

        new Server(xtream).run(port);
    }
}