package my.xtream;

import helper.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class Xtream {
    private final static ArrayList<String> files = new ArrayList<>(List.of(
            "live_categories",
            "series_categories",
            "vod_categories",
            "series",
            "live_streams",
            "vod_streams"));

    // private String header;
    private final Map<String, XtreamList> xtreamLists = new HashMap<>();
    private final Metadata metadata;
    private final String epg;
    private final String uplink;
    private final String streamPostFix;
    private final String username;
    private final String password;
    // private final Map<String,Object> filter;
    private JSONObject filter;

    private void setCfg(JSONObject cfg) {
            this.filter = cfg;
    }

    Xtream(JSONObject cfg, String username, String password, JSONObject filter) throws IOException {
        this.filter = filter;
        if (cfg.has("epg"))
            epg = cfg.getString("epg");
        else
            epg = null;

        String prvdr = cfg.getString("provider");
        String upstream;

        if (prvdr.startsWith("http")) {
            upstream = prvdr + "/player_api.php?username=" + username + "&password=" + password;
        } else {
            upstream = prvdr;
        }
        Logger.info("Reading: metadata from {}", prvdr);
        if (upstream.startsWith("http"))
            metadata = new Metadata(upstream);
        else
            metadata = new Metadata(upstream + ".metadata");

        String r_url, r_protocol, r_port;

        if (cfg.has("keep_uplink") && cfg.getBoolean("keep_uplink")) {
            URI uri = URI.create(prvdr);
            r_protocol = uri.getScheme();
            r_url = uri.getHost();
            int tmp_port = uri.getPort();

            if (tmp_port == -1) {
                r_port = switch (r_protocol.toLowerCase()) {
                    case "http", "https" -> "";
                    default -> throw new IllegalArgumentException(
                            "No default port known for scheme: " + r_protocol
                    );
                };
            } else {
                r_port = String.valueOf(tmp_port);
            }
        } else {
            r_url = metadata.getServerInfo("url");
            r_protocol = metadata.getServerInfo("server_protocol");
            r_port = metadata.getServerInfo("port");
        }

        if ("80".equals(r_port) || "443".equals(r_port) || r_port.isEmpty()) {
            uplink = r_protocol + "://" + r_url + "/";
        } else {
            uplink = r_protocol + "://" + r_url + ":" + r_port + "/";
        }
        streamPostFix = "/" + metadata.getUserInfo("username") + "/" + metadata.getUserInfo("password") + "/";

        URI uri = URI.create(cfg.getString("me"));
        String m_url = uri.getHost();
        metadata.putServerInfo("url", m_url);
        String m_protocol = uri.getScheme();
        metadata.putServerInfo("server_protocol", m_protocol);
        int m_port = uri.getPort();

        if (m_port > -1) {
            switch (m_protocol.toLowerCase()) {
                case "http" -> metadata.putServerInfo("port", String.valueOf(m_port));
                case "https" -> metadata.putServerInfo("https_port", String.valueOf(m_port));
                default -> throw new IllegalArgumentException(
                        "No default port known for scheme: " + m_protocol
                );
            }
        }

        this.username = metadata.getUserInfo("username");
        this.password = metadata.getUserInfo("password");
    }

    Set<Object> getIDs(String file) {
        String categoryFile = XtreamList.getCat(file);
        if (categoryFile != null) {
            return xtreamLists.get(categoryFile).getAll("category_id");
        } else {
            return null;
        }
    }

    XtreamList update(String file) throws IOException {
        String c = XtreamList.getCat(file);
        XtreamList p = null;

        if (c != null) {
            if (!xtreamLists.containsKey(c)) {
                p = update(c);
            } else {
                p = xtreamLists.get(c);
            }
        }

        Logger.info("Reading: {} [{}]", file, c);
        XtreamList x = new XtreamList(uplink + "player_api.php?username=" + username + "&password=" + password + "&action=get_" + file, p);
        Logger.info("Read {} entries", x.size());

        if (filter.has(file)) {
            JSONArray commands = filter.getJSONArray(file);

            for (int i = 0; i < commands.length(); i++) {
                String command = commands.getString(i);
                x.cmd(command);
            }
        }
        xtreamLists.put(file, x);
        return x;
    }

    public void reset(JSONObject cfg) {
        setCfg(cfg);
        //for (XtreamList x: xtreamLists.values()) {
            //x.makeDirty();
        //}
        //for (XtreamList x: xtreamLists.values()) {
            //x.reset();
        //}

        for (String s: files) {
            if (filter.has(s) && xtreamLists.containsKey(s)) {
                Logger.info("Updating: {}", s);
                XtreamList x=xtreamLists.get(s);
                x.reset();

                JSONArray commands = filter.getJSONArray(s);

                for (int i = 0; i < commands.length(); i++) {
                    String command = commands.getString(i);
                    x.cmd(command);
                }
            }
        }
    }

    String apiCmd(String cmd) throws IOException {
        if (cmd == null) {
            return metadata.stringJSON();
        } else if (cmd.startsWith("get_") && files.contains(cmd.substring(4))) {
            String list = cmd.substring(4);
            if (!xtreamLists.containsKey(list)) {
                update(list);
            }
            return xtreamLists.get(list).stringJSON(getIDs(list));
        }
        return null;
    }

    String getM3U() {
        return xtreamLists.get("live_streams").stringM3U(xtreamLists.get("live_categories"), uplink, streamPostFix);
    }

    String getEPG() {
        return epg;
    }

    String redirectTarget(String path, String rawQuery) {
        String query = rawQuery == null ? "" : "?" + rawQuery;
        return uplink + path + query;
    }

}