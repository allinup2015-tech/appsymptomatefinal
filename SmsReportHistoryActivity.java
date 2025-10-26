package com.example.answer;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SmsReportHistoryActivity extends AppCompatActivity {

    private static final String TAG = "SmsReportHistoryActivity";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_report_history);

        String reportText = getIntent().getStringExtra("report_text");
        String recognizedText = getIntent().getStringExtra("recognized_text");
        String chatGPTResponse = getIntent().getStringExtra("chatgpt_response");
        int sessionDuration = getIntent().getIntExtra("session_duration", 0);
        boolean isUserLoggedIn = getIntent().getBooleanExtra("user_logged_in", false);
        String userEmail = getIntent().getStringExtra("user_email");

        saveSmsReportHistory(reportText, recognizedText, chatGPTResponse, sessionDuration, isUserLoggedIn, userEmail);
    }

    private void saveSmsReportHistory(String reportText, String recognizedText, String chatGPTResponse, int sessionDuration, boolean isUserLoggedIn, String userEmail) {
        if (!isUserLoggedIn || userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "유저 정보가 없어 저장 실패.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String encodedEmail = userEmail.replace(".", ",");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("recordingsHistory").child(encodedEmail).push();

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("smsReport", reportText);
        historyData.put("recognizedText", recognizedText);
        historyData.put("chatGPTResponse", chatGPTResponse);
        historyData.put("sessionDuration", sessionDuration);
        historyData.put("timestamp", System.currentTimeMillis());
        historyData.put("reportedTo911", true);

        historyRef.setValue(historyData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "신고내역 저장 성공.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "신고내역 저장 실패.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}
