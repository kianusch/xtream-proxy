package my.xtream;

import org.tinylog.Logger;

import java.io.IOException;
import java.util.*;

public class Xtream {
    private final static ArrayList<String> files = new ArrayList<>(List.of(
            "live_categories",
            "series_categories",
            "vod_categories",
            "series",
            "live_streams",
            "vod_streams"));
    private final static Map<String, String> joins = Map.of(
            "live_streams", "live_categories",
            "series", "series_categories",
            "vod_streams", "vod_categories");

    // private String header;
    private final Map<String, XtreamList> xtreamLists = new HashMap<>();
    private final Metadata metadata;
    private final String upstream;
    private final String epg;

    String r_url;
    String r_protocol;
    String r_port;

    String streamUrl;
    String streamPostFix;

    Xtream(String us, Map<String, Object> cfg) throws IOException {
        upstream = us;
        epg = (String) cfg.get("epg");
        Logger.debug("reading: metadata");

        if (upstream.startsWith("http"))
            metadata = new Metadata(upstream);
        else
            metadata = new Metadata(upstream+"metadata");

        r_url = (String) metadata.getServerInfo("url");
        r_protocol = (String) metadata.getServerInfo("server_protocol");
        r_port = (String) metadata.getServerInfo("port");

        streamUrl = r_protocol+"://"+r_url+":"+r_port+"/";
        streamPostFix = "/"+metadata.getUserInfo("username")+"/"+metadata.getUserInfo("password")+"/";

        System.out.println(metadata.stringJSON());

        if (cfg.containsKey("my_url")) {
            metadata.putServerInfo("url", (String) cfg.get("my_url"));
        }
        if (cfg.containsKey("my_server_protocol")) {
            metadata.putServerInfo("server_protocol", (String) cfg.get("my_server_protocol"));
        }
        if (cfg.containsKey("my_port")) {
            metadata.putServerInfo("port", (String) cfg.get("my_port"));
        }

        for (var file : files) {
            Logger.debug("reading: {}", file);
            if (upstream.startsWith("http"))
                xtreamLists.put(file, new XtreamList(upstream+"&action=get_"+file));
            else
                xtreamLists.put(file, new XtreamList(upstream+file));
        }
    }

    String getRedirectURL() {
        return r_protocol+"://"+r_url+":"+r_port;
    }

    void cmd(String file, String arg) {
        if ("*".equals(file)) {
            for (var process : files) {
                xtreamLists.get(process).cmd(arg, getIDs(process));
            }
        } else {
            xtreamLists.get(file).cmd(arg, getIDs(file));
        }
    }

    Set<Object> getIDs(String file) {
        String categoryFile = joins.get(file);
        if (categoryFile != null) {
            return xtreamLists.get(categoryFile).getAll("category_id");
        } else {
            return null;
        }
    }

    String apiCmd (String cmd) {
        if (cmd==null) {
            return metadata.stringJSON();
        } else if ("getM3U".equals(cmd)) {
            return xtreamLists.get("live_streams").stringM3U(xtreamLists.get("live_categories"), streamUrl, streamPostFix);
        } else if (cmd.startsWith("get_") && files.contains(cmd.substring(4))) {
            String file = cmd.substring(4);
            return xtreamLists.get(file).cmd("stringJSON", getIDs(file));
        }
        return null;
    }

    static ArrayList<String> getFiles() {
        return files;
    }

    String getEPG() {
        return epg;
    }
}