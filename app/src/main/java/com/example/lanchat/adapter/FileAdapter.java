package com.example.lanchat.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lanchat.R;
import com.example.lanchat.VideoPlayerActivity;
import com.example.lanchat.model.SharedFile;
import com.example.lanchat.utils.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(SharedFile file);
    }

    private final List<SharedFile> files = new ArrayList<>();
    private final OnFileClickListener listener;

    public FileAdapter(OnFileClickListener listener) {
        this.listener = listener;
    }

    public void setFiles(List<SharedFile> newFiles) {
        this.files.clear();
        if (newFiles != null) {
            this.files.addAll(newFiles);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SharedFile sharedFile = files.get(position);

        holder.tvTitle.setText(sharedFile.getName());
        String typeStr = sharedFile.getType() != null ? sharedFile.getType().toUpperCase() : "FILE";
        holder.tvSub.setText(NetworkUtils.formatFileSize(sharedFile.getSize()) + " • " + typeStr);

        int iconRes = R.drawable.ic_folder;
        if ("photo".equalsIgnoreCase(sharedFile.getType())) iconRes = R.drawable.ic_image;
        else if ("video".equalsIgnoreCase(sharedFile.getType())) iconRes = R.drawable.ic_video;
        else if ("voice".equalsIgnoreCase(sharedFile.getType())) iconRes = R.drawable.ic_mic_red;
        else if ("pdf".equalsIgnoreCase(sharedFile.getType()) || sharedFile.getName().endsWith(".pdf")) iconRes = R.drawable.ic_pdf;

        holder.ivIcon.setImageResource(iconRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(sharedFile);
            } else {
                openFile(v.getContext(), sharedFile.getFile(), sharedFile.getType());
            }
        });
    }

    public static void openFile(Context context, File file, String type) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("video".equalsIgnoreCase(type) || isVideoFile(file.getName())) {
            try {
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("file_path", file.getAbsolutePath());
                intent.putExtra("file_name", file.getName());
                if (!(context instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }

        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            String mime = context.getContentResolver().getType(uri);
            if (mime == null) {
                String name = file.getName().toLowerCase(Locale.ROOT);
                if ("video".equals(type) || isVideoFile(name)) mime = "video/*";
                else if ("photo".equals(type) || isImageFile(name)) mime = "image/*";
                else if (name.endsWith(".pdf")) mime = "application/pdf";
                else if (name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".wav")) mime = "audio/*";
                else if (name.endsWith(".docx") || name.endsWith(".doc")) mime = "application/msword";
                else if (name.endsWith(".xlsx") || name.endsWith(".xls")) mime = "application/vnd.ms-excel";
                else if (name.endsWith(".pptx") || name.endsWith(".ppt")) mime = "application/vnd.ms-powerpoint";
                else if (name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".json")) mime = "text/plain";
                else if (name.endsWith(".apk")) mime = "application/vnd.android.package-archive";
                else if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")) mime = "application/zip";
                else mime = "*/*";
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(Intent.createChooser(intent, "Open " + file.getName()));
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean isVideoFile(String name) {
        if (name == null) return false;
        String l = name.toLowerCase(Locale.ROOT);
        return l.endsWith(".mp4") || l.endsWith(".mkv") || l.endsWith(".3gp") || l.endsWith(".webm") || l.endsWith(".avi");
    }

    private static boolean isImageFile(String name) {
        if (name == null) return false;
        String l = name.toLowerCase(Locale.ROOT);
        return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".gif") || l.endsWith(".webp");
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvSub;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            tvTitle = itemView.findViewById(R.id.tv_file_title);
            tvSub = itemView.findViewById(R.id.tv_file_sub);
        }
    }
}
