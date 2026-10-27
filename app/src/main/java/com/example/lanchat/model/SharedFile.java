package com.example.lanchat.model;

import java.io.File;

public class SharedFile {
    private File file;
    private String name;
    private long size;
    private String type; // pdf, image, video, voice, doc, zip, file
    private long timestamp;
    private String senderName;

    public SharedFile(File file, String name, long size, String type, long timestamp, String senderName) {
        this.file = file;
        this.name = name;
        this.size = size;
        this.type = type;
        this.timestamp = timestamp;
        this.senderName = senderName;
    }

    public File getFile() { return file; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public String getSenderName() { return senderName; }
}
