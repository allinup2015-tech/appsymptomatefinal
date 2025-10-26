
package com.example.answer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ChatGPT {

    private static final String TAG = "ChatGPT_API";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");


    private static final String MY_SECRET_KEY = "MySecertAPIKEy";

    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;

 
    public interface ChatGPTCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public ChatGPT(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

   
    public void callAPI(String question, ChatGPTCallback callback) {
        Log.d(TAG, "Calling ChatGPT API with question: " + question);

        if (callback == null) {
            Log.e(TAG, "Callback is null");
            return;
        }

        if (question == null || question.trim().isEmpty()) {
            Log.e(TAG, "Question is null or empty");
            mainHandler.post(() -> callback.onError("Input text is empty"));
            return;
        }

        
        if (MY_SECRET_KEY == null || MY_SECRET_KEY.equals("YOUR_OPENAI_API_KEY") || MY_SECRET_KEY.isEmpty()) {
            Log.e(TAG, "API Key is missing.");
            mainHandler.post(() -> callback.onError("API key is not set. Please check your configuration."));
            return;
        }

        String systemPrompt = "You are a medical symptom analysis AI assistant. Analyze the patient's symptoms and provide a structured assessment.\n\n" +
                "📋 ANALYSIS FORMAT (Respond in English):\n\n" +
                "👤 PATIENT INFORMATION\n" +
                "- Name: [If provided, otherwise 'Not specified']\n" +
                "- Age: [If provided, otherwise 'Not specified']\n" +
                "- Gender: [If provided, otherwise 'Not specified']\n\n" +
                "🔍 SYMPTOM SUMMARY\n" +
                "- Primary Symptoms: [Main symptoms described]\n" +
                "- Duration: [How long symptoms have persisted, if mentioned]\n" +
                "- Severity: [Mild/Moderate/Severe, if determinable]\n\n" +
                "🏥 MEDICAL ASSESSMENT\n" +
                "- Possible Conditions: [List 2-3 most likely conditions based on symptoms]\n" +
                "- Pre-existing Conditions: [Any mentioned conditions, otherwise 'None mentioned']\n" +
                "- Risk Level: [Low/Medium/High - based on symptom severity]\n\n" +
                "🚨 RECOMMENDED ACTION\n" +
                "- Urgency: [Immediate/Soon/Routine medical attention needed]\n" +
                "- Suggested Specialists: [Relevant medical specialists to consult]\n\n" +
                "🏥 NEARBY MEDICAL FACILITIES\n" +
                "[List 3-5 appropriate hospitals or clinics based on location and symptoms]\n\n" +
                "⚠️ IMPORTANT DISCLAIMER\n" +
                "This analysis is for informational purposes only. A consultation with a qualified medical professional is necessary for an accurate diagnosis and treatment plan.";

        try {
            // JSON 요청 구성
            JSONArray messages = new JSONArray();

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", "Please analyze the following symptoms: " + question.trim());

            messages.put(systemMsg);
            messages.put(userMsg);

            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("model", "gpt-4o");
            requestBodyJson.put("messages", messages);
            requestBodyJson.put("max_tokens", 500);
            requestBodyJson.put("temperature", 0.7);

            RequestBody body = RequestBody.create(requestBodyJson.toString(), JSON);
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + MY_SECRET_KEY)
                    .post(body)
                    .build();

    
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API call failed", e);
                    if (mainHandler != null && callback != null) {
                        mainHandler.post(() -> callback.onError("Network error occurred: " + e.getMessage()));
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (Response r = response) {
                        String responseBodyString = r.body() != null ? r.body().string() : "";
                        Log.d(TAG, "API Response Code: " + r.code());

                        if (r.isSuccessful() && !responseBodyString.isEmpty()) {
                            try {
                                JSONObject jsonObject = new JSONObject(responseBodyString);
                                JSONArray choices = jsonObject.getJSONArray("choices");
                                if (choices.length() > 0) {
                                    String result = choices.getJSONObject(0).getJSONObject("message").getString("content");

                                    if (mainHandler != null && callback != null) {
                                        mainHandler.post(() -> {
                                            callback.onSuccess(result.trim());
                                            Log.d(TAG, "Successfully received and processed API response.");
                                        });
                                    }
                                } else {
                                    if (mainHandler != null && callback != null) {
                                        mainHandler.post(() -> callback.onError("Empty response from ChatGPT API"));
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error", e);
                                if (mainHandler != null && callback != null) {
                                    mainHandler.post(() -> callback.onError("Invalid response format from API"));
                                }
                            }
                        } else {
                            Log.e(TAG, "API Error Response: " + responseBodyString);
                            String errorMessage = handleHttpError(r.code(), responseBodyString);
                            if (mainHandler != null && callback != null) {
                                mainHandler.post(() -> callback.onError(errorMessage));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        if (mainHandler != null && callback != null) {
                            mainHandler.post(() -> callback.onError("An error occurred while processing the response."));
                        }
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON request", e);
            mainHandler.post(() -> callback.onError("Error creating request data."));
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            mainHandler.post(() -> callback.onError("Unexpected error occurred: " + e.getMessage()));
        }
    }

 
    private String handleHttpError(int responseCode, String responseBody) {
        String errorMessage;
        try {
            if (responseBody != null && !responseBody.isEmpty()) {
                JSONObject errorJson = new JSONObject(responseBody).getJSONObject("error");
                errorMessage = errorJson.getString("message");
            } else {
                errorMessage = "Server error (Code: " + responseCode + ")";
            }
        } catch (JSONException e) {
            errorMessage = "Server error (Code: " + responseCode + ")";
        }
        return "Error: " + errorMessage;
    }


    public void cleanup() {
        new Thread(() -> {
            try {
                if (client != null) {
                    client.dispatcher().executorService().shutdown();
                    client.connectionPool().evictAll();
                }
                Log.d(TAG, "ChatGPT resources cleaned up in background.");
            } catch (Exception e) {
                Log.w(TAG, "Error during cleanup: " + e.getMessage());
            }
        }).start();
    }
}
