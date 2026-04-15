package main.service;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import main.implementation.PUCommsAPIImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EmailApiServer {

    private HttpServer server;
    private final Gson gson = new Gson();
    private final PUCommsAPIImpl commsApi = new PUCommsAPIImpl();

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/comms/email", new EmailHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Email API server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Email API server stopped.");
        }
    }

    private class EmailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"status\":\"method_not_allowed\"}");
                return;
            }

            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<?, ?> json = gson.fromJson(body, Map.class);

                String to = json.get("to") != null ? json.get("to").toString() : null;
                String subject = json.get("subject") != null ? json.get("subject").toString() : null;
                String messageBody = json.get("body") != null ? json.get("body").toString() : null;

                boolean sent = commsApi.sendEmail(to, subject, messageBody);

                if (sent) {
                    sendResponse(exchange, 200, "{\"status\":\"sent\"}");
                } else {
                    sendResponse(exchange, 400, "{\"status\":\"failed\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"status\":\"error\"}");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}