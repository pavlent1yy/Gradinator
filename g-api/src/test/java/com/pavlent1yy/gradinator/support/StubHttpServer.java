package com.pavlent1yy.gradinator.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StubHttpServer implements AutoCloseable {

    private record Response(int status, byte[] body) {}

    private final HttpServer server;
    private final Map<String, Response> responses = new ConcurrentHashMap<>();

    public StubHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            Response response = responses.getOrDefault(
                    exchange.getRequestURI().getPath(), new Response(404, "not found".getBytes()));
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(response.status(), response.body().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.body());
            }
        });
        server.start();
    }

    public StubHttpServer html(String path, String html) {
        responses.put(path, new Response(200, html.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    public StubHttpServer status(String path, int status) {
        responses.put(path, new Response(status, "error".getBytes()));
        return this;
    }

    public String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
