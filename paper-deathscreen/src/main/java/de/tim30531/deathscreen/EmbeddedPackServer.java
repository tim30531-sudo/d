package de.tim30531.deathscreen;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class EmbeddedPackServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;
    private final Path packFile;

    EmbeddedPackServer(String bindAddress, int port, Path packFile) throws IOException {
        this.packFile = packFile;
        this.server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "deathscreen-pack-http");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(executor);
        this.server.createContext("/death-knight-pack.zip", this::servePack);
    }

    void start() {
        server.start();
    }

    private void servePack(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if (!Files.isRegularFile(packFile)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        long size = Files.size(packFile);
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=death-knight-pack.zip");
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=300");

        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, size);
        try (OutputStream output = exchange.getResponseBody()) {
            Files.copy(packFile, output);
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
