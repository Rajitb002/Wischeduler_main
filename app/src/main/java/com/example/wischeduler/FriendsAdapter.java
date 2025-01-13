package com.example.wischeduler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder> {

    private final List<String> friendsList;

    // Constructor to pass the list of friends
    public FriendsAdapter(List<String> friendsList) {
        this.friendsList = friendsList;
    }

    @NonNull
    @Override
    public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each friend card
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendsViewHolder holder, int position) {
        // Bind data to the views
        String friendName = friendsList.get(position);
        holder.tvFriendName.setText(friendName);

        // Set a placeholder profile picture (you can replace this with unique images later)
        holder.imgProfile.setImageResource(R.drawable.ic_person);
    }

    @Override
    public int getItemCount() {
        // Return the total number of items
        return friendsList.size();
    }

    static class FriendsViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendName;
        ImageView imgProfile;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            imgProfile = itemView.findViewById(R.id.imgProfile);
        }
    }
}
