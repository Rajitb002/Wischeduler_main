package com.example.wischeduler;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenAIApi {
    @Headers("Authorization: ") // Replace YOUR_API_KEY with your OpenAI key
    @POST("chat/completions")
    Call<ChatResponse> getChatCompletion(@Body ChatRequest request);
}
