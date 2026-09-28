import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class SenderMain {
    public static void main(String[] args) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8081/callback"))
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpClient client = HttpClient.newHttpClient();
        long startNanos = System.nanoTime();

        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            System.out.println("status = " + response.statusCode());
        } catch (HttpTimeoutException e) {
            System.out.println("exception = HttpTimeoutException");
        } finally {
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            System.out.println("elapsedMillis = " + elapsedMillis);
        }
    }
}
