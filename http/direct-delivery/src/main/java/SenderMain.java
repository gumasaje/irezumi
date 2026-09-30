import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;

public class SenderMain {
    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.out.println("사용법: SenderMain EventID <ex: event-001>");
            return;
        }

        String eventId = args[0];

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8081/callback"))
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("X-Event-Id", eventId)
                .build();

        HttpClient client = HttpClient.newHttpClient();
        long startNanos = System.nanoTime();

        System.out.println("ID: " + eventId);
        System.out.println("전송 시도 시각: " + Instant.now());

        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            System.out.println("status = " + response.statusCode());
        } catch (HttpTimeoutException e) {
            System.out.println("exception = HttpTimeoutException");
            System.out.println("timeout 확인 시각: " + Instant.now());
        } finally {
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            System.out.println("elapsedMillis = " + elapsedMillis);
        }
    }
}
