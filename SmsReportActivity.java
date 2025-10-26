package com.example.answer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SmsReportActivity extends AppCompatActivity {

    private static final String TAG = "SmsReportActivity";
    private static final int SMS_PERMISSION_CODE = 1001;

    
    private TextView reportTextView;
    private Button backButton, sendSmsButton, previewButton;

 
    private String reportText;
    private String recognizedText;
    private String chatGPTResponse;
    private int sessionDuration;
    private boolean isUserLoggedIn;
    private String userEmail;

    // Firebase
    private DatabaseReference userRecordingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_report);

        initViews();
        loadDataFromIntent();
        setupFirebase();
        setupClickListeners();
    }

    private void initViews() {
        reportTextView = findViewById(R.id.report_message_text);
        backButton = findViewById(R.id.back_to_recording_btn);
        sendSmsButton = findViewById(R.id.send_to_911_btn);
        previewButton = findViewById(R.id.preview_report_btn);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Emergency SMS Report");
        }
    }

    private void loadDataFromIntent() {
        recognizedText = getIntent().getStringExtra("recognized_text");
        chatGPTResponse = getIntent().getStringExtra("chatgpt_response");
        sessionDuration = getIntent().getIntExtra("session_duration", 0);
        isUserLoggedIn = getIntent().getBooleanExtra("user_logged_in", false);
        userEmail = getIntent().getStringExtra("user_email");

        reportText = formatCompleteReport();
        reportTextView.setText(generateDisplaySummary());
    }

    private void setupFirebase() {
        if (isUserLoggedIn && userEmail != null && !userEmail.isEmpty()) {
            String encodedEmail = userEmail.replace(".", ",");
            userRecordingsRef = FirebaseDatabase.getInstance().getReference("recordings").child(encodedEmail);
        } else {
            Log.w(TAG, "User not logged in or email is missing. DB features will be disabled.");
        }
    }

    private String generateDisplaySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Emergency SMS Report Preview\n\n");

        int minutes = sessionDuration / 60;
        int seconds = sessionDuration % 60;
        summary.append("Session Duration: ").append(String.format("%02d:%02d", minutes, seconds)).append("\n");
        summary.append("Date: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n\n");

        if (recognizedText != null && !recognizedText.isEmpty()) {
            summary.append("Reported Symptoms:\n");
            String shortSymptoms = recognizedText.length() > 200 ? recognizedText.substring(0, 200) + "..." : recognizedText;
            summary.append(shortSymptoms).append("\n\n");
        }

        summary.append("Tap 'Preview Full Report' to see the complete SMS message.\n");
        summary.append("Tap 'Send Emergency SMS' to proceed.");

        return summary.toString();
    }

    private String formatCompleteReport() {
        StringBuilder report = new StringBuilder();
        report.append("EMERGENCY MEDICAL ALERT\n\n");
        report.append("Time: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n\n");

        if (recognizedText != null && !recognizedText.isEmpty()) {
            report.append("SYMPTOMS:\n").append(recognizedText).append("\n\n");
        }

        if (chatGPTResponse != null && !chatGPTResponse.isEmpty()) {
            report.append("AI ANALYSIS:\n").append(chatGPTResponse).append("\n\n");
        }

        report.append("Sent via Symptomate AI Assistant");
        return report.toString();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        if (previewButton != null) {
            previewButton.setOnClickListener(v -> showFullReportPreview());
        }

        sendSmsButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestSMSPermission();
            } else {
                showSMSConfirmationDialog();
            }
        });
    }

    private void showFullReportPreview() {
        new AlertDialog.Builder(this)
                .setTitle("Complete SMS Report")
                .setMessage(reportText)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSMSConfirmationDialog() {
        String message = "Warning:\n" +
                "You are about to send an emergency medical report via SMS.\n\n" +
                "For real emergencies, call 119 immediately!\n\n" +
                "This feature is intended as a supplementary tool. Do you want to continue?";

        new AlertDialog.Builder(this)
                .setTitle("Confirm Emergency SMS")
                .setMessage(message)
                .setPositiveButton("Send SMS", (dialog, which) -> sendSms(reportText))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void requestSMSPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    private void sendSms(String text) {
        try {
            // ⚠️ For testing, replace "911" with a phone number you own.
            // Using "911" in a real app without proper integration is dangerous.
            String phoneNumber = "911";

            SmsManager smsManager = SmsManager.getDefault();

            if (text.length() > 160) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, smsManager.divideMessage(text), null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, text, null, null);
            }

           
            updateReportStatusInDB();

       
            Toast.makeText(this, "Emergency report sent successfully!", Toast.LENGTH_LONG).show();

            new AlertDialog.Builder(this)
                    .setTitle("SMS Sent Successfully")
                    .setMessage("Your emergency report has been sent.\n\nIf this is a serious emergency, please call 119 immediately!")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "SMS sending failed", e);
            Toast.makeText(this, "SMS sending failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateReportStatusInDB() {
        if (userRecordingsRef == null) {
            Log.w(TAG, "Cannot update DB: User not logged in or DB reference is null.");
            return;
        }

        Query lastRecordingQuery = userRecordingsRef.orderByChild("timestamp").limitToLast(1);

        lastRecordingQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot lastRecordingSnapshot = dataSnapshot.getChildren().iterator().next();
                    String lastRecordingId = lastRecordingSnapshot.getKey();

                    if (lastRecordingId != null) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("reportedTo911", true);
                        updates.put("reportTimestamp", System.currentTimeMillis());

                        userRecordingsRef.child(lastRecordingId).updateChildren(updates)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully updated report status in DB for ID: " + lastRecordingId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update report status in DB.", e));
                    }
                } else {
                    Log.w(TAG, "No recordings found for user to update.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "DB query cancelled for updating report status.", databaseError.toException());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSMSConfirmationDialog();
            } else {
                Toast.makeText(this, "SMS permission is required to send emergency reports.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
