package my.xtream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XtreamList {
    private List<Map<String, Object>> entries;
    private Map<String, Map<String, Object>> entriesMap;
    private final XtreamList parentList;

    private JSONArray orig;
    private Map<String, List<String>> _filter;
    private final String uplink;
    private final String listname;
    private final String username;
    private final String password;

    private Instant lastUpdate = Instant.EPOCH;

    private boolean isDirty() {
        return entries == null;
    }

    void update() throws IOException {
        if (parentList != null && parentList.isDirty()) {
            parentList.update();
            entries = null;
        }

        if (Duration.between(lastUpdate, Instant.now()).toSeconds() > 3600) {
            orig = null;
            entries = null;
        }

        if (isDirty()) {
            if (orig == null) {
                String ul = uplink + "/player_api.php?username=" + username + "&password=" + password + "&action=get_" + listname;
                orig = new JSONArray(HttpClient.get(ul));
                lastUpdate = Instant.now();
            }

            entries = filterAndCvt(orig);
            applyFilter();
        }
    }

    XtreamList(XtreamList parent, Map<String, List<String>> _filter, String listname, String uplink, String username, String password) {
        this.uplink = uplink.endsWith("/")?uplink.substring(0, uplink.length()-1):uplink;
        this.listname = listname;
        this.username = username;
        this.password = password;
        this._filter = _filter;
        parentList = parent;
        orig = null;
        entries = null;
    }

    public XtreamList reset(Map<String, List<String>> filter) {
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

    void s(String field, String cmd) {
        // first try ... s/MATCHFIELD/MATCH/DST/
        final Pattern fieldsAndReplacementsPattern = Pattern.compile("^s/(~?\\w+)/([^/]*)/(.+)/$");
        final Matcher fieldsAndReplacementsMatcher = fieldsAndReplacementsPattern.matcher(cmd);

        if (fieldsAndReplacementsMatcher.matches()) {
            String cField = fieldsAndReplacementsMatcher.group(1);
            String condRegex = fieldsAndReplacementsMatcher.group(2);
            String targetValue = fieldsAndReplacementsMatcher.group(3);
            Pattern condPattern = Pattern.compile(condRegex);

            boolean srcIsParent;
            String condField;

            if (cField.startsWith("~")) {
                srcIsParent = true;
                condField = cField.substring(1);
            } else {
                srcIsParent = false;
                condField = cField;
            }

            Pattern pattern = Pattern.compile("\\$\\{([^}]*)}");
            Matcher matcher = pattern.matcher(targetValue);
            String template = matcher.find() ? matcher.group(1) : null;

            // Replace entries
            entries.replaceAll(entryMap -> {
                String condFileValue;
                if (srcIsParent) {
                    var v = parentList.get((String) entryMap.get("category_id"));
                    if (v != null)
                        condFileValue = (String) v.get(condField);
                    else
                        condFileValue = null;
                } else {
                    condFileValue = (String) entryMap.get(condField);
                }
                //System.err.println(condFileValue+" "+condRegex+" "+condPattern.matcher(condFileValue).find());
                if (condFileValue != null && condPattern.matcher(condFileValue).find()) {
                    if (template != null) {
                        String t;
                        if (template.isEmpty()) {
                            if (srcIsParent) {
                                t = (String) parentList.get((String) entryMap.get("category_id")).get(condField);
                            } else {
                                t = (String) entryMap.get(condField);
                            }
                        } else {
                            if (template.startsWith("~")) {
                                t = (String) parentList.get((String) entryMap.get("category_id")).get(template.substring(1));
                            } else {
                                t = (String) entryMap.get(template);
                            }
                        }
                        String value = targetValue.replace("${" + template + "}", t.toLowerCase().replace(" ", "-"));
                        entryMap.put(field, value);
                    } else {
                        entryMap.put(field, targetValue);
                    }
                }
                return entryMap;
            });
            return;
        }

        // ... s/MATCH/DST/
        final Pattern replacementsPattern = Pattern.compile("^s/((?:[^/\\\\]|\\\\.)+)/((?:[^/\\\\]|\\\\.)*)/$");
        final Matcher replacementsMatcher = replacementsPattern.matcher(cmd);

        if (replacementsMatcher.matches()) {
            String target = replacementsMatcher.group(1);
            String replacement = replacementsMatcher.group(2);

            entries.replaceAll(entryMap -> {
                String value = (String) entryMap.get(field);
                if (value != null) {
                    value = value.replaceAll(target, replacement);
                    entryMap.put(field, value);
                }
                return entryMap;
            });
            return;
        }
        throw new IllegalArgumentException("Invalid filter: " + field + "." + cmd);
    }

    void deUmlaut(String field) {
        entries.replaceAll(entryMap -> {
            String fieldValue = (String) entryMap.get(field);
            if (fieldValue != null) {
                entryMap.put(field, UMLAUT_PATTERN.matcher(fieldValue).replaceAll(match -> UMLAUT_MAP.get(match.group())));
            }
            return entryMap;
        });
    }

    void assign(String field, String tmplate) {
            String t = tmplate.substring(1)
                    .replace("${username}", username)
                    .replace("${password}", password)
                    .replace("${uplink}", uplink);
            Pattern pattern = Pattern.compile("\\$\\{([^}]*)}");
            Matcher matcher = pattern.matcher(t);
            String template = matcher.find() ? matcher.group(1) : null;
            if (template != null) {
                entries.replaceAll(entryMap -> {
                    String tt =String.valueOf(entryMap.get(template));
                    String value = t.replace("${" + template + "}", tt);
                    entryMap.put(field, value);
                    return entryMap;
                });
            }
    }

    void cmd(String f, String cmd) {
        switch (cmd) {
            case String s when s.startsWith("i/") -> f(f, cmd, true);
            case String s when s.startsWith("e/") -> f(f, cmd, false);
            case String s when s.startsWith("s/") -> s(f, cmd);
            case String s when s.startsWith("=") -> assign(f, cmd);
            case "deumlaut" -> deUmlaut(f);
            default -> throw new IllegalArgumentException("Invalid command: " + cmd);
        }
    }

    private void f(String field, String cmd, boolean include) {
        final Pattern pattern = Pattern.compile("[ie]/(.+)/$");
        final Matcher matcher = pattern.matcher(cmd);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid filter command: " + cmd);

        String regexp = matcher.group(1);

        Pattern pattern1 = Pattern.compile(regexp);

        entries.removeIf(entryMap -> {
            if (entryMap.get(field) == null) {
                return include;
            }
            return include ^ pattern1.matcher((String) entryMap.get(field)).find();
        });
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

    String stringM3U(XtreamList categories, String url, String postfix) throws IOException {
        update();
        Map<String, String> filter = categories.getAll("category_id", "category_name");
        List<Map<?, ?>> tmp = entries.stream().filter(e -> filter.containsKey(e.get("category_id"))).collect(Collectors.toList());
        return "#EXTM3U\n" + tmp.stream().map(e -> "#EXTINF:-1 tvg-id=\"\" tvg-name=\"" + e.get("name") + "\" tvg-logo=\"" + e.get("stream_icon") + "\" group-title=\"" + filter.get(e.get("category_id")) + "\"," + e.get("name") + "\n" + HttpClient.mkUrl(url) + e.get("stream_type") + postfix + e.get("stream_id") + ".ts").collect(Collectors.joining("\n"));
    }

    private Map<String, String> getAll(String keyField, String valueField) {
        return entries.stream().collect(Collectors.toMap(map -> map.get(keyField).toString(), map -> map.get(valueField).toString()));
    }

    public Set<Object> getAll(String field) {
        //System.err.println(entries.stream().map(map -> map.get(field)).collect(Collectors.toSet()));
        return entries.stream().map(map -> map.get(field)).collect(Collectors.toSet());
    }

    private static final Pattern UMLAUT_PATTERN = Pattern.compile("[ÄÖÜäöüß]");

    private static final Map<String, String> UMLAUT_MAP = Map.of(
            "Ä", "A", "Ö", "O", "Ü", "U",
            "ä", "a", "ö", "o", "ü", "u",
            "ß", "s"
    );

    public int size() {
        return entries.size();
    }

    public XtreamList applyFilter() {
        if (_filter != null) {
            for (String n : _filter.keySet()) {
                for (String cmdStr : _filter.get(n)) {
                    cmd(n, cmdStr);
                }
            }
        }
        AtomicInteger counter = new AtomicInteger(1);
        entries.forEach(row -> row.put("num", counter.getAndIncrement()));

        if (parentList == null) {
            entriesMap = entries.stream()
                    .collect(Collectors.toMap(
                            row -> (String) row.get("category_id"),
                            row -> row
                    ));
        } else {
            entriesMap = null;
        }
        return this;
    }
}