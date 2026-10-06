package my.xtream;

import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Metadata {
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final JSONObject metadata;
    private final JSONObject server_info;
    private final JSONObject user_info;

    Metadata(String filePath) throws IOException {
        metadata = new JSONObject(Get.get(filePath));
        user_info = metadata.getJSONObject("user_info");
        server_info = metadata.getJSONObject("server_info");
    }

    String stringJSON() {
        long timestamp = System.currentTimeMillis() / 1000;
        String formattedDateTime = LocalDateTime.now().format(formatter);

        server_info.put("timestamp_now", timestamp);
        server_info.put("time_now", formattedDateTime);

        return metadata.toString(2);
    }

    void putServerInfo(String key, String value) {
        server_info.put(key, value);
    }

    String getServerInfo(String key) {
        return server_info.getString(key);
    }

    String getUserInfo(String key) {
        return user_info.getString(key);
    }
}