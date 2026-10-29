package com.example.lanchat.desktop;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DesktopLanManager {
    public static final int DISCOVERY_PORT = 45454;
    public static final int CHAT_PORT = 45455;
    public static final int AUDIO_PORT = 45457;
    public static final int VIDEO_PORT = 45458;

    private static DesktopLanManager instance;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<String, DesktopPeer> peers = new ConcurrentHashMap<>();

    private String deviceId;
    private String deviceName;
    private volatile boolean running = true;

    // Call Engine Fields
    private volatile boolean isCallActive = false;
    private volatile boolean isMuted = false;
    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
    private DatagramSocket audioSendSocket;
    private DatagramSocket audioRecvSocket;
    private DatagramSocket videoRecvSocket;

    public interface Listener {
        void onPeersUpdated(List<DesktopPeer> peerList);
        void onMessageReceived(String senderIp, String senderName, String body, String type, String filePath, String fileName);
        default void onProgressUpdated(String peerIp, int progress) {}
        default void onCallOfferReceived(String senderIp, String senderName, String callType) {}
        default void onCallAcceptReceived(String senderIp) {}
        default void onCallDeclineReceived(String senderIp) {}
        default void onCallEndReceived(String senderIp) {}
        default void onRemoteVideoFrame(byte[] jpegData, int camId) {}
    }

    public static class DesktopPeer {
        private String id;
        private String name;
        private String ip;
        private long lastSeen;

        public DesktopPeer(String id, String name, String ip) {
            this.id = id;
            this.name = name;
            this.ip = ip;
            this.lastSeen = System.currentTimeMillis();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public long getLastSeen() { return lastSeen; }
        public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    }

    private final List<Listener> listeners = new ArrayList<>();

    private DesktopLanManager() {
        this.deviceId = UUID.randomUUID().toString().substring(0, 8);
        String osName = System.getProperty("os.name", "Desktop").toLowerCase(Locale.ROOT);
        String prefix = osName.contains("win") ? "Win-User-" : osName.contains("nux") || osName.contains("nix") ? "Linux-User-" : "Desktop-User-";
        this.deviceName = prefix + deviceId;
    }

    public static synchronized DesktopLanManager getInstance() {
        if (instance == null) {
            instance = new DesktopLanManager();
        }
        return instance;
    }

    public void registerListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void unregisterListener(Listener l) {
        listeners.remove(l);
    }

    public void setDeviceName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.deviceName = name.trim();
        }
    }

    public String getDeviceName() { return deviceName; }
    public String getDeviceId() { return deviceId; }

    public List<DesktopPeer> getPeersList() {
        // Prune peers not seen in 15 seconds
        long now = System.currentTimeMillis();
        peers.values().removeIf(p -> (now - p.getLastSeen()) > 15000);
        return new ArrayList<>(peers.values());
    }

    public void start() {
        running = true;
        startServer();
        startDiscovery();
    }

    public void announce() {
        pool.execute(() -> {
            try (DatagramSocket s = new DatagramSocket()) {
                s.setBroadcast(true);
                byte[] data = ("LANCHAT|" + deviceId + "|" + deviceName).getBytes("UTF-8");
                
                // 1. Send to limited broadcast
                try {
                    DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                    s.send(p);
                } catch (Exception ignored) {}

                // 2. Send to local subnet broadcasts
                try {
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface ni = interfaces.nextElement();
                        if (ni.isUp() && !ni.isLoopback()) {
                            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                                InetAddress broadcast = ia.getBroadcast();
                                if (broadcast != null) {
                                    DatagramPacket p = new DatagramPacket(data, data.length, broadcast, DISCOVERY_PORT);
                                    s.send(p);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

            } catch (Exception ignored) {}
        });
    }

    private void startServer() {
        pool.execute(() -> {
            try (ServerSocket server = new ServerSocket()) {
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(CHAT_PORT));
                while (running) {
                    Socket socket = server.accept();
                    pool.execute(() -> receive(socket));
                }
            } catch (Exception ignored) {}
        });
    }

    private void startDiscovery() {
        pool.execute(() -> {
            try (DatagramSocket ds = new DatagramSocket(null)) {
                ds.setReuseAddress(true);
                ds.bind(new InetSocketAddress(DISCOVERY_PORT));
                ds.setBroadcast(true);
                byte[] buf = new byte[512];
                while (running) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    ds.setSoTimeout(1200);
                    try {
                        ds.receive(p);
                        String line = new String(p.getData(), 0, p.getLength(), "UTF-8");
                        String[] parts = line.split("\\|", 3);
                        if (parts.length == 3 && !parts[1].equals(deviceId)) {
                            String peerIp = p.getAddress().getHostAddress();
                            DesktopPeer peer = peers.get(parts[1]);
                            if (peer == null) {
                                peer = new DesktopPeer(parts[1], parts[2], peerIp);
                                peers.put(parts[1], peer);
                            } else {
                                peer.setName(parts[2]);
                                peer.setIp(peerIp);
                                peer.setLastSeen(System.currentTimeMillis());
                            }
                            notifyPeersUpdated();
                        }
                    } catch (SocketTimeoutException ignored) {}
                    announce();
                }
            } catch (Exception ignored) {}
        });
    }

    private void notifyPeersUpdated() {
        List<DesktopPeer> list = getPeersList();
        for (Listener l : listeners) l.onPeersUpdated(list);
    }

    public void sendText(String destIp, String text, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        pool.execute(() -> {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(destIp, CHAT_PORT), 3500);
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                o.writeInt(1); // 1 = text
                o.writeUTF(deviceName);
                o.writeUTF(text);
                o.flush();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception e) {
                if (onError != null) onError.accept("Send failed: " + e.getMessage());
            }
        });
    }

    public void sendFile(String destIp, File file, String type, java.util.function.Consumer<Integer> onProgress, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        pool.execute(() -> {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(destIp, CHAT_PORT), 6000);
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                int typeInt = "voice".equals(type) ? 3 : "photo".equals(type) ? 2 : "video".equals(type) ? 4 : 5;
                o.writeInt(typeInt);
                o.writeUTF(deviceName);
                o.writeUTF(file.getName());
                o.writeLong(file.length());

                try (FileInputStream in = new FileInputStream(file)) {
                    byte[] b = new byte[8192];
                    long sent = 0;
                    long total = file.length();
                    int n;
                    int lastPct = -1;
                    while ((n = in.read(b)) > 0) {
                        o.write(b, 0, n);
                        sent += n;
                        if (total > 0 && onProgress != null) {
                            int pct = (int) ((sent * 100) / total);
                            if (pct != lastPct) {
                                lastPct = pct;
                                onProgress.accept(pct);
                            }
                        }
                    }
                }
                o.flush();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception e) {
                if (onError != null) onError.accept("Transfer failed: " + e.getMessage());
            }
        });
    }

    public void sendCallSignal(String destIp, int signalType, String callType) {
        pool.execute(() -> {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(destIp, CHAT_PORT), 3500);
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                o.writeInt(signalType);
                o.writeUTF(deviceName);
                o.writeUTF(callType != null ? callType : "audio");
                o.flush();
            } catch (Exception ignored) {}
        });
    }

    private void receive(Socket socket) {
        try (Socket s = socket) {
            DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            int type = in.readInt();
            String sender = in.readUTF();
            String senderIp = s.getInetAddress().getHostAddress();

            if (type == 10) { // CALL_OFFER
                String callType = in.readUTF();
                for (Listener l : listeners) l.onCallOfferReceived(senderIp, sender, callType);
                return;
            } else if (type == 11) { // CALL_ACCEPT
                for (Listener l : listeners) l.onCallAcceptReceived(senderIp);
                return;
            } else if (type == 12) { // CALL_DECLINE
                for (Listener l : listeners) l.onCallDeclineReceived(senderIp);
                return;
            } else if (type == 13) { // CALL_END
                for (Listener l : listeners) l.onCallEndReceived(senderIp);
                return;
            }

            if (type == 1) { // text
                String body = in.readUTF();
                for (Listener l : listeners) l.onMessageReceived(senderIp, sender, body, "text", null, null);
            } else { // photo/video/voice/file
                String name = in.readUTF();
                long len = in.readLong();
                File downloadFolder = new File(System.getProperty("user.home"), "Downloads/LanChatDownloads");
                if (!downloadFolder.exists()) downloadFolder.mkdirs();

                File file = new File(downloadFolder, System.currentTimeMillis() + "_" + name);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    byte[] b = new byte[8192];
                    long remain = len;
                    while (remain > 0) {
                        int n = in.read(b, 0, (int) Math.min(b.length, remain));
                        if (n < 0) throw new EOFException();
                        out.write(b, 0, n);
                        remain -= n;
                    }
                }
                String msgType = (type == 2) ? "photo" : (type == 3) ? "voice" : (type == 4) ? "video" : "file";
                for (Listener l : listeners) l.onMessageReceived(senderIp, sender, "Received " + msgType + ": " + name, msgType, file.getAbsolutePath(), name);
            }
        } catch (Exception ignored) {}
    }

    // Audio Call Methods for Linux and Windows Desktop
    public void startAudioCall(String destIp) {
        this.isCallActive = true;
        this.isMuted = false;

        pool.execute(() -> {
            try {
                AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);

                micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
                speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);

                micLine.open(format);
                speakerLine.open(format);

                micLine.start();
                speakerLine.start();

                audioSendSocket = new DatagramSocket();
                audioRecvSocket = new DatagramSocket(null);
                audioRecvSocket.setReuseAddress(true);
                audioRecvSocket.bind(new InetSocketAddress(AUDIO_PORT));

                InetAddress destAddr = InetAddress.getByName(destIp);

                // Mic Sender Loop
                pool.execute(() -> {
                    byte[] buf = new byte[640];
                    while (isCallActive) {
                        int read = micLine.read(buf, 0, buf.length);
                        if (read > 0 && !isMuted && audioSendSocket != null) {
                            try {
                                DatagramPacket p = new DatagramPacket(buf, read, destAddr, AUDIO_PORT);
                                audioSendSocket.send(p);
                            } catch (Exception ignored) {}
                        }
                    }
                });

                // Speaker Receiver Loop
                pool.execute(() -> {
                    byte[] recvBuf = new byte[2048];
                    while (isCallActive) {
                        try {
                            DatagramPacket p = new DatagramPacket(recvBuf, recvBuf.length);
                            audioRecvSocket.receive(p);
                            if (p.getLength() > 0 && speakerLine != null) {
                                speakerLine.write(p.getData(), 0, p.getLength());
                            }
                        } catch (Exception ignored) {}
                    }
                });

            } catch (Exception ignored) {}
        });

        // Video Frame Receiver Loop
        pool.execute(() -> {
            try {
                videoRecvSocket = new DatagramSocket(null);
                videoRecvSocket.setReuseAddress(true);
                videoRecvSocket.bind(new InetSocketAddress(VIDEO_PORT));

                byte[] recvBuf = new byte[65535];
                while (isCallActive) {
                    try {
                        DatagramPacket p = new DatagramPacket(recvBuf, recvBuf.length);
                        videoRecvSocket.receive(p);
                        int len = p.getLength();
                        if (len > 4) {
                            byte[] data = p.getData();
                            int camId = ByteBuffer.wrap(data, 0, 4).getInt();
                            byte[] jpegData = new byte[len - 4];
                            System.arraycopy(data, 4, jpegData, 0, len - 4);
                            for (Listener l : listeners) {
                                l.onRemoteVideoFrame(jpegData, camId);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        });
    }

    public void stopCall() {
        isCallActive = false;
        if (micLine != null) { try { micLine.stop(); micLine.close(); } catch (Exception ignored) {} micLine = null; }
        if (mutexed()) { isMuted = false; }
        if (speakerLine != null) { try { speakerLine.stop(); speakerLine.close(); } catch (Exception ignored) {} speakerLine = null; }
        if (audioSendSocket != null) { try { audioSendSocket.close(); } catch (Exception ignored) {} audioSendSocket = null; }
        if (audioRecvSocket != null) { try { audioRecvSocket.close(); } catch (Exception ignored) {} audioRecvSocket = null; }
        if (videoRecvSocket != null) { try { videoRecvSocket.close(); } catch (Exception ignored) {} videoRecvSocket = null; }
    }

    private boolean mutexed() { return isMuted; }

    public void toggleMute() { isMuted = !isMuted; }
    public boolean isMuted() { return isMuted; }

    public void stop() {
        running = false;
        stopCall();
        pool.shutdownNow();
    }
}
