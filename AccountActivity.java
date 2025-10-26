package com.example.answer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 계정 설정 화면 (사이드바 포함)
 * 로그인한 사용자의 계정 정보 및 설정 옵션
 */
public class AccountActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AccountActivity";

    /* NavigationDrawer */
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    /* UI Components */
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private TextView accountStatusTextView;
    private Button signOutButton;
    private Button deleteAccountButton;
    private Button recordingHistoryButton;
    private Button privacySettingsButton;

    /* User State */
    private String userEmail;
    private String userName;
    private boolean isUserLoggedIn = true; // 이 화면은 로그인 필요
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Firebase 초기화
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 사용자 정보 가져오기
        userEmail = getIntent().getStringExtra("user_email");
        userName = getIntent().getStringExtra("user_name");

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "User information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (userName == null || userName.isEmpty()) {
            userName = userEmail.split("@")[0];
        }

        initNavigationDrawer();
        initViews();
        updateUserInfo();
    }

    private void initNavigationDrawer() {
        try {
            // NavigationDrawer 컴포넌트 찾기
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            toolbar = findViewById(R.id.toolbar);

            // 툴바 설정
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setTitle("Account Settings");
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

        } catch (Exception e) {
            Log.e(TAG, "Error initializing NavigationDrawer: " + e.getMessage(), e);
        }
    }

    private void updateNavigationHeader() {
        try {
            View headerView = navigationView.getHeaderView(0);
            TextView nameTextView = headerView.findViewById(R.id.nav_user_name);
            TextView emailTextView = headerView.findViewById(R.id.nav_user_email);
            TextView statusTextView = headerView.findViewById(R.id.nav_user_status);

            if (nameTextView != null) nameTextView.setText(userName);
            if (emailTextView != null) emailTextView.setText(userEmail);
            if (statusTextView != null) statusTextView.setText("Signed in");

            // 메뉴 가시성 업데이트
            MenuItem signInItem = navigationView.getMenu().findItem(R.id.nav_sign_in);
            MenuItem signOutItem = navigationView.getMenu().findItem(R.id.nav_sign_out);
            MenuItem accountItem = navigationView.getMenu().findItem(R.id.nav_account);

            if (signInItem != null) signInItem.setVisible(false);
            if (signOutItem != null) signOutItem.setVisible(true);
            if (accountItem != null) accountItem.setVisible(true);

        } catch (Exception e) {
            Log.e(TAG, "Error updating navigation header: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        try {
            if (id == R.id.nav_new_recording) {
                openRecordingScreen();
            } else if (id == R.id.nav_recording_history) {
                openRecordingHistory();
            } else if (id == R.id.nav_emergency_reports) {
                Toast.makeText(this, "Emergency Reports - Coming soon", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_account) {
                // 현재 화면이므로 아무것도 하지 않음
                Toast.makeText(this, "Already on account settings", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_sign_in) {
                openLoginActivity();
            } else if (id == R.id.nav_sign_out) {
                showSignOutDialog();
            } else if (id == R.id.nav_help) {
                showHelpDialog();
            } else if (id == R.id.nav_about) {
                showAboutDialog();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
        } catch (Exception e) {
            Log.e(TAG, "Error handling navigation item: " + e.getMessage(), e);
        }

        return true;
    }

    private void openRecordingScreen() {
        Intent intent = new Intent(this, Main_Page.class);
        intent.putExtra("user_logged_in", isUserLoggedIn);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_name", userName);
        startActivity(intent);
        finish();
    }

    private void openRecordingHistory() {
        Intent intent = new Intent(this, RecordingHistoryActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_name", userName);
        startActivity(intent);
    }

    private void openLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help & Support")
                .setMessage("📱 Account Help\n\n• Manage your account settings\n• View and export your data\n• Control privacy preferences\n• Get support when needed")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About Symptomate")
                .setMessage("🏥 Symptomate v1.0.0\n\nAI-powered medical voice analysis for emergency situations.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void initViews() {
        userNameTextView = findViewById(R.id.user_name_text);
        userEmailTextView = findViewById(R.id.user_email_text);
        accountStatusTextView = findViewById(R.id.account_status_text);
        signOutButton = findViewById(R.id.sign_out_button);
        deleteAccountButton = findViewById(R.id.delete_account_button);
        recordingHistoryButton = findViewById(R.id.recording_history_button);
        privacySettingsButton = findViewById(R.id.privacy_settings_button);

        // 버튼 리스너 설정
        if (signOutButton != null) {
            signOutButton.setOnClickListener(v -> showSignOutDialog());
        }
        if (deleteAccountButton != null) {
            deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
        }
        if (recordingHistoryButton != null) {
            recordingHistoryButton.setOnClickListener(v -> openRecordingHistory());
        }
        if (privacySettingsButton != null) {
            privacySettingsButton.setOnClickListener(v -> showPrivacySettings());
        }
    }

    private void updateUserInfo() {
        if (userNameTextView != null) userNameTextView.setText(userName);
        if (userEmailTextView != null) userEmailTextView.setText(userEmail);
        if (accountStatusTextView != null) accountStatusTextView.setText("✅ Account verified and active");
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?\n\nYour recordings will remain saved in the cloud.")
                .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOut() {
        try {
            mAuth.signOut();
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

            // 로그인 화면으로 이동
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error signing out", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("⚠️ WARNING: This action cannot be undone!\n\n" +
                        "Deleting your account will:\n" +
                        "• Remove all your recorded data\n" +
                        "• Delete your analysis history\n" +
                        "• Cancel your account permanently\n\n" +
                        "Are you absolutely sure?")
                .setPositiveButton("DELETE", (dialog, which) -> showFinalDeleteConfirmation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFinalDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Final Confirmation")
                .setMessage("This is your last chance!\n\nAre you absolutely sure you want to delete your account and all data?")
                .setPositiveButton("DELETE ACCOUNT", (dialog, which) -> deleteAccount())
                .setNegativeButton("Keep My Account", null)
                .show();
    }

    private void deleteAccount() {
        // TODO: 실제 계정 삭제 구현
        Toast.makeText(this, "Account deletion feature coming soon", Toast.LENGTH_LONG).show();
    }

    private void showPrivacySettings() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy Settings")
                .setMessage("🔒 Your Privacy Matters\n\n" +
                        "Current Settings:\n" +
                        "• Voice recordings: Encrypted in transit and at rest\n" +
                        "• Data retention: 1 year (configurable)\n" +
                        "• Third-party sharing: Disabled\n" +
                        "• Analytics: Anonymous usage only\n\n" +
                        "🛡️ We never share your medical data with third parties without your explicit consent.")
                .setPositiveButton("Got it", null)
                .setNeutralButton("Download My Data", (dialog, which) ->
                        Toast.makeText(this, "Data export feature coming soon", Toast.LENGTH_SHORT).show())
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }
}