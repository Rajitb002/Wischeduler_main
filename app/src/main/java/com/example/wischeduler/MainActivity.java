package com.example.wischeduler;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        // Splash screen delay (2 seconds)
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
            startActivity(intent);
            finish();
        }, 2000); // 2000 ms = 2 seconds
    }
}
