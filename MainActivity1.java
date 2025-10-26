package com.example.answer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * NavigationDrawer 구조의 메인 액티비티
 * 로그인 상태에 따라 기능 제한/활성화
 */
public class MainActivity1 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    /* UI Components */
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    /* User State */
    private boolean isUserLoggedIn = false;
    private String userEmail = "";
    private String userName = "";

    /* Firebase */
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_main.xml을 사용한다고 가정

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupNavigationDrawer();
        loadUserState();
        updateNavigationHeader();

        // 기본으로 녹음 화면 표시
        if (savedInstanceState == null) {
            // 앱 시작 시 첫 화면 로드는 여기서 관리합니다.
            // loadRecordingFragment(); // 필요 시 주석 해제
        }

        Log.d(TAG, "MainActivity initialized - User logged in: " + isUserLoggedIn);
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
    }

    private void setupNavigationDrawer() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    /**
     * 사용자 상태 로드 (Intent 또는 Firebase에서)
     */
    private void loadUserState() {
        Intent intent = getIntent();
        isUserLoggedIn = intent.getBooleanExtra("user_logged_in", false);

        if (isUserLoggedIn) {
            userEmail = intent.getStringExtra("user_email");
            userName = intent.getStringExtra("user_name");

            if (userName == null || userName.isEmpty()) {
                userName = userEmail != null ? userEmail.split("@")[0] : "User";
            }
        } else {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                isUserLoggedIn = true;
                userEmail = currentUser.getEmail();
                userName = currentUser.getDisplayName();
                if (userName == null || userName.isEmpty()) {
                    userName = userEmail != null ? userEmail.split("@")[0] : "User";
                }
            }
        }

        Log.d(TAG, "User state loaded - Email: " + userEmail + ", Name: " + userName);
    }

    /**
     * NavigationDrawer 헤더 업데이트
     */
    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView nameTextView = headerView.findViewById(R.id.nav_user_name);
        TextView emailTextView = headerView.findViewById(R.id.nav_user_email);
        TextView statusTextView = headerView.findViewById(R.id.nav_user_status);

        if (isUserLoggedIn) {
            nameTextView.setText(userName);
            emailTextView.setText(userEmail);
            statusTextView.setText("Signed in");
        } else {
            nameTextView.setText("Guest User");
            emailTextView.setText("Sign in to save your recordings");
            statusTextView.setText("Guest mode");
        }

        if (!isUserLoggedIn) {
            headerView.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showLoginPrompt();
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_new_recording) {
            loadRecordingFragment();
        } else if (id == R.id.nav_recording_history) {
            if (checkLoginRequired("view your recording history")) {
                // 'Recording History' 메뉴 클릭 시 RecordingHistoryActivity를 시작합니다.
                Intent intent = new Intent(this, RecordingHistoryActivity.class);
                // 사용자 정보를 Intent에 담아 전달합니다.
                intent.putExtra("user_email", userEmail);
                intent.putExtra("user_name", userName);
                startActivity(intent);
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
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * 녹음 화면 로드
     */
    private void loadRecordingFragment() {
        // Main_Page가 별도의 Activity이므로 Intent로 시작합니다.
        Intent intent = new Intent(this, Main_Page.class);
        intent.putExtra("user_logged_in", isUserLoggedIn);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("user_name", userName);
        startActivity(intent);
        Log.d(TAG, "Loaded recording screen");
    }

    private boolean checkLoginRequired(String feature) {
        if (!isUserLoggedIn) {
            showLoginPrompt("Please sign in to " + feature);
            return false;
        }
        return true;
    }

    private void showLoginPrompt() {
        showLoginPrompt("Sign in to save your recordings and view history");
    }

    private void showLoginPrompt(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Sign in required")
                .setMessage(message)
                .setPositiveButton("Sign In", (dialog, which) -> openLoginActivity())
                .setNegativeButton("Continue as Guest", null)
                .show();
    }

    private void openLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
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
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean newLoginState = (currentUser != null);

        if (newLoginState != isUserLoggedIn) {
            recreate();
        }
    }
}