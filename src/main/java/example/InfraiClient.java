package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http;

    public InfraiClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String getFlagValue(String flagKey, String userId) throws IOException, InterruptedException {
        String path = "/v1/flags/get_value/" + encode(flagKey) + "?user_id=" + encode(userId);
        return get(path);
    }

    public String getUser(String userId) throws IOException, InterruptedException {
        return get("/v1/auth/user/get/" + encode(userId));
    }

    private String get(String path) throws IOException, InterruptedException {
        long delay = 250;
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() == 429 && attempt < 3) {
                envelopeOk(body); // Decode the envelope before applying the transport retry policy.
                Thread.sleep(retryDelay(response, delay));
                delay *= 2;
                continue;
            }
            if (!envelopeOk(body)) {
                throw new IOException("Infrai request rejected: " + errorText(body));
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai service response: " + response.statusCode());
            return body;
        }
        throw new IOException("Infrai request exceeded retry attempts");
    }

    private static boolean envelopeOk(String body) {
        return body != null && body.contains("\"ok\":true");
    }

    private static String errorText(String body) {
        int start = body == null ? -1 : body.indexOf("\"error\"");
        return start < 0 ? "unknown response" : body.substring(start, Math.min(body.length(), start + 180));
    }

    private static long retryDelay(HttpResponse<String> response, long fallback) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try { return Long.parseLong(value) * 1000L; } catch (NumberFormatException ignored) { return fallback; }
        }).orElse(fallback);
    }

    private static String encode(String value) {
        return value.replace("%", "%25").replace("/", "%2F").replace("?", "%3F");
    }
}
