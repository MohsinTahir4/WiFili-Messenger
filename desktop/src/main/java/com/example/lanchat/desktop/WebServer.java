package com.example.lanchat.desktop;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebServer {

    private static final List<String> messageHistory = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            DesktopLanManager lanManager = DesktopLanManager.getInstance();
            lanManager.start();
            lanManager.registerListener(new DesktopLanManager.Listener() {
                @Override
                public void onPeersUpdated(List<DesktopLanManager.DesktopPeer> peerList) {}

                @Override
                public void onMessageReceived(String senderIp, String senderName, String body, String type, String filePath, String fileName) {
                    String msgJson = "{\"sender\":\"" + escapeJson(senderName) + "\",\"body\":\"" + escapeJson(body) + "\",\"type\":\"received\",\"time\":\"Now\"}";
                    messageHistory.add(msgJson);
                }
            });

            int activePort = findAvailablePort(8080);
            HttpServer server = HttpServer.create(new InetSocketAddress(activePort), 0);

            // Serve HTML Web Interface
            server.createContext("/", new StaticFileHandler());

            // API Endpoints
            server.createContext("/api/peers", new PeersHandler());
            server.createContext("/api/messages", new MessagesHandler());
            server.createContext("/api/send", new SendHandler());

            server.setExecutor(null);
            server.start();

            String localIp = getLocalIp();
            System.out.println("====================================================");
            System.out.println("🚀 WiFili Messenger Web Server Running!");
            System.out.println("👉 Open in Chrome/Edge/Firefox (Local PC): http://localhost:" + activePort);
            System.out.println("👉 Open from other phones/PCs on Wi-Fi: http://" + localIp + ":" + activePort);
            System.out.println("====================================================");

            // Try to open Chrome / Default browser automatically
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + activePort));
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("Failed to start Web Server: " + e.getMessage());
        }
    }

    private static int findAvailablePort(int defaultPort) {
        int[] portsToTry = {defaultPort, 8081, 8082, 8085, 8088, 9090, 0};
        for (int p : portsToTry) {
            try (ServerSocket ss = new ServerSocket(p)) {
                return ss.getLocalPort();
            } catch (Exception ignored) {}
        }
        return defaultPort;
    }

    private static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = null;

            // 1. Try reading directly from inside JAR classpath resource
            try (InputStream is = WebServer.class.getResourceAsStream("/web/index.html")) {
                if (is != null) {
                    bytes = is.readAllBytes();
                }
            } catch (Exception ignored) {}

            // 2. Fallback to external disk file
            if (bytes == null) {
                File htmlFile = new File("web/index.html");
                if (!htmlFile.exists()) {
                    htmlFile = new File("../web/index.html");
                }
                if (htmlFile.exists()) {
                    bytes = java.nio.file.Files.readAllBytes(htmlFile.toPath());
                }
            }

            // 3. Fallback inline HTML
            if (bytes == null) {
                String fallback = "<html><body><h1>WiFili Messenger Web Server</h1><p>Running offline.</p></body></html>";
                bytes = fallback.getBytes(StandardCharsets.UTF_8);
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class PeersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DesktopLanManager lanManager = DesktopLanManager.getInstance();
            List<DesktopLanManager.DesktopPeer> peers = lanManager.getPeersList();

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < peers.size(); i++) {
                DesktopLanManager.DesktopPeer p = peers.get(i);
                sb.append("{\"id\":\"").append(p.getId())
                  .append("\",\"name\":\"").append(p.getName())
                  .append("\",\"ip\":\"").append(p.getIp())
                  .append("\",\"status\":\"Online\"}");
                if (i < peers.size() - 1) sb.append(",");
            }
            sb.append("]");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    static class MessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < messageHistory.size(); i++) {
                sb.append(messageHistory.get(i));
                if (i < messageHistory.size() - 1) sb.append(",");
            }
            sb.append("]");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    static class SendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                String destIp = extractJsonValue(body, "ip");
                String text = extractJsonValue(body, "text");

                if (destIp != null && text != null) {
                    DesktopLanManager.getInstance().sendText(destIp, text, null, null);
                    String msgJson = "{\"sender\":\"You\",\"body\":\"" + escapeJson(text) + "\",\"type\":\"sent\",\"time\":\"Now\"}";
                    messageHistory.add(msgJson);
                }

                String resp = "{\"status\":\"ok\"}";
                byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    private static String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) return null;
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
