package my.xtream;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.regex.Pattern;

public final class Get {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private Get() {
    }

    public static String get(String filePath) throws IOException {
        try {
            return filePath.startsWith("http")
                    ? getFromHttp(mkUrl(filePath))
                    : getFromFile(filePath);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFromHttp(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Upstream returned: " + response.statusCode());
        }

        return response.body();
    }

    public static String getFromFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }

    static String mkUrl(String str) {
        if (!(str.startsWith("http://*.") || str.startsWith("https://*.") || str.startsWith("*.")))
            return str;
        return Pattern.compile("\\*")
                .matcher(str)
                .replaceFirst(java.util.regex.Matcher.quoteReplacement(randomToken()));
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";

    private static String randomToken() {
        StringBuilder sb = new StringBuilder(8);

        for (int i = 0; i < 4; i++) {
            sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }
        for (int i = 0; i < 4; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }

        return sb.toString();
    }
}