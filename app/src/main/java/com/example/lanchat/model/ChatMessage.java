package com.example.lanchat.model;

public class ChatMessage {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_PHOTO = "photo";
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_VOICE = "voice";

    private long id;
    private String senderId;
    private String senderName;
    private String recipientIp;
    private String body;
    private String type; // text, photo, video, file, voice
    private String filePath;
    private String fileName;
    private long fileSize;
    private long timestamp;
    private boolean isSentByMe;

    public ChatMessage(long id, String senderId, String senderName, String recipientIp, String body,
                       String type, String filePath, String fileName, long fileSize, long timestamp, boolean isSentByMe) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientIp = recipientIp;
        this.body = body;
        this.type = type;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
    }

    public long getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getRecipientIp() { return recipientIp; }
    public String getBody() { return body; }
    public String getType() { return type; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public long getTimestamp() { return timestamp; }
    public boolean isSentByMe() { return isSentByMe; }
}
