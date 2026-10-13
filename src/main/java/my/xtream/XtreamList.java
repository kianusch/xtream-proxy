package my.xtream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XtreamList {
    private final static Map<String, String> joins = Map.of(
            "live_streams", "live_categories",
            "series", "series_categories",
            "vod_streams", "vod_categories");

    private final List<Map<String, Object>> entries;
    private final Map<String, Map<String, Object>> parent;

    XtreamList(String filePath, XtreamList parent) throws IOException {
        if (parent != null) {
            this.parent = parent.copy();
        } else {
            this.parent = null;
        }
        entries = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(HttpClient.get(filePath));
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (this.parent == null || this.parent.containsKey(jsonObject.getString("category_id"))) {
                entries.add(jsonObject.toMap());
            }
        }
    }

    public static String getCat(String file) {
        return joins.get(file);
    }

    Map<String, Map<String, Object>> copy() {
        return entries.stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("category_id"),
                        row -> row
                ));
    }

    XtreamList set(String cmd) {
        final Pattern fieldsAndReplacementsPattern = Pattern.compile("(~?\\w+)/([^/]*)/\\s*(\\w+)/(.+)/$");
        final Matcher fieldsAndReplacementsMatcher = fieldsAndReplacementsPattern.matcher(cmd);
        if (fieldsAndReplacementsMatcher.matches()) {
            String cField = fieldsAndReplacementsMatcher.group(1);
            String condRegex = fieldsAndReplacementsMatcher.group(2);
            String targetField = fieldsAndReplacementsMatcher.group(3);
            String targetValue = fieldsAndReplacementsMatcher.group(4);
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
                    var v = parent.get((String) entryMap.get("category_id"));
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
                                t = (String) parent.get((String) entryMap.get("category_id")).get(condField);
                            } else {
                                t = (String) entryMap.get(condField);
                            }
                        } else {
                            if (template.startsWith("_")) {
                                t = (String) parent.get((String) entryMap.get("category_id")).get(template.substring(1));
                            } else {
                                t = (String) entryMap.get(template);
                            }
                        }
                        String value = targetValue.replace("${" + template + "}", t.toLowerCase().replace(" ", "-"));
                        entryMap.put(targetField, value);
                    } else {
                        entryMap.put(targetField, targetValue);
                    }
                }
                return entryMap;
            });
        }
        return this;
    }

    XtreamList replace(String cmd) {
        // First Pattern: Extract fields and replacements
        final Pattern fieldsAndReplacementsPattern = Pattern.compile("(~?[|\\-\\w]+)((?:(/[^/]+/[^/]*/)(?:,|$))*)");
        final Matcher fieldsAndReplacementsMatcher = fieldsAndReplacementsPattern.matcher(cmd);

        if (fieldsAndReplacementsMatcher.matches()) {
            String fields = fieldsAndReplacementsMatcher.group(1);
            String replacements = fieldsAndReplacementsMatcher.group(2);

            // Compile the second pattern to extract individual replacements
            final Pattern replacementsPattern = Pattern.compile("/([^/]+)/([^/]*)/(?:,|$)");
            final Matcher replacementsMatcher = replacementsPattern.matcher(replacements);

            // Replace entries
            if (fields.startsWith("~")) {
                String f = fields.substring(1);
                parent.values().forEach(entryMap -> {
                    for (String field : f.split("[|]")) {
                        Object rawValue = entryMap.get(field);

                        if (rawValue instanceof String value) {
                            replacementsMatcher.reset();

                            while (replacementsMatcher.find()) {
                                String target = replacementsMatcher.group(1);
                                String replacement = replacementsMatcher.group(2);
                                value = value.replaceAll(target, replacement);
                            }

                            entryMap.put(field, value);
                        }
                    }
                });
                System.err.println(new JSONObject(parent).toString(2));
            } else {
                entries.replaceAll(entryMap -> {
                    for (String field : fields.split("[|]")) {
                        String value = (String) entryMap.get(field);
                        if (value != null) {
                            replacementsMatcher.reset();
                            while (replacementsMatcher.find()) {
                                String target = replacementsMatcher.group(1);
                                String replacement = replacementsMatcher.group(2);
                                value = value.replaceAll(target, replacement);
                            }
                            entryMap.put(field, value);
                        }
                    }
                    return entryMap;
                });
            }
            return this;
        }

        // Second Pattern: Handle field mapping replacement
        final Pattern fieldMappingPattern = Pattern.compile("([\\-\\w]+)/([\\-\\w]+)/([^/]+)/([^/]*)/");
        final Matcher fieldMappingMatcher = fieldMappingPattern.matcher(cmd);

        if (fieldMappingMatcher.matches()) {
            String dstField = fieldMappingMatcher.group(1);
            String srcField = fieldMappingMatcher.group(2);
            String srcValue = fieldMappingMatcher.group(3);
            String dstValue = fieldMappingMatcher.group(4);

            // Precompile the source value pattern
            final Pattern srcValuePattern = Pattern.compile(srcValue);

            entries.replaceAll(entryMap -> {
                String fieldValue = (String) entryMap.get(srcField);
                if (fieldValue != null && srcValuePattern.matcher(fieldValue).find()) {
                    entryMap.put(dstField, dstValue);
                }
                return entryMap;
            });
            return this;
        }

        throw new IllegalArgumentException("Invalid replace command: " + cmd);
    }

    String capitalizeString(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    XtreamList deumlaut(String field) {
        entries.replaceAll(entryMap -> {
            String fieldValue = (String) entryMap.get(field);
            if (fieldValue != null) {
                entryMap.put(field, UMLAUT_PATTERN.matcher(fieldValue).replaceAll(match -> UMLAUT_MAP.get(match.group())));
            }
            return entryMap;
        });
        return this;
    }

    XtreamList capitalize(String cmd) {
        // Compile patterns once to improve efficiency
        final Pattern fieldsAndReplacementsPattern = Pattern.compile("([\\-\\w]+)/([^/]+)/");
        final Matcher fieldsAndReplacementsMatcher = fieldsAndReplacementsPattern.matcher(cmd);

        if (fieldsAndReplacementsMatcher.matches()) {
            String field = fieldsAndReplacementsMatcher.group(1);
            String matcherPattern = fieldsAndReplacementsMatcher.group(2);

            final Pattern matchPattern = Pattern.compile(matcherPattern);
            final Pattern uppercaseWordsPattern = Pattern.compile("\\b[A-Z]{3,}");

            entries.replaceAll(entryMap -> {
                String fieldValue = (String) entryMap.get(field);

                if (fieldValue != null) {
                    Matcher matcher = matchPattern.matcher(fieldValue);

                    if (matcher.find()) {
                        String matchedSegmentOrig = matcher.group();
                        String matchedSegmentNew = matchedSegmentOrig;

                        Matcher uppercaseMatcher = uppercaseWordsPattern.matcher(matchedSegmentOrig);
                        while (uppercaseMatcher.find()) {
                            String uppercaseWord = uppercaseMatcher.group();
                            String capitalizedWord = capitalizeString(uppercaseWord);
                            matchedSegmentNew = matchedSegmentNew.replaceFirst(uppercaseWord, capitalizedWord);
                        }

                        fieldValue = fieldValue.replaceFirst(matchedSegmentOrig, matchedSegmentNew);
                        entryMap.put(field, fieldValue);
                    }
                }
                return entryMap;
            });
            return this;
        }

        throw new IllegalArgumentException("Invalid replace command: " + cmd);
    }


    void cmd(String cmd) {
        String[] command = cmd.split("[\\s:]", 2);
        switch (command[0]) {
            case "include" -> filter(command[1], true);
            case "exclude" -> filter(command[1], false);
            case "replace" -> replace(command[1]);
            case "capitalize" -> capitalize(command[1]);
            case "set" -> set(command[1]);
            case "deumlaut" -> deumlaut(command[1]);
            case "noop" -> {
            }
            default -> throw new IllegalArgumentException("Invalid command: " + cmd);
        }
    }

    private void filter(String cmd, boolean include) {
        final Pattern pattern = Pattern.compile("([\\-\\w]+)\\s*([~/=])\\s*(.+)");
        final Matcher matcher = pattern.matcher(cmd);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid filter command: " + cmd);

        String field = matcher.group(1);
        String operator = matcher.group(2);
        String value = matcher.group(3);

        if ("/".equals(operator) && value.endsWith("/")) {
            filterRegexp(field, value.substring(0, value.length() - 1), include);
        } else if ("~".equals(operator)) {
            filterIfContains(field, value, include);
        } else if ("=".equals(operator)) {
            filterIfEquals(field, value, include);
        } else {
            throw new IllegalArgumentException("Invalid filter command: " + cmd);
        }
    }

    private void filterRegexp(String field, String regexp, boolean include) {
        Pattern pattern = Pattern.compile(regexp);

        entries.removeIf(entryMap -> {
            if (entryMap.get(field) == null) {
                return include;
            }
            return include ^ pattern.matcher((String) entryMap.get(field)).find();
        });
    }

    private void filterIfEquals(String field, String csv, boolean include) {
        Set<String> set = Arrays.stream(csv.split("\\|")).collect(Collectors.toSet());
        boolean filterNull = "null".equals(csv);

        entries.removeIf(entryMap -> {
            if (entryMap.get(field) == null) {
                if (filterNull)
                    return false;
                return include;
            }
            return include ^ set.contains(entryMap.get(field));
        });
    }

    private void filterIfContains(String field, String csv, boolean include) {
        boolean startsWith = csv.startsWith("^");
        if (startsWith)
            csv = csv.substring(1);

        boolean endsWith = csv.endsWith("$");
        if (endsWith)
            csv = csv.substring(0, csv.length() - 1);

        Set<String> set = Arrays.stream(csv.split("\\|")).collect(Collectors.toSet());

        for (String s : set) {
            entries.removeIf(entryMap -> {
                if (entryMap.get(field) == null) {
                    return !include;
                } else if (!startsWith && !endsWith) {
                    return include ^ entryMap.get(field).toString().contains(s);
                } else if (startsWith && endsWith) {
                    return include ^ (entryMap.get(field).toString().equals(s));
                } else if (startsWith) {
                    return include ^ entryMap.get(field).toString().startsWith(s);
                } else {
                    return include ^ entryMap.get(field).toString().endsWith(s);
                }
            });
        }
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

    String stringJSON(Set<Object> filter) {
        if (filter != null) {
            List<Map<?, ?>> tmp = entries.stream().filter(e -> filter.contains(e.get("category_id"))).collect(Collectors.toList());
            return new JSONArray(tmp).toString();
        } else {
            return new JSONArray(entries).toString();
        }
    }

    // #EXTINF:-1 tvg-id="" tvg-name="AL - Baby Bandito (2024) S01 E08" tvg-logo="https://image.tmdb.org/t/p/w185/6vHSGeSrwD6FLqMumSz3fHmKJ2S.jpg" group-title="ALBANIA SERIALE",AL - Baby Bandito (2024) S01 E08
    // http://cf.pro-cdn.me:80/series/48833fd7865b/a5700ffdf3/772734.mp4

    String stringM3U(XtreamList categories, String url, String postfix) {
        Map<String, String> filter = categories.getAll("category_id", "category_name");
        List<Map<?, ?>> tmp = entries.stream().filter(e -> filter.containsKey(e.get("category_id"))).collect(Collectors.toList());
        return "#EXTM3U\n" + tmp.stream().map(e -> "#EXTINF:-1 tvg-id=\"\" tvg-name=\"" + e.get("name") + "\" tvg-logo=\"" + e.get("stream_icon") + "\" group-title=\"" + filter.get(e.get("category_id")) + "\"," + e.get("name") + "\n" + HttpClient.mkUrl(url) + e.get("stream_type") + postfix + e.get("stream_id") + ".ts").collect(Collectors.joining("\n"));
    }

    private Map<String, String> getAll(String keyField, String valueField) {
        return entries.stream().collect(Collectors.toMap(map -> map.get(keyField).toString(), map -> map.get(valueField).toString()));
    }

    public Set<Object> getAll(String field) {
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
}