package my.xtream;

import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Metadata {
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final Map<String, Object> metadata;
    private final Map<String, Object> server_info;
    private final Map<String, Object> user_info;

    Metadata(String filePath) throws IOException {
        metadata = new JSONObject(Get.get(filePath)).toMap();
        server_info = (Map<String, Object>) metadata.get("server_info");
        if (filePath.startsWith("http://*")) {
            String u1 = filePath.replaceFirst("^https?://", "");
            // Remove path starting with first '/'
            int slash = u1.indexOf('/');
            String u2 = (slash >= 0) ? u1.substring(0, slash) : u1;
            //server_info.put("url",u2);
            server_info.put("url","192.168.2.16");
        }
        user_info = (Map<String, Object>) metadata.get("user_info");
    }

    String stringJSON() {
        long timestamp = System.currentTimeMillis() / 1000;
        String formattedDateTime = LocalDateTime.now().format(formatter);

        server_info.put("timestamp_now", timestamp);
        server_info.put("time_now", formattedDateTime);

        metadata.put("server_info", server_info);

        return new JSONObject(metadata).toString(2);
    }

    void putServerInfo(String key, String value) {
        server_info.put(key, value);
    }

    Object getServerInfo(String key) {
        return server_info.get(key);
    }

    Object getUserInfo(String key) {
        return user_info.get(key);
    }
}