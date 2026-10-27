package com.example.lanchat.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lanchat.R;
import com.example.lanchat.model.ChatMessage;
import com.example.lanchat.network.LanManager;
import com.example.lanchat.utils.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<Long, Integer> progressMap = new ConcurrentHashMap<>();
    private final Map<String, Bitmap> videoThumbnailCache = new HashMap<>();

    private MediaPlayer mediaPlayer;
    private String currentlyPlayingPath = null;
    private ViewHolder currentPlayingHolder = null;
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private Runnable seekRunnable;

    public void setMessages(List<ChatMessage> newMessages) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void updateProgress(long messageId, int progress) {
        progressMap.put(messageId, progress);
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId() == messageId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByMe() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_SENT) ? R.layout.item_chat_message_sent : R.layout.item_chat_message_received;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        holder.tvBody.setText(msg.getBody());
        holder.tvTime.setText(NetworkUtils.formatTime(msg.getTimestamp()));

        if (holder.tvSender != null) {
            holder.tvSender.setText(msg.getSenderName());
        }

        String type = msg.getType() != null ? msg.getType() : "text";
        String filePath = msg.getFilePath();

        if ("voice".equals(type)) {
            holder.tvBody.setVisibility(View.GONE);
        } else {
            holder.tvBody.setVisibility(View.VISIBLE);
        }

        // Check active transfer progress
        Integer progress = progressMap.get(msg.getId());
        if (progress == null) {
            int currentProg = LanManager.getInstance(holder.itemView.getContext()).getProgress(msg.getId());
            if (currentProg >= 0) {
                progress = currentProg;
            }
        }

        boolean isTransferring = (progress != null && progress >= 0 && progress < 100);

        if (isTransferring) {
            if (holder.layoutProgress != null) {
                holder.layoutProgress.setVisibility(View.VISIBLE);
                if (holder.tvProgressLabel != null) {
                    String actionStr = msg.isSentByMe() ? "Sending " : "Receiving ";
                    String nameStr = msg.getFileName() != null ? msg.getFileName() : "file";
                    holder.tvProgressLabel.setText(actionStr + nameStr + "...");
                }
                if (holder.tvProgressPercent != null) {
                    holder.tvProgressPercent.setText(progress + "%");
                }
                if (holder.pbTransfer != null) {
                    holder.pbTransfer.setProgress(progress);
                }
            }
            holder.ivPhotoPreview.setVisibility(View.GONE);
            if (holder.layoutVideoPreview != null) holder.layoutVideoPreview.setVisibility(View.GONE);
            holder.layoutFileCard.setVisibility(View.GONE);
            holder.layoutMedia.setVisibility(View.GONE);
            return;
        } else {
            if (holder.layoutProgress != null) {
                holder.layoutProgress.setVisibility(View.GONE);
            }
        }

        // 1. Photo Preview
        if ("photo".equals(type) && filePath != null) {
            File imgFile = new File(filePath);
            if (imgFile.exists()) {
                holder.ivPhotoPreview.setVisibility(View.VISIBLE);
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    holder.ivPhotoPreview.setImageBitmap(bitmap);
                } else {
                    holder.ivPhotoPreview.setImageResource(R.drawable.ic_image);
                }
                holder.ivPhotoPreview.setOnClickListener(v -> FileAdapter.openFile(v.getContext(), imgFile, type));
            } else {
                holder.ivPhotoPreview.setVisibility(View.GONE);
            }
        } else {
            holder.ivPhotoPreview.setVisibility(View.GONE);
        }

        // 2. Video Preview (With thumbnail & play button overlay)
        if ("video".equals(type) && filePath != null) {
            File videoFile = new File(filePath);
            if (videoFile.exists() && holder.layoutVideoPreview != null) {
                holder.layoutVideoPreview.setVisibility(View.VISIBLE);
                Bitmap thumb = getVideoThumbnail(filePath);
                if (thumb != null && holder.ivVideoThumbnail != null) {
                    holder.ivVideoThumbnail.setImageBitmap(thumb);
                } else if (holder.ivVideoThumbnail != null) {
                    holder.ivVideoThumbnail.setImageResource(R.drawable.ic_video);
                }
                holder.layoutVideoPreview.setOnClickListener(v -> FileAdapter.openFile(v.getContext(), videoFile, type));
            } else if (holder.layoutVideoPreview != null) {
                holder.layoutVideoPreview.setVisibility(View.GONE);
            }
        } else if (holder.layoutVideoPreview != null) {
            holder.layoutVideoPreview.setVisibility(View.GONE);
        }

        // 3. WhatsApp style File Card (Only for non-video files or when video preview unavailable)
        if ("file".equals(type) && filePath != null) {
            holder.layoutFileCard.setVisibility(View.VISIBLE);
            File file = new File(filePath);
            String displayFileName = msg.getFileName() != null ? msg.getFileName() : file.getName();
            holder.tvFileName.setText(displayFileName);
            long fSize = file.exists() ? file.length() : msg.getFileSize();

            int iconRes = R.drawable.ic_folder;
            String lower = displayFileName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".pdf")) {
                iconRes = R.drawable.ic_pdf;
            }

            holder.ivFileIcon.setImageResource(iconRes);
            holder.tvFileSize.setText(NetworkUtils.formatFileSize(fSize) + " • File");

            holder.layoutFileCard.setOnClickListener(v -> FileAdapter.openFile(v.getContext(), file, type));
        } else {
            holder.layoutFileCard.setVisibility(View.GONE);
        }

        // 4. Voice Note Player
        if ("voice".equals(type) && filePath != null) {
            holder.layoutMedia.setVisibility(View.VISIBLE);
            boolean isPlaying = filePath.equals(currentlyPlayingPath);
            holder.btnPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            holder.btnPlay.setOnClickListener(v -> toggleVoicePlayback(v.getContext(), filePath, holder));

            if (!isPlaying && holder.seekBarAudio != null) {
                holder.seekBarAudio.setProgress(0);
            }
        } else {
            holder.layoutMedia.setVisibility(View.GONE);
        }
    }

    private Bitmap getVideoThumbnail(String path) {
        if (path == null) return null;
        if (videoThumbnailCache.containsKey(path)) {
            return videoThumbnailCache.get(path);
        }
        Bitmap bitmap = null;
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            retriever.release();
        } catch (Exception ignored) {}
        if (bitmap != null) {
            videoThumbnailCache.put(path, bitmap);
        }
        return bitmap;
    }

    private void toggleVoicePlayback(Context context, String filePath, ViewHolder holder) {
        if (filePath == null) return;
        File f = new File(filePath);
        if (!f.exists()) {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentlyPlayingPath != null && currentlyPlayingPath.equals(filePath) && mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopAudioPlayback();
            return;
        }

        stopAudioPlayback();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentlyPlayingPath = filePath;
            currentPlayingHolder = holder;
            holder.btnPlay.setImageResource(R.drawable.ic_pause);

            if (holder.seekBarAudio != null) {
                holder.seekBarAudio.setMax(mediaPlayer.getDuration());
            }

            seekRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying() && currentPlayingHolder != null) {
                        int pos = mediaPlayer.getCurrentPosition();
                        if (currentPlayingHolder.seekBarAudio != null) {
                            currentPlayingHolder.seekBarAudio.setProgress(pos);
                        }
                        int secs = pos / 1000;
                        if (currentPlayingHolder.tvMediaInfo != null) {
                            currentPlayingHolder.tvMediaInfo.setText(String.format(Locale.getDefault(), "%d:%02d", secs / 60, secs % 60));
                        }
                        seekHandler.postDelayed(this, 200);
                    }
                }
            };
            seekHandler.post(seekRunnable);

            mediaPlayer.setOnCompletionListener(mp -> stopAudioPlayback());
        } catch (Exception e) {
            Toast.makeText(context, "Cannot play audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudioPlayback() {
        seekHandler.removeCallbacks(seekRunnable);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (currentPlayingHolder != null) {
            currentPlayingHolder.btnPlay.setImageResource(R.drawable.ic_play);
            if (currentPlayingHolder.seekBarAudio != null) {
                currentPlayingHolder.seekBarAudio.setProgress(0);
            }
            currentPlayingHolder = null;
        }
        currentlyPlayingPath = null;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBody, tvTime, tvSender, tvMediaInfo, tvFileName, tvFileSize;
        TextView tvProgressLabel, tvProgressPercent;
        ImageView ivPhotoPreview, ivFileIcon, ivVideoThumbnail;
        LinearLayout layoutMedia, layoutFileCard, layoutProgress;
        FrameLayout layoutVideoPreview;
        ImageButton btnPlay;
        SeekBar seekBarAudio;
        ProgressBar pbTransfer;

        ViewHolder(View itemView) {
            super(itemView);
            tvBody = itemView.findViewById(R.id.tv_msg_body);
            tvTime = itemView.findViewById(R.id.tv_msg_time);
            tvSender = itemView.findViewById(R.id.tv_sender_name);
            tvMediaInfo = itemView.findViewById(R.id.tv_media_info);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            tvProgressLabel = itemView.findViewById(R.id.tv_progress_label);
            tvProgressPercent = itemView.findViewById(R.id.tv_progress_percent);

            ivPhotoPreview = itemView.findViewById(R.id.iv_photo_preview);
            ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            ivVideoThumbnail = itemView.findViewById(R.id.iv_video_thumbnail);

            layoutMedia = itemView.findViewById(R.id.layout_media);
            layoutFileCard = itemView.findViewById(R.id.layout_file_card);
            layoutProgress = itemView.findViewById(R.id.layout_progress);
            layoutVideoPreview = itemView.findViewById(R.id.layout_video_preview);

            btnPlay = itemView.findViewById(R.id.btn_play_audio);
            seekBarAudio = itemView.findViewById(R.id.seekbar_audio);
            pbTransfer = itemView.findViewById(R.id.pb_transfer);
        }
    }
}
