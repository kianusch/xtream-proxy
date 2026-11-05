package my.xtream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class XtreamList {
    private final XtreamList parentList;
    private final String uplink;
    private final String listname;
    private final String username;
    private final String password;
    private List<Map<String, Object>> entries;
    private Map<String, Map<String, Object>> entriesMap;
    private JSONArray orig;
    private List<Map.Entry<String, String>> _filter;

    XtreamList(XtreamList parent, List<Map.Entry<String, String>> _filter, String listname, String uplink, String username, String password) {
        this.uplink = uplink.endsWith("/") ? uplink.substring(0, uplink.length() - 1) : uplink;
        this.listname = listname;
        this.username = username;
        this.password = password;
        this._filter = _filter;
        parentList = parent;
        orig = null;
        entries = null;
    }

    private boolean isDirty() {
        return entries == null;
    }

    void mkDirty() {
        entries = null;
    }

    void update() throws IOException {
        if (parentList != null && parentList.isDirty()) {
            parentList.update();
            entries = null;
        }

        if (isDirty()) {
            if (orig == null) {
                String ul = uplink + "/player_api.php?username=" + username + "&password=" + password + "&action=get_" + listname;
                orig = new JSONArray(HttpClient.get(ul));
            }

            entries = filterAndCvt(orig);
            applyFilter();
        }
    }

    public XtreamList reset(List<Map.Entry<String, String>> filter) {
        this._filter = filter;
        entries = null;
        return this;
    }

    List<Map<String, Object>> filterAndCvt(JSONArray al) {
        List<Map<String, Object>> e = new ArrayList<>();
        for (int i = 0; i < al.length(); i++) {
            JSONObject jsonObject = al.getJSONObject(i);
            if (parentList == null || parentList.has(jsonObject.getString("category_id"))) {
                e.add(jsonObject.toMap());
            }
        }
        return e;
    }

    boolean has(String id) {
        return entriesMap.containsKey(id);
    }

    Map<String, Object> get(String id) {
        return entriesMap.get(id);
    }


    void print(Set<Object> filter) {
        if (filter != null) {
            entries.stream().filter(e -> filter.contains(e.get("category_id"))).forEach(System.out::println);
        } else {
            entries.forEach(System.out::println);
        }
    }

    void printJSON(Set<Object> filter) {
        if (filter != null) {
            List<Map<?, ?>> tmp = entries.stream().filter(e -> filter.contains(e.get("category_id"))).collect(Collectors.toList());
            System.out.println(new JSONArray(tmp).toString(2));
        } else {
            entries.forEach(System.out::println);
        }
    }

    String stringJSON() throws IOException {
        update();
        return new JSONArray(entries).toString();
    }

    // #EXTINF:-1 tvg-id="" tvg-name="AL - Baby Bandito (2024) S01 E08" tvg-logo="https://image.tmdb.org/t/p/w185/6vHSGeSrwD6FLqMumSz3fHmKJ2S.jpg" group-title="ALBANIA SERIALE",AL - Baby Bandito (2024) S01 E08
    // http://cf.pro-cdn.me:80/series/48833fd7865b/a5700ffdf3/772734.mp4

    String stringM3U(String url, String postfix) throws IOException {
        update();
        //Map<String, String> filter = categories.getAll("category_id", "category_name");
        //List<Map<?, ?>> tmp = entries.stream().filter(e -> filter.containsKey(e.get("category_id"))).collect(Collectors.toList());
        //return "#EXTM3U\n" + tmp.stream().map(e -> entry2m3u(e,url,postfix,filter.get(e.get("category_id")))).collect(Collectors.joining("\n"));
        return "#EXTM3U\n" + entries.stream().map(e -> entry2m3u(e,url,postfix)).collect(Collectors.joining("\n"));
    }

    String entry2m3u(Map<?,?> m, String url, String postfix) {
        if (parentList == null) return null;
        String s = (String) m.get("category_id");
        String gt= (String) parentList.entriesMap.get(s).get("category_name");
        return "#EXTINF:-1" + " " +
                "tvg-id" + "=\"" + m.get("epg_channel_id") + "\" " +
                "tvg-name" + "=\"" + m.get("name") + "\" " +
                "tvg-logo" + "=\"" + m.get("stream_icon") + "\" " +
                "group-title" + "=\"" + gt + "\"," +
                m.get("name") + "\n" +
                HttpClient.mkUrl(url) + m.get("stream_type") + postfix + m.get("stream_id") + ".ts";
    }

    private Map<String, String> getAll(String keyField, String valueField) {
        return entries.stream().collect(Collectors.toMap(map -> map.get(keyField).toString(), map -> map.get(valueField).toString()));
    }

    public Set<Object> getAll(String field) {
        //System.err.println(entries.stream().map(map -> map.get(field)).collect(Collectors.toSet()));
        return entries.stream().map(map -> map.get(field)).collect(Collectors.toSet());
    }


    public int size() {
        return entries.size();
    }

    Map<String, Map<String, Object>> pEntryMap() {
        if (parentList == null) return null;
        return parentList.entriesMap;
    }

    public XtreamList applyFilter() {
        Filter.applyFilter(_filter, entries, pEntryMap(), username, password, uplink);
        if (parentList == null) {
            entriesMap = entries.stream().collect(Collectors.toMap(
                    row -> (String) row.get("category_id"),
                    row -> row));
        }
        return this;
    }

}