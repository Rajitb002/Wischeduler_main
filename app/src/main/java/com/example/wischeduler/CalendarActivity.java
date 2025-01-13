package com.example.wischeduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CalendarActivity extends AppCompatActivity {

    private static final String TAG = "CalendarActivity";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};
    private static final String PREFS_NAME = "WiSchedulerPrefs";
    private static final String KEY_SELECTED_EMAIL = "selected_email";

    //private GoogleAccountCredential credential;
    private Calendar calendarService;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    //private boolean isFromMainMenu = false; // No longer in use. Was part of old implementation of email selection.
    private boolean isFromChangeEmail = false;
    public static GoogleAccountCredential credential;

    private boolean isScheduleView = false; // Tracks the current view mode (schedule or week)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this); // Initialize Firebase
        setContentView(R.layout.activity_calendar);

        WebView webView = findViewById(R.id.calendarWebView);



        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:document.body.style.margin=\"0\"; void(0);");
            }
        });

        // Initialize Google Account Credential
        credential = GoogleAccountCredential.usingOAuth2(
                        this, Collections.singletonList(CalendarScopes.CALENDAR))
                .setBackOff(new ExponentialBackOff());

        // Retrieve the last selected email from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedAccount = prefs.getString(KEY_SELECTED_EMAIL, null);

        if (selectedAccount != null) {
            // Load the calendar for the saved account
            credential.setSelectedAccountName(selectedAccount);
            initializeCalendarService();
            loadCalendarForAccount(selectedAccount);
        } else {
            // No saved account, prompt the user to select one
            changeEmail();
        }

        // Set up button listeners
        Button addEventButton = findViewById(R.id.btnAddEvent);
        addEventButton.setOnClickListener(v -> processEventsFromJson());

        ImageView changeEmailButton = findViewById(R.id.btnProfile);
        changeEmailButton.setOnClickListener(v -> changeEmail());

        readPreferencesJson(this);
        storePreferencesToFirebase(selectedAccount, readPreferencesJson(this));
        getPreferencesFromFirebase(selectedAccount);
    }

    public List<JSONObject> readPreferencesJson(Context context) {
        List<JSONObject> questionsAndAnswers = new ArrayList<>();
        try {
            InputStream inputStream = context.getAssets().open("preferences.json");
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(json);
            JSONArray qnaArray = jsonObject.getJSONArray("questions_and_answers");

            for (int i = 0; i < qnaArray.length(); i++) {
                questionsAndAnswers.add(qnaArray.getJSONObject(i));
            }
        } catch (Exception e) {
            Log.e("JSON", "Error reading preferences.json", e);
        }
        Log.d("JSON", "Questions and Answers: " + questionsAndAnswers);
        return questionsAndAnswers;
    }

    void storePreferencesToFirebase(String accountName, List<JSONObject> questionsAndAnswers) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users").child(accountName.replace(".","_"));

        List<Map<String, String>> firebaseData = new ArrayList<>();
        for (JSONObject qna : questionsAndAnswers) {
            try {
                Map<String, String> map = new HashMap<>();
                map.put("question", qna.getString("question"));
                map.put("answer", qna.getString("answer"));
                firebaseData.add(map);
            } catch (Exception e) {
                Log.e("Firebase", "Error processing QnA", e);
            }
        }

        databaseRef.child("preferences").setValue(firebaseData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebase", "Preferences saved successfully!");
                    } else {
                        Log.e("Firebase", "Failed to save preferences", task.getException());
                    }
                });
    }

    void getPreferencesFromFirebase(String accountName) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users").child(accountName.replace(".","_")).child("preferences");
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Map<String, String>> questionsAndAnswers = new ArrayList<>();
                    for (DataSnapshot qnaSnapshot : snapshot.getChildren()) {
                        String question = qnaSnapshot.child("question").getValue(String.class);
                        String answer = qnaSnapshot.child("answer").getValue(String.class);
                        Map<String, String> qna = new HashMap<>();
                        qna.put("question", question);
                        qna.put("answer", answer);
                        questionsAndAnswers.add(qna);
                    }
                    Log.d("Firebase", "Retrieved preferences: " + questionsAndAnswers);
                } else {
                    Log.d("Firebase", "No preferences found for user: " + accountName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error retrieving preferences", error.toException());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) { // Handle account selection
            if (resultCode == RESULT_OK && data != null) {
                String accountName = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    // Save the selected account to SharedPreferences
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(KEY_SELECTED_EMAIL, accountName);
                    editor.apply();

                    // Update the credential and load the calendar
                    credential.setSelectedAccountName(accountName);
                    initializeCalendarService();
                    loadCalendarForAccount(accountName);
                }
            } else {
                // Handle cancel option only for "Change Email"
                if (isFromChangeEmail) {
                    Log.e(TAG, "User canceled email change. Returning to current calendar view.");
                }
            }
        } else if (requestCode == 1002) { // Handle authorization request
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Authorization successful, retrying operation...");
                processEventsFromJson();
            } else {
                Log.e(TAG, "Authorization denied.");
            }
        }
    }

    //Outdated code. No longer asking for email to view at startup all the time.
    /*private void requestAccountAndAuthorize(boolean isFromMainMenu) {
        this.isFromMainMenu = isFromMainMenu; // Track the context
        startActivityForResult(credential.newChooseAccountIntent(), 1001);
    }*/


    //Just added TimeZone check. Need to test it to make sure its working as intended.
    private void loadCalendarForAccount(String accountName) {
        if (accountName != null) {
            String calendarUrl = "https://calendar.google.com/calendar/embed?" +
                    "src=" + accountName +
                    "&ctz=" + TimeZone.getDefault().getID() +
                    "&mode=WEEK";

            WebView webView = findViewById(R.id.calendarWebView);
            webView.loadUrl(calendarUrl);
            Log.d(TAG, "Calendar loaded for account: " + accountName);
        } else {
            Log.e(TAG, "Account name is null. Unable to load calendar.");
        }
    }

    private void initializeCalendarService() {
        calendarService = new Calendar.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                new com.google.api.client.json.gson.GsonFactory(),
                credential)
                .setApplicationName("Wischeduler")
                .build();
    }


