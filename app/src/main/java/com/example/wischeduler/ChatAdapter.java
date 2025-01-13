package com.example.wischeduler;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        // Set message text and timestamp
        holder.chatBubble.setText(message.getMessage());
        holder.timestamp.setText(message.getTimestamp());

        // Use different backgrounds for bot and user messages
        if (message.isBot()) {
            holder.chatBubble.setBackgroundResource(R.drawable.bubble_bot);
            alignToLeft(holder.chatBubble, holder.timestamp);
        } else {
            holder.chatBubble.setBackgroundResource(R.drawable.bubble_user);
            alignToRight(holder.chatBubble, holder.timestamp);
        }
    }


    private void alignToLeft(TextView chatBubble, TextView timestamp) {
        // Align chat bubble to the left
        LinearLayout.LayoutParams bubbleParams = (LinearLayout.LayoutParams) chatBubble.getLayoutParams();
        bubbleParams.gravity = Gravity.START;
        chatBubble.setLayoutParams(bubbleParams);

        // Align timestamp to the left
        LinearLayout.LayoutParams timestampParams = (LinearLayout.LayoutParams) timestamp.getLayoutParams();
        timestampParams.gravity = Gravity.START;
        timestamp.setLayoutParams(timestampParams);
    }

    private void alignToRight(TextView chatBubble, TextView timestamp) {
        // Align chat bubble to the right
        LinearLayout.LayoutParams bubbleParams = (LinearLayout.LayoutParams) chatBubble.getLayoutParams();
        bubbleParams.gravity = Gravity.END;
        chatBubble.setLayoutParams(bubbleParams);

        // Align timestamp to the right
        LinearLayout.LayoutParams timestampParams = (LinearLayout.LayoutParams) timestamp.getLayoutParams();
        timestampParams.gravity = Gravity.END;
        timestamp.setLayoutParams(timestampParams);
    }



    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView chatBubble;
        TextView timestamp;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatBubble = itemView.findViewById(R.id.chatBubble);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}
