package com.example.wischeduler;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private EditText chatInput;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot2);

        // Initialize UI elements
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInput = findViewById(R.id.chatInput);
        sendButton = findViewById(R.id.sendButton);

        // Initialize message list and adapter
        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);

        // Set up RecyclerView
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        // Add an opening message from Wizzy
        addBotMessage("Hello! I'm Wizzy, your friendly scheduling assistant. How can I help you today?");

        // Set up send button click listener
        sendButton.setOnClickListener(v -> {
            String userMessage = chatInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                addUserMessage(userMessage);
                sendMessageToChatbot(userMessage);
                chatInput.setText(""); // Clear the input field
            }
        });

        ImageButton backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            finish(); // Close the ChatbotActivity and return to the previous activity (MainMenu)
        });
    }

    private void addUserMessage(String message) {
        String timestamp = getCurrentTimestamp();
        messages.add(new ChatMessage(message, false, timestamp)); // Add user message
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1); // Scroll to the latest message
    }

    private void addBotMessage(String message) {
        String timestamp = getCurrentTimestamp();
        messages.add(new ChatMessage(message, true, timestamp)); // Add bot message
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1); // Scroll to the latest message
    }

    private void sendMessageToChatbot(String userMessage) {
        OpenAIApi openAIApi = OpenAIClient.getClient().create(OpenAIApi.class);

        // Prepare the API request
        List<ChatRequest.Message> apiMessages = new ArrayList<>();
        apiMessages.add(new ChatRequest.Message("system", "You are Wizzy, a friendly scheduling assistant."));
        apiMessages.add(new ChatRequest.Message("user", userMessage));

        ChatRequest request = new ChatRequest("gpt-4o-mini", apiMessages);

        openAIApi.getChatCompletion(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Extract the bot's response
                    String botResponse = response.body().choices.get(0).message.content;
                    runOnUiThread(() -> addBotMessage(botResponse));
                } else {
                    runOnUiThread(() -> addBotMessage("Oops! I couldn't process that. Please try again."));
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                runOnUiThread(() -> addBotMessage("Sorry, I'm having trouble connecting. Check your network and try again."));
            }
        });
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }
}