//    private void processEventsFromJson() {
//        try {
//            InputStream inputStream = getAssets().open("tasks.json");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//            StringBuilder stringBuilder = new StringBuilder();
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line);
//            }
//
//            JSONArray eventsArray = new JSONArray(stringBuilder.toString());
//            Log.d(TAG, "Total events to add: " + eventsArray.length());
//
//            for (int i = 0; i < eventsArray.length(); i++) {
//                JSONObject eventObject = eventsArray.getJSONObject(i);
//
//                String task = eventObject.getString("task");
//                String date = eventObject.getString("date");
//                String start = eventObject.getString("start");
//                String end = eventObject.getString("end");
//
//                executor.submit(() -> addEventToCalendar(task, date, start, end));
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error reading or parsing tasks.json", e);
//        }
//    }

    private void addEventToCalendar(String task, String startDateTime, String endDateTime, String description) {
        try {
            // Sanitize date-time strings to RFC3339 format
            startDateTime = sanitizeDateTime(startDateTime);
            endDateTime = sanitizeDateTime(endDateTime);

            Event event = new Event()
                    .setSummary(task)
                    .setDescription(description)
                    .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startDateTime)))
                    .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endDateTime)));

            // Insert the event into the user's primary calendar
            calendarService.events().insert("primary", event).execute();
            Log.d(TAG, "Event successfully added: " + task);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid event data: " + task + ", startDateTime: " + startDateTime + ", endDateTime: " + endDateTime, e);
        } catch (UserRecoverableAuthIOException e) {
            Log.e(TAG, "Authorization required for event: " + task, e);
            startActivityForResult(e.getIntent(), 1002);
        } catch (Exception e) {
            Log.e(TAG, "Error creating or inserting event: " + task, e);
        }
    }

    private String sanitizeDateTime(String dateTime) {
        // Remove square brackets and their contents (e.g. [America/Chicago])
        dateTime = dateTime.replaceAll("\\[.*?\\]", "").trim();

        // At this point, a typical input might look like "2024-12-09T06:30-06:00"
        // Check if we have seconds. If not, we add ":00" before the timezone or Z.
        // Let's split the time portion from the date.
        // Expected final format: YYYY-MM-DDTHH:MM:SS±HH:MM or YYYY-MM-DDTHH:MM:SSZ

        // First, check if we already have a timezone offset or a 'Z':
        // Possible endings: Z or ±HH:MM (e.g., -06:00)
        String timeRegex = "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2})(:?(\\d{2}))?(Z|[+-]\\d{2}:\\d{2})?$";
        // Explanation:
        // ^\d{4}-\d{2}-\d{2}T matches YYYY-MM-DDT
        // \d{2}:\\d{2} matches HH:MM
        // (:?(\\d{2}))? optionally matches :SS (seconds)
        // (Z|[+-]\\d{2}:\\d{2})? optionally matches Z or an offset

        java.util.regex.Pattern p = java.util.regex.Pattern.compile(timeRegex);
        java.util.regex.Matcher m = p.matcher(dateTime);
        if (!m.matches()) {
            // If it doesn't match, we try to fix it by ensuring seconds and offset.
            // Let's do a more manual approach:

            // Separate the date from the time zone
            int tIndex = dateTime.indexOf('T');
            if (tIndex == -1) {
                throw new IllegalArgumentException("Invalid date-time format (no T found): " + dateTime);
            }

            String datePart = dateTime.substring(0, tIndex); // YYYY-MM-DD
            String timePart = dateTime.substring(tIndex + 1); // e.g. 06:30-06:00 or 06:30Z or just 06:30

            // Check if there's an offset or 'Z'
            String offset = "";
            if (timePart.contains("Z")) {
                offset = "Z";
                timePart = timePart.replace("Z", "");
            } else {
                // Try to find an offset of form ±HH:MM
                int plusIndex = timePart.indexOf('+');
                int minusIndex = timePart.indexOf('-');
                int offsetIndex = plusIndex == -1 ? minusIndex : plusIndex;
                if (offsetIndex > 0) {
                    offset = timePart.substring(offsetIndex);
                    timePart = timePart.substring(0, offsetIndex);
                }
            }

            // Now timePart should be just HH:MM or HH:MM:SS
            // If it has no seconds, add ':00'
            if (timePart.matches("\\d{2}:\\d{2}")) {
                timePart += ":00";
            }

            // If no offset and no Z, default to Z
            if (offset.isEmpty()) {
                offset = "Z";
            }

            dateTime = datePart + "T" + timePart + offset;
        } else {
            // It matched the initial pattern, but we must ensure seconds are present.
            // If group(3) is null, it means no seconds were provided.
            if (m.group(3) == null) {
                // Insert ":00" before the offset/Z
                // We'll reconstruct from the groups
                String dateAndTime = m.group(1); // YYYY-MM-DDTHH:MM
                String offsetOrZ = (m.group(4) == null) ? "Z" : m.group(4);
                dateTime = dateAndTime + ":00" + offsetOrZ;
            }
        }

        // Validate final format against a strict RFC3339 pattern:
        // YYYY-MM-DDTHH:MM:SSZ or YYYY-MM-DDTHH:MM:SS±HH:MM
        if (!dateTime.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})$")) {
            throw new IllegalArgumentException("Invalid date-time format after sanitization: " + dateTime);
        }

        return dateTime;
    }

    private void processEventsFromJson() {
        try {
            InputStream quizInputStream = getAssets().open("quiz.json");
            InputStream preferencesInputStream = getAssets().open("preferences.json");
            InputStream socialInputStream = getAssets().open("social.json");
            InputStream eventsInputStream = getAssets().open("events.json");

            String quizJsonString = readStreamToString(quizInputStream);
            String preferencesJsonString = readStreamToString(preferencesInputStream);
            String socialJsonString = readStreamToString(socialInputStream);
            String eventsJsonString = readStreamToString(eventsInputStream);

            Log.d(TAG, "Preferences JSON: " + preferencesJsonString);
            Log.d(TAG, "Quiz JSON: " + quizJsonString);
            Log.d(TAG, "Events JSON: " + eventsJsonString);
            Log.d(TAG, "Social JSON: " + socialJsonString);

            smartScheduler scheduler = new smartScheduler();
            String result = scheduler.generateSchedule(preferencesJsonString, quizJsonString, eventsJsonString, socialJsonString);

            Log.d(TAG, "Generated schedule JSON: " + result);

            JSONArray eventsArray;
            try {
                JSONObject resultJson = new JSONObject(result);
                if (resultJson.has("schedule")) {
                    eventsArray = resultJson.getJSONArray("schedule");
                } else {
                    Log.e(TAG, "Missing 'schedule' key. Using an empty array as a fallback.");
                    eventsArray = new JSONArray();
                }
            } catch (JSONException ex) {
                Log.e(TAG, "Malformed JSON. Using an empty array as a fallback.", ex);
                eventsArray = new JSONArray();
            }

            Log.d(TAG, "Total events to add: " + eventsArray.length());

            for (int i = 0; i < eventsArray.length(); i++) {
                try {
                    JSONObject eventObject = eventsArray.getJSONObject(i);

                    String task = eventObject.getString("title");
                    String start = eventObject.getString("startTime");
                    String end = eventObject.getString("endTime");
                    String description = eventObject.optString("description", "");

                    executor.submit(() -> addEventToCalendar(task, start, end, description));
                } catch (JSONException e) {
                    Log.e(TAG, "Error processing individual event", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing events from JSON", e);
        }
    }



    private String readStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        inputStream.close();
        return stringBuilder.toString();
    }


//    private void addEventToCalendar(String task, String date, String startTime, String endTime) {
//        try {
//            String startDateTime = date + "T" + startTime + ":00";
//            String endDateTime = date + "T" + endTime + ":00";
//
//            Event event = new Event()
//                    .setSummary(task)
//                    .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startDateTime)))
//                    .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endDateTime)));
//
//            calendarService.events().insert("primary", event).execute();
//            Log.d(TAG, "Event successfully added: " + task);
//        } catch (UserRecoverableAuthIOException e) {
//            Log.e(TAG, "Authorization required for event: " + task, e);
//            startActivityForResult(e.getIntent(), 1002);
//        } catch (Exception e) {
//            Log.e(TAG, "Error creating or inserting event: " + task, e);
//        }
//    }


    public void openMainMenu(View view) {
        // Navigate back to the main menu
        startActivity(new Intent(CalendarActivity.this, MenuActivity.class));
    }

    //Read from a json file add to firebase and add functionality to retrieve data from firebase.


    public void openToDoList(View view) {
        WebView webView = findViewById(R.id.calendarWebView);
        Button toDoListButton = findViewById(R.id.btnToDoList);

        String accountName = credential.getSelectedAccountName();
        if (accountName != null) {
            // Determine the URL and button text based on the current view mode
            String calendarUrl;
            if (isScheduleView) {
                // Switch to week view
                calendarUrl = "https://calendar.google.com/calendar/embed?" +
                        "src=" + accountName +
                        "&ctz=" + TimeZone.getDefault().getID() +
                        "&mode=WEEK";
                toDoListButton.setText("To-Do List"); // Update button text
            } else {
                // Switch to schedule view
                calendarUrl = "https://calendar.google.com/calendar/embed?" +
                        "src=" + accountName +
                        "&ctz=" + TimeZone.getDefault().getID() +
                        "&mode=AGENDA";
                toDoListButton.setText("Week View"); // Update button text
            }

            // Load the URL in the WebView
            webView.loadUrl(calendarUrl);
            Log.d(TAG, (isScheduleView ? "Week View" : "Schedule View") + " loaded for account: " + accountName);

            // Toggle the view mode
            isScheduleView = !isScheduleView;
        } else {
            Log.e(TAG, "Account name is null. Unable to toggle views.");
        }
    }

    private void loadScheduleView(String date) {
        String accountName = credential.getSelectedAccountName();
        if (accountName != null) {
            // Generate the URL for Google Calendar's schedule view
            String scheduleUrl = "https://calendar.google.com/calendar/embed?" +
                    "src=" + accountName +
                    "&ctz=" + TimeZone.getDefault().getID() +
                    "&mode=AGENDA";

            WebView webView = findViewById(R.id.calendarWebView);
            webView.loadUrl(scheduleUrl);
            Log.d(TAG, "Schedule view loaded for account: " + accountName + ", date: " + date);
        } else {
            Log.e(TAG, "Account name is null. Unable to load schedule view.");
        }
    }


    private void changeEmail() {
        Log.d(TAG, "Prompting user to change email account...");
        isFromChangeEmail = true; // Mark that the request is from the "Change Email" button
        startActivityForResult(credential.newChooseAccountIntent(), 1001);
    }

}