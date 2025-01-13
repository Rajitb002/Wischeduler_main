package com.example.wischeduler;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PreferencesActivity extends AppCompatActivity {

    private static final String TAG = "PreferencesActivity";
    private static final String PREFS_NAME = "WiSchedulerPrefs";
    private static final String KEY_SELECTED_EMAIL = "selected_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Retrieve user email from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userEmail = prefs.getString(KEY_SELECTED_EMAIL, null);

        if (userEmail == null) {
            Toast.makeText(this, "No Google account found. Please select an account.", Toast.LENGTH_SHORT).show();
            finish(); // Exit the activity if no email is found
            return;
        }

        // Initialize UI Components
        RadioGroup studyTimeGroup = findViewById(R.id.radioGroupStudyTime);
        RadioGroup sittingDurationGroup = findViewById(R.id.radioGroupSittingDuration);
        RadioGroup examPrepGroup = findViewById(R.id.radioGroupExamPrep);
        RadioGroup eventPreferencesGroup = findViewById(R.id.radioGroupEventPreferences);
        EditText sleepTimeEditText = findViewById(R.id.editTextSleepTime);
        EditText wakeUpTimeEditText = findViewById(R.id.editTextWakeUpTime);

        Button submitButton = findViewById(R.id.btnSubmit);

        submitButton.setOnClickListener(v -> {
            try {
                // Collect preferences into JSON
                JSONObject preferencesJson = new JSONObject();

                preferencesJson.put("preferredStudyTime", getSelectedRadioText(studyTimeGroup));
                preferencesJson.put("sittingDuration", getSelectedRadioText(sittingDurationGroup));
                preferencesJson.put("examPreparation", getSelectedRadioText(examPrepGroup));
                preferencesJson.put("eventPreferences", getSelectedRadioText(eventPreferencesGroup));
                preferencesJson.put("sleepTime", sleepTimeEditText.getText().toString().trim());
                preferencesJson.put("wakeUpTime", wakeUpTimeEditText.getText().toString().trim());

                // Convert JSON to List<Map<String, String>> for Firebase
                List<Map<String, String>> firebaseData = new ArrayList<>();
                Iterator<String> keys = preferencesJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Map<String, String> map = new HashMap<>();
                    map.put("question", key);
                    map.put("answer", preferencesJson.getString(key));
                    firebaseData.add(map);
                }

                // Store preferences in Firebase
                storePreferencesToFirebase(userEmail, firebaseData);

                Toast.makeText(this, "Preferences submitted successfully!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error creating JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch and print preferences from Firebase
        getPreferencesFromFirebase(userEmail);
    }

    // Helper method to get selected RadioButton text
    private String getSelectedRadioText(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton radioButton = findViewById(selectedId);
            return radioButton.getText().toString();
        }
        return "Not specified";
    }

    // Store preferences in Firebase
    void storePreferencesToFirebase(String userEmail, List<Map<String, String>> firebaseData) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userEmail.replace(".", "_"));

        databaseRef.child("preferences").setValue(firebaseData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Preferences saved successfully!");
                    } else {
                        Log.e(TAG, "Failed to save preferences", task.getException());
                    }
                });
    }

    // Fetch preferences from Firebase and print them
    void getPreferencesFromFirebase(String userEmail) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userEmail.replace(".", "_"))
                .child("preferences");
        Log.d(TAG, "Accessing Firebase path: users/" + userEmail.replace(".", "_") + "/preferences");

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Map<String, String>> preferences = new ArrayList<>();
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        String question = childSnapshot.child("question").getValue(String.class);
                        String answer = childSnapshot.child("answer").getValue(String.class);

                        if (question != null && answer != null) {
                            Map<String, String> qna = new HashMap<>();
                            qna.put("question", question);
                            qna.put("answer", answer);
                            preferences.add(qna);
                        }
                    }

                    // Log retrieved preferences
                    for (Map<String, String> qna : preferences) {
                        Log.d(TAG, "Question: " + qna.get("question") +
                                ", Answer: " + qna.get("answer"));
                    }

                    Toast.makeText(PreferencesActivity.this, "Preferences retrieved successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "No preferences found for the user.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error retrieving preferences", error.toException());
            }
        });
    }
}
