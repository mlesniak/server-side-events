package com.mlesniak.sse;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

record TickEvent(int tick) {
    String toJson() {
        return "{\"tick\":" + tick + "}";
    }
}

record SseEvent(String id, String event, String data) {
    String toSseFormat() {
        return "id: " + id + "\n" +
                "event: " + event + "\n" +
                "data: " + data + "\n\n";
    }
}

public class SseServer {
    public static void main(String[] args) throws Exception {
        // Create a basic HTTP server which is part of the JDK.
        HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
        server.createContext("/events", new SseHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    static class SseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // Set default headers for SSE.
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.getResponseHeaders().set("Connection", "keep-alive");
                exchange.sendResponseHeaders(200, 0);

                try (OutputStream os = exchange.getResponseBody()) {
                    var id = UUID.randomUUID().toString();
                    for (int i = 0; i < 10; i++) {
                        TickEvent tickEvent = new TickEvent(i);
                        SseEvent sseEvent = new SseEvent(
                                id,
                                "tick",
                                tickEvent.toJson()
                        );

                        os.write(sseEvent.toSseFormat().getBytes());
                        os.flush();
                        Thread.sleep(500);
                    }

                    SseEvent closeEvent = new SseEvent("", "close", "");
                    os.write(closeEvent.toSseFormat().getBytes());
                    os.flush();
                }
            } catch (Exception e) {
                // 🙈 ... good enough for us.
                e.printStackTrace();
            }
        }
    }
}
