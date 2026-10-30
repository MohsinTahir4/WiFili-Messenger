package com.example.lanchat.desktop;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {

    private static final int DISCOVERY_PORT = 45454;
    private static final int CHAT_PORT = 45455;
    private static final String DEVICE_ID = UUID.randomUUID().toString().substring(0, 8);
    private static String deviceName = "Web-User-" + DEVICE_ID;

    private static final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private static final List<String> messageHistory = new CopyOnWriteArrayList<>();
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static class Peer {
        String id, name, ip;
        Peer(String id, String name, String ip) {
            this.id = id; this.name = name; this.ip = ip;
        }
    }

    public static void main(String[] args) {
        startP2PNetworking();

        try {
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
            System.out.println("👉 Open from Android / other devices on Wi-Fi: http://" + localIp + ":" + activePort);
            System.out.println("====================================================");

            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + activePort));
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("Failed to start Web Server: " + e.getMessage());
        }
    }

    private static void startP2PNetworking() {
        // 1. Start TCP Server for incoming messages from Android
        pool.execute(() -> {
            try (ServerSocket server = new ServerSocket(CHAT_PORT)) {
                server.setReuseAddress(true);
                while (true) {
                    Socket socket = server.accept();
                    pool.execute(() -> receiveMessage(socket));
                }
            } catch (Exception ignored) {}
        });

        // 2. Start UDP Discovery (Broadcast & Receive)
        pool.execute(() -> {
            try {
                WifiMulticastLockWrapper.acquire();
                try (DatagramSocket ds = new DatagramSocket(DISCOVERY_PORT)) {
                    ds.setBroadcast(true);
                    byte[] buf = new byte[512];
                    while (true) {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        ds.setSoTimeout(1500);
                        try {
                            ds.receive(p);
                            String line = new String(p.getData(), 0, p.getLength(), "UTF-8");
                            String[] parts = line.split("\\|", 3);
                            if (parts.length == 3 && !parts[1].equals(DEVICE_ID)) {
                                peers.put(parts[1], new Peer(parts[1], parts[2], p.getAddress().getHostAddress()));
                            }
                        } catch (SocketTimeoutException ignored) {}

                        // Send broadcast announcement
                        try (DatagramSocket broadcastSock = new DatagramSocket()) {
                            broadcastSock.setBroadcast(true);
                            byte[] data = ("LANCHAT|" + DEVICE_ID + "|" + deviceName).getBytes("UTF-8");
                            DatagramPacket pkt = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                            broadcastSock.send(pkt);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private static void receiveMessage(Socket socket) {
        try (Socket s = socket) {
            DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            int type = in.readInt();
            String sender = in.readUTF();
            String senderIp = s.getInetAddress().getHostAddress();

            if (type == 1) { // text
                String body = in.readUTF();
                String json = "{\"sender\":\"" + escapeJson(sender) + "\",\"body\":\"" + escapeJson(body) + "\",\"type\":\"received\",\"time\":\"Now\"}";
                messageHistory.add(json);
            }
        } catch (Exception ignored) {}
    }

    private static void sendTextToAndroid(String destIp, String text) {
        pool.execute(() -> {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(destIp, CHAT_PORT), 3500);
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                o.writeInt(1); // text
                o.writeUTF(deviceName);
                o.writeUTF(text);
                o.flush();
            } catch (Exception ignored) {}
        });
    }

    private static class WifiMulticastLockWrapper {
        static void acquire() {
            try {
                // No-op on standard desktop JVM, supports Android if run there
            } catch (Exception ignored) {}
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
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = null;

            // 1. Try reading from classpath resource
            try (InputStream is = WebServer.class.getResourceAsStream("/web/index.html")) {
                if (is != null) {
                    bytes = is.readAllBytes();
                }
            } catch (Exception ignored) {}

            // 2. Fallback to external disk file
            if (bytes == null) {
                File htmlFile = new File("web/src/main/resources/web/index.html");
                if (!htmlFile.exists()) {
                    htmlFile = new File("index.html");
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
            StringBuilder sb = new StringBuilder("[");
            List<Peer> peerList = new ArrayList<>(peers.values());
            for (int i = 0; i < peerList.size(); i++) {
                Peer p = peerList.get(i);
                sb.append("{\"id\":\"").append(p.id)
                  .append("\",\"name\":\"").append(p.name)
                  .append("\",\"ip\":\"").append(p.ip)
                  .append("\",\"status\":\"Online\"}");
                if (i < peerList.size() - 1) sb.append(",");
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
                    sendTextToAndroid(destIp, text);
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
