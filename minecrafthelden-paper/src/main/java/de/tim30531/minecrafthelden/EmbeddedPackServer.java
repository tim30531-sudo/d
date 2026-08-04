package de.tim30531.minecrafthelden;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

final class EmbeddedPackServer implements AutoCloseable {
    private final HttpServer server;
    private final Path pack;

    EmbeddedPackServer(String bindAddress, int port, Path pack) throws IOException {
        this.pack = pack;
        server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.createContext("/minecrafthelden-pack.zip", this::serve);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    void start() { server.start(); }

    private void serve(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (!Files.isRegularFile(pack)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            long length = Files.size(pack);
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(exchange.getRequestMethod()) ? -1 : length);
            if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                Files.copy(pack, exchange.getResponseBody());
            }
        }
    }

    @Override public void close() { server.stop(0); }
}
