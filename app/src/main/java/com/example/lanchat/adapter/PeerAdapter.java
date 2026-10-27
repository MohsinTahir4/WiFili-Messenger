package com.example.lanchat.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lanchat.R;
import com.example.lanchat.model.Peer;
import com.example.lanchat.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.ViewHolder> {

    public interface OnPeerClickListener {
        void OnPeerClick(Peer peer);
    }

    private final List<Peer> peers = new ArrayList<>();
    private final OnPeerClickListener listener;

    private static final int[] AVATAR_COLORS = new int[]{
            0xFF6366F1, 0xFFEC4899, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6, 0xFF06B6D4
    };

    public PeerAdapter(OnPeerClickListener listener) {
        this.listener = listener;
    }

    public void setPeers(List<Peer> newPeers) {
        this.peers.clear();
        if (newPeers != null) {
            this.peers.addAll(newPeers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peer_chat, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Peer peer = peers.get(position);

        String initial = (peer.getName() != null && !peer.getName().isEmpty())
                ? peer.getName().substring(0, 1).toUpperCase() : "?";
        holder.tvAvatar.setText(initial);

        int color = AVATAR_COLORS[Math.abs(peer.getName().hashCode()) % AVATAR_COLORS.length];
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        holder.tvAvatar.setBackground(bg);

        holder.tvName.setText(peer.getName());
        holder.tvLastMsg.setText(peer.getLastMessage() != null ? peer.getLastMessage() : "Tap to start chatting");
        holder.tvTime.setText(NetworkUtils.formatTime(peer.getLastSeen()));

        holder.dotOnline.setVisibility(peer.isOnline() ? View.VISIBLE : View.GONE);

        if (peer.getUnreadCount() > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(String.valueOf(peer.getUnreadCount()));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.OnPeerClick(peer);
        });
    }

    @Override
    public int getItemCount() {
        return peers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLastMsg, tvTime, tvUnread;
        View dotOnline;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_peer_avatar);
            tvName = itemView.findViewById(R.id.tv_peer_name);
            tvLastMsg = itemView.findViewById(R.id.tv_peer_last_msg);
            tvTime = itemView.findViewById(R.id.tv_peer_time);
            tvUnread = itemView.findViewById(R.id.tv_peer_unread);
            dotOnline = itemView.findViewById(R.id.dot_online);
        }
    }
}
