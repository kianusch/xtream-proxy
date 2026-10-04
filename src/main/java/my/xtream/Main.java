package my.xtream;

import org.json.JSONObject;
import org.tinylog.configuration.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @SuppressWarnings("unchecked")
    static void main(String[] args) throws IOException {
        Configuration.set("level", "DEBUG");
        Configuration.set("writer.format", "{date: HH:mm:ss}: {message}");

        Configuration.set("level@io.javalin", "WARN");
        Configuration.set("level@org.eclipse.jetty", "WARN");

        String cfgPath = (args.length < 1)? "xtream.json" : args[0];
        Map<String, Object> map = new JSONObject(Get.get(cfgPath)).toMap();
        Map<String, Object> cfg = (Map<String, Object>) map.get("cfg");

        /*
        String upstream;
        if (((String) map.get("provider")).startsWith("http")) {
            upstream = map.get("provider")+"/player_api.php?username="+map.get("username")+"&password="+map.get("password");
        } else {
            upstream = (String) map.get("provider");
        }
        */

        Xtream xtream = new Xtream(map);
        for (String file : Xtream.getFiles()) {
            if (!cfg.containsKey(file))
                continue;
            for (String cfgEntry : ((List<String>) cfg.get(file))) {
                xtream.cmd(file, cfgEntry);
            }
        }

        int port=8080;
        if (map.containsKey("my_port")) {
            port = Integer.parseInt((String) map.get("my_port"));
        }

        new Server(xtream).run(port);
    }

    private static List<String> readCfg(String cfgFile) {
        List<String> cfg = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(cfgFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                cfg.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cfg;
    }
}