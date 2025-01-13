package com.example.wischeduler;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class AddFriendsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);

        // Sample list of friends
        List<String> friends = Arrays.asList(
                "Rajit Mahesh",
                "Ayush Jadhav",
                "Joseph Tao",
                "Kanish Vuyyuru",
                "Sumanth Karnati",
                "Charles Huai",
                "Sophia Weston",
                "Heather Ridha",
                "Al Han",
                "Yoyo Qian"
        );

        // Set up RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerFriendsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        FriendsAdapter adapter = new FriendsAdapter(friends);
        recyclerView.setAdapter(adapter);

        // Back button functionality
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Set up Invite Friends button
        findViewById(R.id.btnInviteFriends).setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            String shareMessage = "Check out Wischeduler! It's amazing for organizing schedules. Download here: [App Link]";
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, "Invite a Friend");
            startActivity(shareIntent);
        });
    }
}
