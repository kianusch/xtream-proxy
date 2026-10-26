package my.xtream;

import helper.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Filter {
    private static final Pattern UMLAUT_PATTERN = Pattern.compile("[ÄÖÜäöüß]");
    private static final Map<String, String> UMLAUT_MAP = Map.of("Ä", "A", "Ö", "O", "Ü", "U", "ä", "a", "ö", "o", "ü", "u", "ß", "s");
    private final Map<String, List<Map.Entry<String, String>>> filter;

    Filter(Path path) throws IOException {
        Map<String, List<Map.Entry<String, String>>> result = new HashMap<>();
        String file = null;
        String key = null;
        int keyIndent = -1;

        for (String line : Files.readAllLines(path)) {
            if (line.isBlank()) continue;

            int indent = line.length() - line.stripLeading().length();
            String text = line.strip();
            if (text.startsWith("//")) continue;

            if (indent == 0) {                       // level 1: file/list
                file = text;
                key = null;
                keyIndent = -1;
                if (!result.containsKey(file)) {
                    result.put(file, new ArrayList<>());
                }
            } else if (keyIndent == -1 || indent == keyIndent) {   // level 2: field
                keyIndent = indent;
                key = text;
            } else {
                result.get(file).add(new AbstractMap.SimpleEntry<>(key, text));
            }
        }
        this.filter = result;
    }

    static private void deUmlaut(String field, List<Map<String, Object>> entries) {
        entries.replaceAll(entryMap -> {
            String fieldValue = (String) entryMap.get(field);
            if (fieldValue != null) {
                entryMap.put(field, UMLAUT_PATTERN.matcher(fieldValue).replaceAll(match -> UMLAUT_MAP.get(match.group())));
            }
            return entryMap;
        });
    }

    static private void ms(String mField, String field, String cmd, List<Map<String, Object>> entries, Map<String, Map<String, Object>> parentList) {
        // first try ... S/MATCHFIELD/MATCHPATTERN/MATCH/DST/
        // final Pattern pattern4 = Pattern.compile("^ms/([^/]*)/([^/]*)/(.+)/$");
        final Pattern pattern4 = Pattern.compile("^ms/((?:[^/\\\\]|\\\\.)+)/((?:[^/\\\\]|\\\\.)*)/((?:[^/\\\\]|\\\\.)*)/$");
        final Matcher matcher4 = pattern4.matcher(cmd);

        if (matcher4.matches()) {
            String target = matcher4.group(2);
            String replacement = matcher4.group(3);
            //String tField = matcher4.group(1);

            Pattern mPattern = Pattern.compile(matcher4.group(1));
            Matcher mMatcher = mPattern.matcher("");

            boolean mIsParent = !entries.getFirst().containsKey(mField);

            // Replace entries
            entries.replaceAll(entryMap -> {
                String value = (String) entryMap.get(field);
                if (value != null) {
                    String mValue = mIsParent ? (String) parentList.get((String) entryMap.get("category_id")).get(mField) : (String) entryMap.get(mField);
                    mMatcher.reset(mValue);
                    // Logger.error("{} -> {}  | {} | {} = {}", matcher4.group(2),mValue,mMatcher.matches(),target,replacement);
                    if (mMatcher.matches()) {
                        value = value.replaceAll(target, replacement);
                        entryMap.put(field, value);
                    }
                }
                return entryMap;
            });
            return;
        }
        Logger.error("Field: {}\nPreFilterField: {}\nFilter: {}", field, mField, cmd);
        throw new IllegalArgumentException("Invalid filter: " + cmd);
    }

    static private void ma(String mField, String field, String cmd, List<Map<String, Object>> entries, Map<String, Map<String, Object>> parentList) {
        // first try ... s/MATCHFIELD/MATCH/DST/
        final Pattern pattern3 = Pattern.compile("^ma/([^/]*)/(.+)/$");
        final Matcher matcher3 = pattern3.matcher(cmd);

        if (matcher3.matches()) {
            Pattern pPattern = Pattern.compile(matcher3.group(1));
            Matcher pMatcher = pPattern.matcher("");
            String replacement = matcher3.group(2);

            boolean pIsParent = !entries.getFirst().containsKey(mField);

            Pattern pattern = Pattern.compile("\\$\\{([^}]*)}");
            Matcher matcher = pattern.matcher(replacement);
            String template = matcher.find() ? matcher.group(1) : null;

            // Replace entries
            entries.replaceAll(entryMap -> {
                String category_id = (String) entryMap.get("category_id");
                Map<String, Object> categories = parentList.get(category_id);
                String condFileValue = pIsParent
                        ? (String) parentList.get(category_id).get(mField)
                        : (String) entryMap.get(mField);
                pMatcher.reset(condFileValue);
                if (pMatcher.matches()) {
                    if (template != null) {
                        String t;
                        if (template.isEmpty()) {
                            t = pIsParent
                                    ? (String) categories.get(mField)
                                    : (String) entryMap.get(mField);
                        } else {
                            t = (String) entryMap.get(template);
                        }
                        String value = replacement.replace("${" + template + "}", t.toLowerCase().replace(" ", "-"));
                        entryMap.put(field, value);
                    } else {
                        entryMap.put(field, replacement);
                    }
                }
                return entryMap;
            });
            return;
        }
        Logger.error("Field: {}\nPreFilterField: {}\nFilter: {}", field, mField, cmd);
        throw new IllegalArgumentException("Invalid filter: " + cmd);
    }
        static private void s(String field, String cmd, List<Map<String, Object>> entries, Map<String, Map<String, Object>> parentList) {
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

    static private void f(String field, String cmd, List<Map<String, Object>> entries) {
        boolean include = cmd.startsWith("i/");

        final Pattern pattern = Pattern.compile("[ie]/(.+)/$");
        final Matcher matcher = pattern.matcher(cmd);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid filter command: " + cmd);

        String regexp = matcher.group(1);

        Pattern pattern1 = Pattern.compile(regexp);

        entries.removeIf(entryMap -> {
            if (entryMap.get(field) == null) {
                return include;
            }
            return include ^ pattern1.matcher((String) entryMap.get(field)).find();
        });
    }

    static private void assign(String field, String tmplate, List<Map<String, Object>> entries, String username, String password, String uplink) {
        String t = tmplate.substring(1).replace("${username}", username).replace("${password}", password).replace("${uplink}", uplink);
        Pattern pattern = Pattern.compile("\\$\\{([^}]*)}");
        Matcher matcher = pattern.matcher(t);
        String template = matcher.find() ? matcher.group(1) : null;
        if (template != null) {
            entries.replaceAll(entryMap -> {
                String tt = String.valueOf(entryMap.get(template));
                String value = t.replace("${" + template + "}", tt);
                entryMap.put(field, value);
                return entryMap;
            });
        }
    }

    static void applyFilter(List<Map.Entry<String, String>> _filter, List<Map<String, Object>> entries, Map<String, Map<String, Object>> parentList, String username, String password, String uplink) {
        if (_filter != null) {
            String match = null;
            for (Map.Entry<String, String> n : _filter) {
                String field = n.getKey();
                if (field.contains(".")) {
                    match = field.substring(0, field.indexOf("."));
                    field = field.substring(field.indexOf(".") + 1);
                }
                String cmdStr = n.getValue();
                //Logger.error("Filter: {} -> {} -> {}", match, field, cmdStr);
                switch (cmdStr) {
                    case String s when s.startsWith("i/") -> f(field, cmdStr, entries);
                    case String s when s.startsWith("e/") -> f(field, cmdStr, entries);
                    case String s when s.startsWith("s/") -> s(field, cmdStr, entries, parentList);
                    case String s when s.startsWith("ms/") -> ms(match, field, cmdStr, entries, parentList);
                    case String s when s.startsWith("ma/") -> ma(match, field, cmdStr, entries, parentList);
                    case String s when s.startsWith("=") -> assign(field, cmdStr, entries, username, password, uplink);
                    case "deumlaut" -> deUmlaut(field, entries);
                    default -> throw new IllegalArgumentException("Invalid command: " + cmdStr);
                }
            }
            AtomicInteger counter = new AtomicInteger(1);
            entries.forEach(row -> row.put("num", counter.getAndIncrement()));
        }
    }


    List<Map.Entry<String, String>> get(String s) {
        return filter.get(s);
    }

    Set<String> keySet() {
        return this.filter.keySet();
    }
}
