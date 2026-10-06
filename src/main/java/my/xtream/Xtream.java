package my.xtream;

import org.json.JSONObject;

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
    private final String epg;

    private final String r_url;
    private final String r_protocol;
    private final String r_port;

    String streamUrl;
    String streamPostFix;
    String prvdr;

    Xtream(JSONObject cfg) throws IOException {
        prvdr = cfg.getString("provider");
        String upstream;
        if (prvdr.startsWith("http")) {
            upstream = prvdr+"/player_api.php?username="+cfg.get("username")+"&password="+cfg.get("password");
        } else {
            upstream = prvdr;
        }

        if (cfg.has("epg"))
            epg = cfg.getString("epg");
        else
            epg = null;

        System.out.println("reading: metadata from "+prvdr);

        if (upstream.startsWith("http"))
            metadata = new Metadata(upstream);
        else
            metadata = new Metadata(upstream +"metadata");

        r_url = metadata.getServerInfo("url");
        r_protocol = metadata.getServerInfo("server_protocol");
        r_port = metadata.getServerInfo("port");

        String s_port;
        String s_protocol;
        if (cfg.has("override_protocol")) {
            s_protocol = cfg.getString("override_protocol");
            if (cfg.has("override_port")) {
                s_port = cfg.getString("override_port");
            } else {
                if ("https".equals(s_protocol))
                    s_port ="443";
                else
                    s_port ="80";
            }
        } else {
            s_protocol = r_protocol;
            s_port =r_port;
        }

        if ("80".equals(s_port) || "443".equals(s_port)) {
            streamUrl = s_protocol + "://" + r_url + "/";
        } else {
            streamUrl = s_protocol + "://" + r_url + ":" + s_port + "/";
        }
        streamPostFix = "/"+metadata.getUserInfo("username")+"/"+metadata.getUserInfo("password")+"/";

        if (cfg.has("my_url")) {
            metadata.putServerInfo("url", cfg.getString("my_url"));
        }
        if (cfg.has("my_server_protocol")) {
            metadata.putServerInfo("server_protocol", cfg.getString("my_server_protocol"));
        }
        if (cfg.has("my_port")) {
            metadata.putServerInfo("port", cfg.getString("my_port"));
        }

        System.out.println(metadata.stringJSON());

        for (var file : files) {
            System.out.println("reading: "+ file);
            if (upstream.startsWith("http"))
                xtreamLists.put(file, new XtreamList(Get.mkUrl(upstream)+"&action=get_"+file));
            else
                xtreamLists.put(file, new XtreamList(upstream +file));
        }
    }

    String getRedirectURL() {
        if (prvdr.startsWith("http://*."))
            return Get.mkUrl(prvdr);
        return r_protocol+"://"+Get.mkUrl(r_url)+":"+r_port;
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