package com.example.lanchat.model;

public class Peer {
    private String id;
    private String name;
    private String ip;
    private long lastSeen;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    private boolean isOnline;

    public Peer(String id, String name, String ip) {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.lastSeen = System.currentTimeMillis();
        this.isOnline = true;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}
