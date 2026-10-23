package my.xtream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Filter {
    private final Map<String, Map<String, List<String>>> filter;

    Filter(Path path) throws IOException {
        Map<String, Map<String, List<String>>> result = new HashMap<>();
        String file = null;
        String key = null;
        int keyIndent = -1;

        for (String line : Files.readAllLines(path)) {
            if (line.isBlank()) continue;

            int indent = line.length() - line.stripLeading().length();
            String text = line.strip();

            if (indent == 0) {                       // level 1: file/list
                file = text;
                key = null;
                keyIndent = -1;
                if (!result.containsKey(file)) {
                    result.put(file, new LinkedHashMap<>());
                }
            } else if (keyIndent == -1 || indent == keyIndent) {   // level 2: field
                keyIndent = indent;
                key = text;
                if (!result.get(file).containsKey(file)) {
                    result.get(file).put(key, new ArrayList<>());
                }
            } else if (!text.startsWith("//")) {
                result.get(file).get(key).add(text);
            }
        }
        this.filter = result;
    }

    boolean has(String s) {
        return filter.containsKey(s);
    }

    Map<String, List<String>> get(String s) {
        return filter.get(s);
    }

    Set<String> keySet() {
        return this.filter.keySet();
    }
}
