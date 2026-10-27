package com.example.lanchat.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.lanchat.model.ChatMessage;
import com.example.lanchat.model.SharedFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "wifi_chat.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_MESSAGES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sender_id TEXT, " +
                "sender_name TEXT, " +
                "recipient_ip TEXT, " +
                "body TEXT, " +
                "type TEXT, " +
                "file_path TEXT, " +
                "file_name TEXT, " +
                "file_size INTEGER, " +
                "timestamp INTEGER, " +
                "is_sent INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    public long addMessage(String senderId, String senderName, String recipientIp, String body,
                           String type, String filePath, String fileName, long fileSize, boolean isSent) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sender_id", senderId);
        cv.put("sender_name", senderName);
        cv.put("recipient_ip", recipientIp);
        cv.put("body", body);
        cv.put("type", type);
        cv.put("file_path", filePath);
        cv.put("file_name", fileName);
        cv.put("file_size", fileSize);
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("is_sent", isSent ? 1 : 0);
        return db.insert(TABLE_MESSAGES, null, cv);
    }

    public List<ChatMessage> getMessagesForPeer(String peerIp) {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MESSAGES +
                " WHERE recipient_ip=? OR sender_id=? ORDER BY id ASC", new String[]{peerIp, peerIp});
        if (cursor.moveToFirst()) {
            do {
                list.add(new ChatMessage(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("sender_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("sender_name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("recipient_ip")),
                        cursor.getString(cursor.getColumnIndexOrThrow("body")),
                        cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        cursor.getString(cursor.getColumnIndexOrThrow("file_path")),
                        cursor.getString(cursor.getColumnIndexOrThrow("file_name")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("file_size")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("is_sent")) == 1
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public String getLastMessageForPeer(String peerIp) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT body, type FROM " + TABLE_MESSAGES +
                " WHERE recipient_ip=? OR sender_id=? ORDER BY id DESC LIMIT 1", new String[]{peerIp, peerIp});
        String msg = "No messages yet";
        if (cursor.moveToFirst()) {
            String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            if ("photo".equals(type)) msg = "📷 Photo";
            else if ("video".equals(type)) msg = "🎥 Video";
            else if ("voice".equals(type)) msg = "🎙️ Voice message";
            else if ("file".equals(type)) msg = "📄 File";
            else msg = body;
        }
        cursor.close();
        return msg;
    }

    public List<SharedFile> getAllSharedFiles() {
        List<SharedFile> files = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT sender_name, file_path, file_name, file_size, type, timestamp FROM " +
                TABLE_MESSAGES + " WHERE type != 'text' AND file_path IS NOT NULL ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndexOrThrow("file_path"));
                if (path != null) {
                    File f = new File(path);
                    files.add(new SharedFile(
                            f,
                            cursor.getString(cursor.getColumnIndexOrThrow("file_name")),
                            cursor.getLong(cursor.getColumnIndexOrThrow("file_size")),
                            cursor.getString(cursor.getColumnIndexOrThrow("type")),
                            cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                            cursor.getString(cursor.getColumnIndexOrThrow("sender_name"))
                    ));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return files;
    }
}
