package my.xtream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Src {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();

    public static String get(String filePath) throws IOException {
//        System.out.println("Get from " + filePath);

        if (filePath.startsWith("http")) {
            return getFromHTTP(filePath);
        }

        return getFromFile(filePath);
    }


    public static String getFromHTTP(String path) throws IOException {
        Request request = new Request.Builder().url(path).build();

        try (Response response = client.newCall(request).execute()) {
            int responseCode = response.code();

            if (responseCode == 200) {
                return response.body().string();
            }

            throw new IOException("Upstream returned: "+responseCode);
        }
    }

    public static String getFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

}