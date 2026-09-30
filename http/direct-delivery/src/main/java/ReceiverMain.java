import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceiverMain {
    public static void main(String[] args) throws IOException {
        if (args.length != 1
                || !("normal".equals(args[0])
                || "fail-before".equals(args[0])
                || "delay-before".equals(args[0])
                || "delay-after".equals(args[0]))
        ) {
            System.out.println("사용법: ReceiverMain Mode <normal or fail-before or delay-before or delay-after>");
            return;
        }

        String mode = args[0];
        AtomicInteger count = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8081), 0);

        server.createContext("/callback", exchange -> {
            System.out.println("callback 도착: " + Instant.now());
            System.out.println("ID: " + exchange.getRequestHeaders().getFirst("X-Event-Id"));
            System.out.println("mode = " + mode);

            if ("fail-before".equals(mode)) {
                System.out.println("처리 건수: " + count.get());
                System.out.println("returningStatus = 500");
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }

            if ("delay-before".equals(mode)) {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
            }


            System.out.println("처리 시작: " + Instant.now());
            System.out.println("처리 건수: " + count.incrementAndGet());
            System.out.println("처리 완료: " + Instant.now());

            if ("delay-after".equals(mode)) {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
            }

            System.out.println("응답 시도: " + Instant.now());
            System.out.println("returningStatus = 204");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });

        server.start();
        System.out.println("server started: 127.0.0.1:8081, mode = " + mode);
    }
}
