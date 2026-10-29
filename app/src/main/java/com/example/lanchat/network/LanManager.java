package com.example.lanchat.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.example.lanchat.db.DatabaseHelper;
import com.example.lanchat.model.Peer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LanManager {
    public static final int DISCOVERY_PORT = 45454;
    public static final int CHAT_PORT = 45455;

    public static final int TYPE_CALL_OFFER = 10;
    public static final int TYPE_CALL_ACCEPT = 11;
    public static final int TYPE_CALL_DECLINE = 12;
    public static final int TYPE_CALL_END = 13;

    private static LanManager instance;
    private final Context context;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final Map<Long, Integer> activeProgressMap = new ConcurrentHashMap<>();
    private final DatabaseHelper db;

    private String deviceId;
    private String deviceName;
    private volatile boolean running = true;
    private WifiManager.MulticastLock multicastLock;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Listener {
        void onPeersUpdated(List<Peer> peerList);
        void onMessageReceived(String senderIp);
        default void onProgressUpdated(long messageId, int progress, String peerIp) {}
        default void onCallOfferReceived(String senderIp, String senderName, String callType) {}
        default void onCallAcceptReceived(String senderIp) {}
        default void onCallDeclineReceived(String senderIp) {}
        default void onCallEndReceived(String senderIp) {}
    }

    private final List<Listener> listeners = new ArrayList<>();

    private LanManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = new DatabaseHelper(context);
        this.deviceId = UUID.randomUUID().toString().substring(0, 8);
        this.deviceName = "User-" + deviceId;
    }

    public static synchronized LanManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanManager(context);
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

    public List<Peer> getPeersList() {
        return new ArrayList<>(peers.values());
    }

    public int getProgress(long messageId) {
        return activeProgressMap.getOrDefault(messageId, -1);
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

                // 2. Send to local subnet broadcasts across all network interfaces
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
            try (ServerSocket server = new ServerSocket(CHAT_PORT)) {
                server.setReuseAddress(true);
                while (running) {
                    Socket socket = server.accept();
                    pool.execute(() -> receive(socket));
                }
            } catch (Exception ignored) {}
        });
    }

    private void startDiscovery() {
        pool.execute(() -> {
            try {
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    multicastLock = wm.createMulticastLock("lanchat-discovery");
                    multicastLock.setReferenceCounted(false);
                    multicastLock.acquire();
                }
                try (DatagramSocket ds = new DatagramSocket(DISCOVERY_PORT)) {
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
                                Peer peer = peers.get(parts[1]);
                                if (peer == null) {
                                    peer = new Peer(parts[1], parts[2], peerIp);
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
                }
            } catch (Exception ignored) {}
        });
    }

    private void notifyPeersUpdated() {
        List<Peer> list = getPeersList();
        for (Peer p : list) {
            p.setLastMessage(db.getLastMessageForPeer(p.getIp()));
        }
        mainHandler.post(() -> {
            for (Listener l : listeners) l.onPeersUpdated(list);
        });
    }

    private void notifyMessageReceived(String ip) {
        notifyPeersUpdated();
        mainHandler.post(() -> {
            for (Listener l : listeners) l.onMessageReceived(ip);
        });
    }

    private void notifyProgressUpdated(long messageId, int progress, String peerIp) {
        mainHandler.post(() -> {
            for (Listener l : listeners) l.onProgressUpdated(messageId, progress, peerIp);
        });
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
                db.addMessage(deviceId, deviceName, destIp, text, "text", null, null, 0, true);
                notifyMessageReceived(destIp);
                if (onSuccess != null) mainHandler.post(onSuccess);
            } catch (Exception e) {
                if (onError != null) mainHandler.post(() -> onError.accept("Send failed: " + e.getMessage()));
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

    public interface ProgressListener {
        void onProgress(int percent);
    }

    public void sendFile(String destIp, File file, String type, ProgressListener listener, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        pool.execute(() -> {
            long msgId = -1;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(destIp, CHAT_PORT), 6000);
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                int typeInt = "voice".equals(type) ? 3 : "photo".equals(type) ? 2 : "video".equals(type) ? 4 : 5;
                o.writeInt(typeInt);
                o.writeUTF(deviceName);
                o.writeUTF(file.getName());
                o.writeLong(file.length());

                msgId = db.addMessage(deviceId, deviceName, destIp, "Sending " + type + ": " + file.getName(),
                        type, file.getAbsolutePath(), file.getName(), file.length(), true);
                activeProgressMap.put(msgId, 0);
                notifyMessageReceived(destIp);

                try (FileInputStream in = new FileInputStream(file)) {
                    byte[] b = new byte[8192];
                    long sent = 0;
                    long total = file.length();
                    int n;
                    int lastPct = -1;
                    while ((n = in.read(b)) > 0) {
                        o.write(b, 0, n);
                        sent += n;
                        if (total > 0) {
                            int pct = (int) ((sent * 100) / total);
                            if (pct != lastPct) {
                                lastPct = pct;
                                activeProgressMap.put(msgId, pct);
                                final long finalMsgId = msgId;
                                mainHandler.post(() -> {
                                    if (listener != null) listener.onProgress(pct);
                                    notifyProgressUpdated(finalMsgId, pct, destIp);
                                });
                            }
                        }
                    }
                }
                o.flush();
                activeProgressMap.put(msgId, 100);
                notifyProgressUpdated(msgId, 100, destIp);

                final long finalMsgId = msgId;
                mainHandler.postDelayed(() -> {
                    activeProgressMap.remove(finalMsgId);
                    notifyMessageReceived(destIp);
                }, 300);

                if (onSuccess != null) mainHandler.post(onSuccess);
            } catch (Exception e) {
                if (msgId != -1) {
                    activeProgressMap.remove(msgId);
                    notifyMessageReceived(destIp);
                }
                if (onError != null) mainHandler.post(() -> onError.accept("Transfer failed: " + e.getMessage()));
            }
        });
    }

    private void receive(Socket socket) {
        long msgId = -1;
        String senderIp = null;
        try (Socket s = socket) {
            DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            int type = in.readInt();
            String sender = in.readUTF();
            senderIp = s.getInetAddress().getHostAddress();

            if (type == TYPE_CALL_OFFER) {
                String callType = in.readUTF();
                final String finalSenderIp = senderIp;
                mainHandler.post(() -> {
                    for (Listener l : listeners) l.onCallOfferReceived(finalSenderIp, sender, callType);
                });
                return;
            } else if (type == TYPE_CALL_ACCEPT) {
                final String finalSenderIp = senderIp;
                mainHandler.post(() -> {
                    for (Listener l : listeners) l.onCallAcceptReceived(finalSenderIp);
                });
                return;
            } else if (type == TYPE_CALL_DECLINE) {
                final String finalSenderIp = senderIp;
                mainHandler.post(() -> {
                    for (Listener l : listeners) l.onCallDeclineReceived(finalSenderIp);
                });
                return;
            } else if (type == TYPE_CALL_END) {
                final String finalSenderIp = senderIp;
                mainHandler.post(() -> {
                    for (Listener l : listeners) l.onCallEndReceived(finalSenderIp);
                });
                return;
            }

            if (type == 1) { // text
                String body = in.readUTF();
                db.addMessage(sender, sender, senderIp, body, "text", null, null, 0, false);
            } else { // photo/video/voice/file
                String name = in.readUTF();
                long len = in.readLong();
                if (len < 0 || len > 500 * 1024 * 1024) throw new IOException("File size limit");
                String safeName = name.replaceAll("[^A-Za-z0-9._-]", "_");
                File file = new File(context.getFilesDir(), System.currentTimeMillis() + "_" + safeName);

                String msgType = (type == 2) ? "photo" : (type == 3) ? "voice" : (type == 4) ? "video" : "file";
                msgId = db.addMessage(sender, sender, senderIp, "Receiving " + msgType + ": " + name,
                        msgType, file.getAbsolutePath(), name, len, false);
                activeProgressMap.put(msgId, 0);
                notifyMessageReceived(senderIp);

                try (FileOutputStream out = new FileOutputStream(file)) {
                    byte[] b = new byte[8192];
                    long remain = len;
                    int lastPct = -1;
                    while (remain > 0) {
                        int n = in.read(b, 0, (int) Math.min(b.length, remain));
                        if (n < 0) throw new EOFException();
                        out.write(b, 0, n);
                        remain -= n;
                        if (len > 0) {
                            int pct = (int) (((len - remain) * 100) / len);
                            if (pct != lastPct) {
                                lastPct = pct;
                                activeProgressMap.put(msgId, pct);
                                final long finalMsgId = msgId;
                                final String finalSenderIp = senderIp;
                                mainHandler.post(() -> notifyProgressUpdated(finalMsgId, pct, finalSenderIp));
                            }
                        }
                    }
                }
                activeProgressMap.put(msgId, 100);
                notifyProgressUpdated(msgId, 100, senderIp);

                final long finalMsgId = msgId;
                final String finalSenderIp = senderIp;
                mainHandler.postDelayed(() -> {
                    activeProgressMap.remove(finalMsgId);
                    notifyMessageReceived(finalSenderIp);
                }, 300);
            }
            notifyMessageReceived(senderIp);
        } catch (Exception e) {
            if (msgId != -1) {
                activeProgressMap.remove(msgId);
                if (senderIp != null) notifyMessageReceived(senderIp);
            }
        }
    }

    public void stop() {
        running = false;
        try { if (multicastLock != null && multicastLock.isHeld()) multicastLock.release(); } catch (Exception ignored) {}
        pool.shutdownNow();
    }
}
