package my.xtream;

import helper.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class Uplink {
    // private String header;
    private final Map<String, XtreamList> xtreamLists = new HashMap<>();
    private final Metadata metadata;
    private final String epg;
    private final String uplink;
    private final String streamPostFix;
    private final String username;
    private final String password;

    private final static Map<String, String> PARENTNAMES = Map.of(
            "live_streams", "live_categories",
            "series", "series_categories",
            "vod_streams", "vod_categories");

    Uplink(JSONObject cfg, String username, String password, Filter filter) throws IOException {
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

        for (String s: PARENTNAMES.keySet()) {
            String p=PARENTNAMES.get(s);
            XtreamList pt=new XtreamList(null, filter.get(p),p, uplink, username, password);
            xtreamLists.put(p,pt);
            xtreamLists.put(s,new XtreamList(pt, filter.get(s), s, uplink, username, password));
        }
        for (String s: filter.keySet()) {
            if (!xtreamLists.containsKey(s)) {
                xtreamLists.put(s,new XtreamList(null, filter.get(s), s, uplink, username, password));
            }
        }
    }

    String getUplinkUrl(String listName) {
        return uplink + "player_api.php?username=" + username + "&password=" + password + "&action=get_" + listName;
    }

    public void reset(Filter filter) {
        for (String s: filter.keySet()) {
            if (xtreamLists.containsKey(s)) {
                Logger.info("Updating: {}", s);
                xtreamLists.get(s).reset(filter.get(s));
            } else {
                xtreamLists.put(s,new XtreamList(null, filter.get(s), s, uplink,  username, password));
            }
        }
    }

    String apiCmd(String cmd) throws IOException {
        if (cmd == null) {
            return metadata.stringJSON();
        } else if (cmd.startsWith("get_") && xtreamLists.containsKey(cmd.substring(4))) {
            return xtreamLists.get( cmd.substring(4) ).stringJSON();
        }
        return null;
    }

    String getM3U() throws IOException {
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