package com.example.answer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;


/**
 * Symptomate Main Recording Activity - ChatGPT.callAPI() 메서드 사용 버전
 * NavigationDrawer + 실제 녹음 + ChatGPT + Firebase 모든 기능 포함
 */
public class Main_Page extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "SYMPTOMATE_MAIN";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String API_KEY = "AIzaSyBcBfDPvC91CC190h5P82nNNCY0UUurllE";
    private static final String API_URL = "https://speech.googleapis.com/v1/speech:recognize?key=" + API_KEY;

    /* Audio Settings */
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_MULTIPLIER = 4;
    private static final int MAX_RECORDING_TIME_MS = 60_000;

    /* NavigationDrawer */
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    /* Audio Objects */
    private AudioRecord audioRecord;
    private volatile boolean isRecording = false;
    private Thread recordingThread;
    private ByteArrayOutputStream audioBuffer;
    private int bufferSize;

    /* UI Components */
    private TextView sttResultTextView;
    private TextView recordingStatusPrimary;
    private TextView recordingStatusSecondary;
    private TextView sessionTimer;
    private TextView connectionStatus;
    private ImageView microphoneIcon;
    private ImageView microphoneShadow;
    private Button startBtn;
    private Button stopBtn;
    private View[] waveBars;
    private TextView userStatusIndicator;

    /* Background Task Management */
    private ExecutorService executorService;
    private Handler mainHandler;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private int secondsElapsed = 0;

    /* Recording State */
    private String recognizedText = "";
    private String chatGPTResponse = "";
    private ChatGPT chatGPTHelper;

    /* User State */
    private boolean isUserLoggedIn = false;
    private String userEmail = "";
    private String userName = "";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== Main_page onCreate START ===");

        try {
            setContentView(R.layout.activity_mainpage1);
            Log.d(TAG, "Layout set successfully");

            // Firebase 초기화
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            // 백그라운드 작업 초기화
            executorService = Executors.newSingleThreadExecutor();
            mainHandler = new Handler(Looper.getMainLooper());
            timerHandler = new Handler(Looper.getMainLooper());
            audioBuffer = new ByteArrayOutputStream();

            // ✅ ChatGPT 헬퍼 초기화 (수정된 부분)
            chatGPTHelper = new ChatGPT(this);
            Log.d(TAG, "ChatGPT helper initialized");

            // 사용자 상태 로드
            loadUserState();

            // NavigationDrawer 초기화 (안전하게)
            initNavigationDrawer();

            // 권한 확인
            if (!checkAndRequestPermissions()) {
                Log.w(TAG, "Permissions not granted yet");
                return;
            }

            // UI 및 기능 초기화
            initializeApp();

            Log.d(TAG, "=== Main_page onCreate SUCCESS ===");

        } catch (Exception e) {
            Log.e(TAG, "FATAL ERROR in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "App initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadUserState() {
        try {
            Intent intent = getIntent();
            isUserLoggedIn = intent.getBooleanExtra("user_logged_in", false);
            userEmail = intent.getStringExtra("user_email");
            userName = intent.getStringExtra("user_name");

            if (userEmail == null) userEmail = "";
            if (userName == null) userName = "";

            // Firebase 자동 로그인 확인
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && !isUserLoggedIn) {
                isUserLoggedIn = true;
                userEmail = currentUser.getEmail();
                userName = currentUser.getDisplayName();
                if (userEmail == null) userEmail = "";
                if (userName == null || userName.isEmpty()) {
                    userName = userEmail.isEmpty() ? "User" : userEmail.split("@")[0];
                }
            }

            Log.d(TAG, "User state loaded - Logged in: " + isUserLoggedIn + ", Email: " + userEmail + ", Name: " + userName);
        } catch (Exception e) {
            Log.e(TAG, "Error loading user state: " + e.getMessage(), e);
            isUserLoggedIn = false;
            userEmail = "";
            userName = "";
        }
    }

    private void initNavigationDrawer() {
        try {
            // NavigationDrawer 컴포넌트 찾기 (옵션)
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            toolbar = findViewById(R.id.toolbar);

            if (drawerLayout != null && navigationView != null && toolbar != null) {
                // 툴바 설정
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeButtonEnabled(true);
                    getSupportActionBar().setTitle("Voice Analysis");
                }

                // DrawerToggle 설정
                toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close
                );

                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
                navigationView.setNavigationItemSelectedListener(this);

                // NavigationView 헤더 업데이트
                updateNavigationHeader();

                Log.d(TAG, "NavigationDrawer initialized successfully");
            } else {
                Log.w(TAG, "NavigationDrawer components not found - continuing without sidebar");
            }
        } catch (Exception e) {
            Log.w(TAG, "NavigationDrawer initialization error: " + e.getMessage() + " - continuing without sidebar");
        }
    }

    private void updateNavigationHeader() {
        try {
            if (navigationView == null) return;

            View headerView = navigationView.getHeaderView(0);
            if (headerView == null) return;

            TextView nameTextView = headerView.findViewById(R.id.nav_user_name);
            TextView emailTextView = headerView.findViewById(R.id.nav_user_email);
            TextView statusTextView = headerView.findViewById(R.id.nav_user_status);
            ImageView profileImageView = headerView.findViewById(R.id.nav_user_image);

            updateMenuVisibility();

            if (isUserLoggedIn) {
                if (nameTextView != null) nameTextView.setText(userName);
                if (emailTextView != null) emailTextView.setText(userEmail);
                if (statusTextView != null) statusTextView.setText("Signed in");
                if (profileImageView != null) {
                    try {
                        profileImageView.setImageResource(R.drawable.ic_user_signed_in);
                    } catch (Exception e) {
                        Log.w(TAG, "Could not set profile image: " + e.getMessage());
                    }
                }
            } else {
                if (nameTextView != null) nameTextView.setText("Guest User");
                if (emailTextView != null) emailTextView.setText("Sign in to save recordings");
                if (statusTextView != null) statusTextView.setText("Guest mode");
                if (profileImageView != null) {
                    try {
                        profileImageView.setImageResource(R.drawable.ic_user_guest);
                    } catch (Exception e) {
                        Log.w(TAG, "Could not set guest image: " + e.getMessage());
                    }
                }
            }

            Log.d(TAG, "Navigation header updated");
        } catch (Exception e) {
            Log.e(TAG, "Error updating navigation header: " + e.getMessage(), e);
        }
    }

    private void updateMenuVisibility() {
        try {
            if (navigationView == null) return;

            MenuItem signInItem = navigationView.getMenu().findItem(R.id.nav_sign_in);
            MenuItem signOutItem = navigationView.getMenu().findItem(R.id.nav_sign_out);
            MenuItem accountItem = navigationView.getMenu().findItem(R.id.nav_account);

            if (isUserLoggedIn) {
                if (signInItem != null) signInItem.setVisible(false);
                if (signOutItem != null) signOutItem.setVisible(true);
                if (accountItem != null) accountItem.setVisible(true);
            } else {
                if (signInItem != null) signInItem.setVisible(true);
                if (signOutItem != null) signOutItem.setVisible(false);
                if (accountItem != null) accountItem.setVisible(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating menu visibility: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        try {
            if (id == R.id.nav_new_recording) {
                // 현재 화면이므로 아무것도 하지 않음
                Toast.makeText(this, "Already on recording screen", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_recording_history) {
                if (checkLoginRequired("view your recording history")) {
                    openRecordingHistory();
                }
            } else if (id == R.id.nav_emergency_reports) {
                if (checkLoginRequired("view emergency reports")) {
                    Toast.makeText(this, "Emergency Reports - Coming soon", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.nav_account) {
                if (checkLoginRequired("access account settings")) {
                    openAccountSettings();
                }
            } else if (id == R.id.nav_sign_in) {
                openLoginActivity();
            } else if (id == R.id.nav_sign_out) {
                showSignOutDialog();
            } else if (id == R.id.nav_help) {
                showHelpDialog();
            } else if (id == R.id.nav_about) {
                showAboutDialog();
            }

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling navigation item: " + e.getMessage(), e);
        }

        return true;
    }

    private boolean checkLoginRequired(String feature) {
        if (!isUserLoggedIn) {
            showLoginPrompt("Please sign in to " + feature);
            return false;
        }
        return true;
    }

    private void showLoginPrompt(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Sign in required")
                .setMessage(message)
                .setPositiveButton("Sign In", (dialog, which) -> openLoginActivity())
                .setNegativeButton("Continue as Guest", (dialog, which) ->
                        Toast.makeText(this, "You can still record and analyze symptoms", Toast.LENGTH_LONG).show())
                .show();
    }

    private void openLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void openRecordingHistory() {
        Intent intent = new Intent(this, RecordingHistoryActivity.class);
        intent.putExtra("user_email", userEmail);
        startActivity(intent);
    }

    private void openAccountSettings() {
        Intent intent = new Intent(this, AccountActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_name", userName);
        startActivity(intent);
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOut() {
        mAuth.signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        openLoginActivity();
        finish();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help & Support")
                .setMessage("📱 App Version: 1.0.0\n\n📧 Support: support@symptomate.app\n\n🌐 Website: www.symptomate.app")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About Symptomate")
                .setMessage("🏥 Symptomate v1.0.0\n\nAI-powered medical voice analysis for emergency situations.\n\n⚠️ This app provides AI analysis only and should not replace professional medical advice.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void initializeApp() {
        try {
            initViews();
            initAudioRecord();
            initTimer();
            updateUI(UIState.READY);
            updateUserStatusDisplay();
            Log.d(TAG, "App initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "App initialization error: " + e.getMessage(), e);
            throw e;
        }
    }

    private void initViews() {
        Log.d(TAG, "Initializing views...");

        try {
            // UI 컴포넌트들 찾기
            microphoneIcon = findViewById(R.id.microphone_icon);
            microphoneShadow = findViewById(R.id.microphone_shadow);
            recordingStatusPrimary = findViewById(R.id.recording_status_primary);
            recordingStatusSecondary = findViewById(R.id.recording_status_secondary);
            sessionTimer = findViewById(R.id.session_timer);
            connectionStatus = findViewById(R.id.connection_status);
            sttResultTextView = findViewById(R.id.stt_result_textview);
            startBtn = findViewById(R.id.start_btn);
            stopBtn = findViewById(R.id.stop_analyze_btn);
            userStatusIndicator = findViewById(R.id.user_status_indicator);

            // 웨이브 바들 (옵션)
            initWaveBars();

            // 버튼 리스너 설정
            setButtonListeners();

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Views initialization error: " + e.getMessage(), e);
            throw e;
        }
    }

    private void initWaveBars() {
        try {
            waveBars = new View[]{
                    findViewById(R.id.wave_bar_1), findViewById(R.id.wave_bar_2),
                    findViewById(R.id.wave_bar_3), findViewById(R.id.wave_bar_4),
                    findViewById(R.id.wave_bar_5), findViewById(R.id.wave_bar_6),
                    findViewById(R.id.wave_bar_7)
            };

            int foundBars = 0;
            for (View bar : waveBars) {
                if (bar != null) foundBars++;
            }
            Log.d(TAG, "Wave bars initialized: " + foundBars + " found");
        } catch (Exception e) {
            Log.w(TAG, "Wave bars initialization warning: " + e.getMessage(), e);
            waveBars = new View[0];
        }
    }

    private void setButtonListeners() {
        try {
            if (startBtn != null) {
                startBtn.setOnClickListener(this::onStartClick);
                Log.d(TAG, "Start button listener set");
            }

            if (stopBtn != null) {
                stopBtn.setOnClickListener(this::onStopClick);
                Log.d(TAG, "Stop button listener set");
            }

            if (microphoneIcon != null) {
                microphoneIcon.setOnClickListener(this::onMicrophoneClick);
                Log.d(TAG, "Microphone icon listener set");
            }
        } catch (Exception e) {
            Log.e(TAG, "Button listeners setup error: " + e.getMessage(), e);
        }
    }

    private void updateUserStatusDisplay() {
        if (mainHandler == null) return;

        mainHandler.post(() -> {
            try {
                if (userStatusIndicator != null) {
                    if (isUserLoggedIn) {
                        String displayName = userName.isEmpty() ? "Signed In" : userName;
                        userStatusIndicator.setText("👤 " + displayName);
                        userStatusIndicator.setTextColor(getColor(android.R.color.holo_blue_light));
                    } else {
                        userStatusIndicator.setText("👤 Guest Mode - Sign in to save recordings");
                        userStatusIndicator.setTextColor(getColor(android.R.color.darker_gray));
                    }
                    userStatusIndicator.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating user status: " + e.getMessage(), e);
            }
        });
    }

    private enum UIState { READY, RECORDING, PROCESSING, COMPLETED, ERROR }

    private void updateUI(UIState state) {
        if (mainHandler == null) return;

        mainHandler.post(() -> {
            try {
                Log.d(TAG, "Updating UI to state: " + state);

                switch (state) {
                    case READY:
                        setMicIdle();
                        setWaveIdle();
                        safeUpdateConnectionStatus("Connected", "#22C55E");
                        safeUpdateTextView(recordingStatusPrimary, "Ready to Record", android.R.color.holo_blue_light);
                        safeUpdateTextView(recordingStatusSecondary, "Press microphone to start", android.R.color.darker_gray);
                        safeUpdateButton(startBtn, "🎙️ Start Recording", true);
                        safeUpdateButton(stopBtn, "⏹️ Stop & Analyze", false);
                        safeUpdateTextView(sttResultTextView,
                                "Press the microphone to start voice analysis.\n\n" +
                                        (isUserLoggedIn ? "✅ Your recordings will be saved to your account." :
                                                "ℹ️ Guest mode: You can record and analyze, but recordings won't be saved."),
                                android.R.color.black);
                        break;

                    case RECORDING:
                        setMicRecording();
                        startWaveAnimation();
                        safeUpdateConnectionStatus("Recording", "#EF4444");
                        safeUpdateTextView(recordingStatusPrimary, "Recording...", android.R.color.holo_red_dark);
                        safeUpdateTextView(recordingStatusSecondary, "Speak clearly about your symptoms", android.R.color.darker_gray);
                        safeUpdateButton(startBtn, "🔴 Recording...", false);
                        safeUpdateButton(stopBtn, "⏹️ Stop & Analyze", true);
                        safeUpdateTextView(sttResultTextView, "🎙️ Listening... Describe your symptoms clearly.", android.R.color.black);
                        break;

                    case PROCESSING:
                        setMicIdle();
                        setWaveIdle();
                        safeUpdateConnectionStatus("Analyzing", "#F59E0B");
                        safeUpdateTextView(recordingStatusPrimary, "Analyzing...", android.R.color.holo_orange_dark);
                        safeUpdateTextView(recordingStatusSecondary, "AI is analyzing your symptoms", android.R.color.darker_gray);
                        safeUpdateButton(startBtn, "⏳ Processing...", false);
                        safeUpdateButton(stopBtn, "⏹️ Stop & Analyze", false);
                        safeUpdateTextView(sttResultTextView, "🤖 AI is analyzing your symptoms...\n\nThis may take a few seconds.", android.R.color.black);
                        break;

                    case COMPLETED:
                        setMicIdle();
                        setWaveIdle();
                        safeUpdateConnectionStatus("Complete", "#22C55E");
                        safeUpdateTextView(recordingStatusPrimary, "Analysis Complete", android.R.color.holo_blue_light);
                        safeUpdateTextView(recordingStatusSecondary, "Review results below", android.R.color.darker_gray);
                        safeUpdateButton(startBtn, "🎙️ New Recording", true);
                        safeUpdateButton(stopBtn, "🚨 Emergency Report", true);
                        break;

                    case ERROR:
                        setMicIdle();
                        setWaveIdle();
                        safeUpdateConnectionStatus("Error", "#EF4444");
                        safeUpdateTextView(recordingStatusPrimary, "Error Occurred", android.R.color.holo_red_dark);
                        safeUpdateTextView(recordingStatusSecondary, "Please try again", android.R.color.darker_gray);
                        safeUpdateButton(startBtn, "🎙️ Try Again", true);
                        safeUpdateButton(stopBtn, "⏹️ Stop & Analyze", false);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            }
        });
    }

    private void safeUpdateTextView(TextView textView, String text, int colorRes) {
        if (textView != null) {
            textView.setText(text);
            textView.setTextColor(getColor(colorRes));
        }
    }

    private void safeUpdateButton(Button button, String text, boolean enabled) {
        if (button != null) {
            button.setText(text);
            button.setEnabled(enabled);
        }
    }

    private void safeUpdateConnectionStatus(String text, String colorHex) {
        if (connectionStatus != null) {
            connectionStatus.setText(text);
            connectionStatus.setTextColor(android.graphics.Color.parseColor(colorHex));
        }
    }

    /* 애니메이션 메서드들 - PNG 파일 사용 */
    private void setMicIdle() {
        if (microphoneIcon != null) {
            microphoneIcon.clearAnimation();
            try {
                // voice__1_.png (파란색) 사용
                microphoneIcon.setImageResource(R.drawable.voice__1_);
            } catch (Exception e) {
                Log.w(TAG, "Could not set mic idle image: " + e.getMessage());
            }
        }
        if (microphoneShadow != null) {
            microphoneShadow.clearAnimation();
        }
    }

    private void setMicRecording() {
        if (microphoneIcon != null) {
            try {
                // voice.png (빨간색) 사용
                microphoneIcon.setImageResource(R.drawable.voice);
                microphoneIcon.clearAnimation();

                try {
                    Animation pulse = AnimationUtils.loadAnimation(this, R.anim.mic_pulse_animation);
                    microphoneIcon.startAnimation(pulse);
                } catch (Exception e) {
                    Log.w(TAG, "Microphone animation not available: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not set mic recording image: " + e.getMessage());
            }
        }

        if (microphoneShadow != null) {
            try {
                Animation shadowPulse = AnimationUtils.loadAnimation(this, R.anim.mic_shadow_pulse_animation);
                microphoneShadow.startAnimation(shadowPulse);
            } catch (Exception e) {
                Log.w(TAG, "Shadow animation not available: " + e.getMessage());
            }
        }
    }

    private void startWaveAnimation() {
        if (waveBars != null) {
            for (int i = 0; i < waveBars.length; i++) {
                if (waveBars[i] != null) {
                    try {
                        Animation barAnim = AnimationUtils.loadAnimation(this, R.anim.audio_wave);
                        barAnim.setStartOffset(i * 100L);
                        waveBars[i].startAnimation(barAnim);
                    } catch (Exception e) {
                        Log.w(TAG, "Wave animation error for bar " + i + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private void setWaveIdle() {
        if (waveBars != null) {
            for (View waveBar : waveBars) {
                if (waveBar != null) {
                    waveBar.clearAnimation();
                }
            }
        }
    }

    /* 타이머 관리 */
    private void initTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    secondsElapsed++;
                    int minutes = secondsElapsed / 60;
                    int seconds = secondsElapsed % 60;
                    String timeString = String.format("%02d:%02d", minutes, seconds);
                    if (sessionTimer != null) {
                        sessionTimer.setText(timeString);
                    }
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        Log.d(TAG, "Timer initialized");
    }

    private void resetTimer() {
        secondsElapsed = 0;
        if (sessionTimer != null) sessionTimer.setText("00:00");
    }

    private void startTimer() {
        if (timerHandler != null) timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerHandler != null) timerHandler.removeCallbacks(timerRunnable);
    }

    /* 버튼 이벤트 핸들러 - 실제 기능 복원 */
    private void onStartClick(View v) {
        Log.d(TAG, "Start button clicked - isRecording: " + isRecording);
        try {
            if (!isRecording) {
                startRecording();
            } else {
                resetForNewRecording();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartClick: " + e.getMessage(), e);
            updateUI(UIState.ERROR);
        }
    }

    private void onStopClick(View v) {
        Log.d(TAG, "Stop button clicked - isRecording: " + isRecording);
        try {
            if (isRecording) {
                stopRecordingAndProcess();
            } else if (!recognizedText.isEmpty() && stopBtn != null && stopBtn.getText().toString().contains("Emergency Report")) {
                navigateToEmergencyReport();
            } else {
                resetForNewRecording();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStopClick: " + e.getMessage(), e);
            updateUI(UIState.ERROR);
        }
    }

    private void onMicrophoneClick(View v) {
        Log.d(TAG, "Microphone clicked - isRecording: " + isRecording);
        try {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecordingAndProcess();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onMicrophoneClick: " + e.getMessage(), e);
            updateUI(UIState.ERROR);
        }
    }

    private void navigateToEmergencyReport() {
        try {
            Log.d(TAG, "Navigating to SMS Report Activity");

            Intent intent = new Intent(this, SmsReportActivity.class);
            intent.putExtra("recognized_text", recognizedText);
            intent.putExtra("chatgpt_response", chatGPTResponse);
            intent.putExtra("session_duration", secondsElapsed);
            intent.putExtra("user_logged_in", isUserLoggedIn);
            intent.putExtra("user_email", userEmail);
            intent.putExtra("user_name", userName);

            startActivity(intent);
            Log.d(TAG, "Successfully launched SmsReportActivity");

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to SMS report: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening SMS report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetForNewRecording() {
        try {
            recognizedText = "";
            chatGPTResponse = "";
            resetTimer();
            updateUI(UIState.READY);
            Log.d(TAG, "Reset for new recording");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting for new recording: " + e.getMessage(), e);
        }
    }

    /* 권한 관리 */
    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET};
            boolean allGranted = true;

            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                try {
                    initializeApp();
                    Log.d(TAG, "Permissions granted, app initialized");
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing after permission grant: " + e.getMessage(), e);
                    updateUI(UIState.ERROR);
                }
            } else {
                Toast.makeText(this, "Permissions are required for voice recognition.", Toast.LENGTH_LONG).show();
                updateUI(UIState.ERROR);
            }
        }
    }

    /* AudioRecord 관리 - 실제 녹음 기능 */
    private void initAudioRecord() {
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                throw new IllegalStateException("Could not get AudioRecord buffer size.");
            }

            bufferSize = minBufferSize * BUFFER_SIZE_MULTIPLIER;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Audio recording permission not granted");
            }

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord initialization failed.");
            }

            Log.d(TAG, "AudioRecord initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "AudioRecord initialization error", e);
            throw new RuntimeException("Audio recording setup failed: " + e.getMessage());
        }
    }

    private void startRecording() {
        try {
            Log.d(TAG, "Starting recording...");

            if (audioRecord == null) {
                throw new IllegalStateException("AudioRecord not initialized");
            }

            audioBuffer.reset();
            isRecording = true;
            audioRecord.startRecording();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                try {
                    while (isRecording && !Thread.currentThread().isInterrupted()) {
                        int bytesRead = audioRecord.read(buffer, 0, bufferSize);
                        if (bytesRead > 0) {
                            audioBuffer.write(buffer, 0, bytesRead);
                        }

                        // 최대 녹음 시간 체크
                        if (audioBuffer.size() >= SAMPLE_RATE * 2 * (MAX_RECORDING_TIME_MS / 1000)) {
                            Log.d(TAG, "Maximum recording time reached");
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Recording error: " + e.getMessage(), e);
                }
            });

            recordingThread.start();
            startTimer();
            updateUI(UIState.RECORDING);

            Log.d(TAG, "Recording started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage(), e);
            updateUI(UIState.ERROR);
            throw e;
        }
    }

    private void stopRecordingAndProcess() {
        try {
            Log.d(TAG, "Stopping recording...");

            isRecording = false;

            if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }

            if (recordingThread != null && recordingThread.isAlive()) {
                recordingThread.interrupt();
                try {
                    recordingThread.join(1000);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Thread join interrupted");
                }
            }

            stopTimer();
            updateUI(UIState.PROCESSING);

            byte[] audioData = audioBuffer.toByteArray();
            Log.d(TAG, "Audio data captured: " + audioData.length + " bytes");

            if (audioData.length > 0) {
                processWithGoogleSTT(audioData);
            } else {
                Log.w(TAG, "No audio data captured");
                mainHandler.post(() -> {
                    safeUpdateTextView(sttResultTextView, "No audio was recorded. Please try again.", android.R.color.holo_red_dark);
                    updateUI(UIState.ERROR);
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage(), e);
            updateUI(UIState.ERROR);
            throw e;
        }
    }

    // ✅ 완전히 수정된 Google STT 메서드 - 영어 우선 + JSON 에러 해결
    private void processWithGoogleSTT(byte[] audioData) {
        if (executorService == null) {
            Log.e(TAG, "Executor service is null");
            return;
        }

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Processing audio with Google STT (English optimized)...");

                // 오디오 데이터 검증
                if (audioData == null || audioData.length == 0) {
                    Log.w(TAG, "No audio data to process");
                    mainHandler.post(() -> {
                        safeUpdateTextView(sttResultTextView,
                                "❌ No audio recorded. Please try again.",
                                android.R.color.holo_red_dark);
                        updateUI(UIState.ERROR);
                    });
                    return;
                }

                // 오디오 길이 확인 (최소 1초 이상)
                int minBytes = SAMPLE_RATE * 2; // 16-bit = 2 bytes per sample
                if (audioData.length < minBytes) {
                    Log.w(TAG, "Audio too short: " + audioData.length + " bytes");
                    mainHandler.post(() -> {
                        safeUpdateTextView(sttResultTextView,
                                "🎙️ Recording too short. Please record for at least 3 seconds.",
                                android.R.color.holo_orange_dark);
                        updateUI(UIState.ERROR);
                    });
                    return;
                }

                // Base64 인코딩
                String base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP);
                Log.d(TAG, "Audio encoded to base64, length: " + base64Audio.length());

                // ✅ JSON 구문 에러 해결 - 단계별 생성
                JSONObject audioObject = new JSONObject();
                audioObject.put("content", base64Audio);

                // ✅ 영어 우선 STT 설정
                JSONArray alternativeLanguages = new JSONArray();
                alternativeLanguages.put("ko-KR");  // 한국어를 대체 언어로
                alternativeLanguages.put("ja-JP");  // 일본어도 지원

                JSONObject config = new JSONObject();
                config.put("encoding", "LINEAR16");
                config.put("sampleRateHertz", SAMPLE_RATE);
                config.put("languageCode", "en-US");  // ✅ 영어 우선으로 변경
                config.put("alternativeLanguageCodes", alternativeLanguages);
                config.put("maxAlternatives", 5);
                config.put("enableAutomaticPunctuation", true);
                config.put("enableWordConfidence", true);
                config.put("useEnhanced", true);
                config.put("profanityFilter", false);
                config.put("enableSpokenPunctuation", true);
                config.put("model", "latest_long");

                // ✅ 최종 요청 JSON 생성
                JSONObject requestJson = new JSONObject();
                requestJson.put("audio", audioObject);
                requestJson.put("config", config);

                Log.d(TAG, "STT Request prepared successfully");

                // API 호출
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(20000);  // 20초 연결 타임아웃
                connection.setReadTimeout(30000);     // 30초 읽기 타임아웃
                connection.setDoOutput(true);

                // 요청 전송
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestJson.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Google STT response code: " + responseCode);

                // 응답 읽기
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                String responseStr = response.toString();
                Log.d(TAG, "STT Response: " + responseStr);

                if (responseCode >= 200 && responseCode < 300) {
                    // 성공 응답 처리
                    JSONObject responseJson = new JSONObject(responseStr);

                    if (responseJson.has("results")) {
                        JSONArray results = responseJson.getJSONArray("results");

                        if (results.length() > 0) {
                            JSONObject firstResult = results.getJSONObject(0);
                            JSONArray alternatives = firstResult.getJSONArray("alternatives");

                            if (alternatives.length() > 0) {
                                JSONObject bestAlternative = alternatives.getJSONObject(0);
                                recognizedText = bestAlternative.getString("transcript");

                                // 신뢰도 확인
                                double confidence = bestAlternative.optDouble("confidence", 0.0);
                                Log.d(TAG, "Recognized text (English): '" + recognizedText + "' (confidence: " + confidence + ")");

                                // 텍스트 후처리
                                recognizedText = recognizedText.trim();
                                if (recognizedText.isEmpty()) {
                                    throw new Exception("Empty recognition result");
                                }

                                mainHandler.post(() -> {
                                    String displayText = "🎙️ Recognized Speech:\n\"" + recognizedText + "\"\n\n";

                                    if (confidence > 0.8) {
                                        displayText += "✅ Very High Accuracy (" + Math.round(confidence * 100) + "%)\n\n";
                                    } else if (confidence > 0.6) {
                                        displayText += "✅ Good Accuracy (" + Math.round(confidence * 100) + "%)\n\n";
                                    } else if (confidence > 0.4) {
                                        displayText += "⚠️ Fair Accuracy (" + Math.round(confidence * 100) + "%) - Consider re-recording\n\n";
                                    } else {
                                        displayText += "❓ Low Accuracy - Re-recording recommended\n\n";
                                    }

                                    displayText += "🤖 AI Analysis in progress...";
                                    safeUpdateTextView(sttResultTextView, displayText, android.R.color.black);
                                });

                                // ChatGPT API 호출
                                callChatGPTAPI(recognizedText);
                                return;
                            }
                        }
                    }

                    // 결과가 없는 경우
                    throw new Exception("No speech recognized in audio");

                } else {
                    // HTTP 에러
                    throw new Exception("STT API error (HTTP " + responseCode + "): " + responseStr);
                }

            } catch (Exception e) {
                Log.e(TAG, "STT processing error: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    String errorMessage;

                    if (e.getMessage().contains("No speech recognized")) {
                        errorMessage = "🔇 No speech detected.\n\n💡 Tips for better recognition:\n" +
                                "• Record in a quiet environment\n" +
                                "• Speak clearly into the microphone\n" +
                                "• Maintain 6-12 inches distance from mic\n" +
                                "• Record for at least 3-5 seconds\n" +
                                "• Speak at normal pace";
                    } else if (e.getMessage().contains("timeout") || e.getMessage().contains("connection")) {
                        errorMessage = "🌐 Network connection issue.\n\n" +
                                "• Check your internet connection\n" +
                                "• Try again in a moment";
                    } else if (e.getMessage().contains("too short")) {
                        errorMessage = "⏱️ Recording too short.\n\n" +
                                "• Please record for at least 3 seconds\n" +
                                "• Describe your symptoms in detail";
                    } else {
                        errorMessage = "❌ Audio processing error.\n\n" +
                                "Error: " + e.getMessage() + "\n\n" +
                                "Please try again or call emergency services if urgent.";
                    }

                    safeUpdateTextView(sttResultTextView, errorMessage, android.R.color.holo_red_dark);
                    updateUI(UIState.ERROR);
                });
            }
        });
    }

    // ✅ 수정된 ChatGPT API 호출 메서드
    private void callChatGPTAPI(String userText) {
        try {
            if (chatGPTHelper == null) {
                Log.w(TAG, "ChatGPT helper is null, showing text only");
                mainHandler.post(() -> {
                    String displayText = "🎙️ 인식된 음성:\n" + userText +
                            "\n\n❗ AI 분석을 사용할 수 없습니다.\n응급 상황이라면 119에 즉시 신고하세요." +
                            "\n\n" + (isUserLoggedIn ? "✅ 음성 기록이 저장되었습니다." :
                            "ℹ️ 게스트 모드: 음성 기록이 저장되지 않습니다.");
                    safeUpdateTextView(sttResultTextView, displayText, android.R.color.black);
                    updateUI(UIState.COMPLETED);

                    if (isUserLoggedIn) {
                        saveRecordingToFirestore();
                    }
                });
                return;
            }

            String prompt = "다음은 의료 증상에 대한 음성 인식 결과입니다. 전문적이고 신뢰할 수 있는 의료 분석을 제공해주세요:\n\n" + userText +
                    "\n\n분석 내용:\n1. 주요 증상 요약\n2. 가능한 원인들\n3. 응급도 평가 (1-10)\n4. 권장 조치사항\n\n" +
                    "중요: 이는 전문의 진단을 대체할 수 없으며, 응급 상황 시 즉시 119에 신고하세요.";

            // ✅ 수정된 부분: getChatGPTResponse → callAPI 사용
            chatGPTHelper.callAPI(prompt, new ChatGPT.ChatGPTCallback() {
                @Override
                public void onSuccess(String response) {
                    chatGPTResponse = response;

                    mainHandler.post(() -> {
                        String displayText = "🎙️ 인식된 음성:\n" + userText +
                                "\n\n🤖 AI 의료 분석:\n" + response +
                                "\n\n" + (isUserLoggedIn ? "✅ 분석 결과가 저장되었습니다." :
                                "ℹ️ 게스트 모드: 분석 결과가 저장되지 않습니다.");

                        safeUpdateTextView(sttResultTextView, displayText, android.R.color.black);
                        updateUI(UIState.COMPLETED);

                        // Firebase에 저장 (로그인된 경우만)
                        if (isUserLoggedIn) {
                            saveRecordingToFirestore();
                        }
                    });

                    Log.d(TAG, "ChatGPT analysis completed successfully");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "ChatGPT error: " + error);
                    mainHandler.post(() -> {
                        String displayText = "🎙️ 인식된 음성:\n" + userText +
                                "\n\n❌ AI 분석 중 오류가 발생했습니다: " + error +
                                "\n\n그래도 응급 상황이라면 119에 신고하세요." +
                                "\n\n" + (isUserLoggedIn ? "✅ 음성 기록이 저장되었습니다." :
                                "ℹ️ 게스트 모드: 음성 기록이 저장되지 않습니다.");

                        safeUpdateTextView(sttResultTextView, displayText, android.R.color.black);
                        updateUI(UIState.COMPLETED);

                        if (isUserLoggedIn) {
                            saveRecordingToFirestore();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error calling ChatGPT API: " + e.getMessage(), e);
            mainHandler.post(() -> {
                String displayText = "🎙️ 인식된 음성:\n" + userText +
                        "\n\n❌ AI 분석을 사용할 수 없습니다.\n응급 상황이라면 119에 즉시 신고하세요." +
                        "\n\n" + (isUserLoggedIn ? "✅ 음성 기록이 저장되었습니다." :
                        "ℹ️ 게스트 모드: 음성 기록이 저장되지 않습니다.");
                safeUpdateTextView(sttResultTextView, displayText, android.R.color.black);
                updateUI(UIState.COMPLETED);

                if (isUserLoggedIn) {
                    saveRecordingToFirestore();
                }
            });
        }
    }

    private void saveRecordingToFirestore() {
        if (!isUserLoggedIn || userEmail.isEmpty()) return;

        try {
            Map<String, Object> recording = new HashMap<>();
            recording.put("timestamp", System.currentTimeMillis());
            recording.put("duration", secondsElapsed);
            recording.put("transcription", recognizedText);
            recording.put("analysis", chatGPTResponse);
            recording.put("reported_to_911", false);
            recording.put("user_email", userEmail);

            db.collection("users").document(userEmail)
                    .collection("recordings")
                    .add(recording)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Recording saved with ID: " + documentReference.getId());
                        Toast.makeText(this, "Recording saved to your account", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error adding recording", e);
                        Toast.makeText(this, "Error saving recording", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error saving recording to Firestore: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (toggle != null) {
            toggle.syncState();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "=== Main_page onDestroy START ===");

        try {
            if (isRecording) {
                isRecording = false;
            }

            if (audioRecord != null) {
                try {
                    if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                        audioRecord.release();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing AudioRecord", e);
                } finally {
                    audioRecord = null;
                }
            }

            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }

            if (timerHandler != null) {
                timerHandler.removeCallbacks(timerRunnable);
            }

            if (audioBuffer != null) {
                try {
                    audioBuffer.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing audioBuffer", e);
                }
            }

            if (chatGPTHelper != null) {
                chatGPTHelper.cleanup();
            }

            Log.d(TAG, "=== Main_page onDestroy SUCCESS ===");

        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        } finally {
            super.onDestroy();
        }
    }
}