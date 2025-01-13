package com.example.wischeduler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.Nullable;


import java.io.InputStream;


import android.net.Uri;

import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;



public class MenuActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WiSchedulerPrefs";
    private static final String KEY_SELECTED_EMAIL = "selected_email";
    private static final int PICK_FILE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Button to navigate to CalendarActivity
        findViewById(R.id.btnViewCalendar).setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        Button btnOpenChatbot = findViewById(R.id.btnOpenChatbot);
        btnOpenChatbot.setOnClickListener(v -> {
            // Navigate to ChatbotActivity
            Intent intent = new Intent(MenuActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnImportSchedule).setOnClickListener(v -> {
            openFilePicker();
        });


        /**
        findViewById(R.id.btnAddFriends).setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);

            // The message content to share
            String shareMessage = "Check out Wischeduler! It's amazing for organizing schedules. Download here: www.wischedulerapp.somwhere";
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            sendIntent.setType("text/plain");

            // Show the chooser to let the user select the app for sharing
            Intent shareIntent = Intent.createChooser(sendIntent, "Invite a Friend");
            startActivity(shareIntent);
        });
        */

        findViewById(R.id.btnAddFriends).setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, AddFriendsActivity.class);
            startActivity(intent);
        });

        // Button to navigate to PreferencesActivity
        findViewById(R.id.btnPreferences).setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PreferencesActivity.class);
            startActivity(intent);
        });

        // Retrieve the last selected email from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedAccount = prefs.getString(KEY_SELECTED_EMAIL, null);

        // Find the TextView for displaying user name
        TextView userNameTextView = findViewById(R.id.tvUserName);

        // If an account is saved, set the account name as the greeting
        if (selectedAccount != null) {
            userNameTextView.setText("Hi, " + selectedAccount);
        } else {
            userNameTextView.setText("Hi, User!");
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow any file type (or restrict to specific types)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            Toast.makeText(this, "Swipe left to go back without selecting a file.", Toast.LENGTH_SHORT).show();
            startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show();
            Log.e("MenuActivity", "Error opening file picker", e);
        }
    }

    // Handle the result of the file picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // File was selected
                Uri selectedFileUri = data.getData();
                if (selectedFileUri != null) {
                    String fileName = getFileName(selectedFileUri);
                    Toast.makeText(this, "Selected File: " + fileName, Toast.LENGTH_SHORT).show();
                    //saveFileToInternalStorage(selectedFileUri); // Save or process the file
                }
            }
        }
    }


    // Get the file name from the Uri
    private String getFileName(Uri uri) {
        String result = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && ((Cursor) cursor).moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex != -1) {
                    result = cursor.getString(columnIndex);
                }
            }
        }
        return result != null ? result : uri.getLastPathSegment();
    }

    // Read the selected file (for demonstration purposes)
    private void readFile(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                // Example: Read the file content or upload to a server
                Toast.makeText(this, "File read successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
            Log.e("MenuActivity", "Error reading file", e);
        }
    }
}
