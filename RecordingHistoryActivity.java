package com.example.answer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordingHistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "RecordingHistory";


    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private RecyclerView recordingsRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView emptyStateTextView;
    private RecordingHistoryAdapter adapter;
    private List<RecordingItem> recordingsList;

 
    private String userEmail;
    private String userName;


    private FirebaseAuth mAuth;
    private DatabaseReference userRecordingsRef;
    private ValueEventListener recordingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_history);

        mAuth = FirebaseAuth.getInstance();

        if (!loadUserInfo()) {
            Toast.makeText(this, "User information is missing. Cannot load history.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupFirebaseDatabase();
        initNavigationDrawer();
        initViews();
    }

    private boolean loadUserInfo() {
        userEmail = getIntent().getStringExtra("user_email");
        userName = getIntent().getStringExtra("user_name");

        if (userEmail == null || userEmail.trim().isEmpty()) {
            return false;
        }
        if (userName == null || userName.trim().isEmpty()) {
            userName = userEmail.split("@")[0];
        }
        return true;
    }

    private void setupFirebaseDatabase() {
        String encodedEmail = userEmail.replace(".", ",");
        userRecordingsRef = FirebaseDatabase.getInstance().getReference("recordings").child(encodedEmail);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadRecordings();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userRecordingsRef != null && recordingsListener != null) {
            userRecordingsRef.removeEventListener(recordingsListener);
        }
    }

    private void initNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Recording History");
        }
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        updateNavigationHeader();
    }

    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView nameTextView = headerView.findViewById(R.id.nav_user_name);
        TextView emailTextView = headerView.findViewById(R.id.nav_user_email);
        nameTextView.setText(userName);
        emailTextView.setText(userEmail);
    }

    private void initViews() {
        recordingsRecyclerView = findViewById(R.id.recordings_recycler_view);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        emptyStateTextView = findViewById(R.id.empty_state_text);
        recordingsList = new ArrayList<>();
        adapter = new RecordingHistoryAdapter(recordingsList);
        recordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recordingsRecyclerView.setAdapter(adapter);
        adapter.setOnRecordingClickListener(this::showRecordingDetails);
    }

    private void loadRecordings() {
        showLoading(true);
        recordingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recordingsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        RecordingItem item = snapshot.getValue(RecordingItem.class);
                        if (item != null) {
                            item.id = snapshot.getKey();
                            recordingsList.add(item);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing recording data", e);
                    }
                }
                Collections.sort(recordingsList, (o1, o2) -> {
                    if (o1.timestamp == null || o2.timestamp == null) return 0;
                    return Long.compare(o2.timestamp, o1.timestamp);
                });
                adapter.notifyDataSetChanged();
                updateEmptyState();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(RecordingHistoryActivity.this, "Failed to load data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        userRecordingsRef.addValueEventListener(recordingsListener);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_new_recording) {
            finish();
        } else if (id == R.id.nav_recording_history) {
            Toast.makeText(this, "Already here", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_account) {
            openAccountSettings();
        } else if (id == R.id.nav_sign_out) {
            showSignOutDialog();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void showRecordingDetails(RecordingItem recording) {
      
        String details = "Date: " + recording.getFormattedDate() + "\n" +
                "Duration: " + recording.getFormattedDuration() + "\n\n" +
                "Transcription:\n" + (recording.transcription != null ? recording.transcription : "N/A") + "\n\n" +
                "AI Analysis:\n" + (recording.analysis != null ? recording.analysis : "N/A");

        new AlertDialog.Builder(this)
                .setTitle("Recording Details")
                .setMessage(details)
                .setPositiveButton("Close", null)
                .setNeutralButton("Delete", (dialog, which) -> confirmDeleteRecording(recording))
                .show();
    }

    private void confirmDeleteRecording(RecordingItem recording) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to permanently delete this recording?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecording(recording))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecording(RecordingItem recording) {
        if (recording.id == null || recording.id.isEmpty()) {
            Toast.makeText(this, "Cannot delete item: Invalid ID.", Toast.LENGTH_SHORT).show();
            return;
        }
 
        userRecordingsRef.child(recording.id).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Recording deleted.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete recording.", Toast.LENGTH_SHORT).show());
    }

    private void showLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recordingsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        emptyStateTextView.setVisibility(recordingsList.isEmpty() ? View.VISIBLE : View.GONE);
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


    public static class RecordingItem {
        public String id;
        public Long timestamp;
        public Long duration;
        public String transcription;
        public String analysis;
        public boolean reportedTo911;
        public String audioUrl; 

        public RecordingItem() {
          
        }


        public RecordingItem(Long timestamp, Long duration, String transcription, String analysis, boolean reportedTo911, String audioUrl) {
            this.timestamp = timestamp;
            this.duration = duration;
            this.transcription = transcription;
            this.analysis = analysis;
            this.reportedTo911 = reportedTo911;
            this.audioUrl = audioUrl; // [추가]
        }

        public String getFormattedDate() {
            if (timestamp == null) return "Unknown date";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        public String getFormattedDuration() {
            if (duration == null) return "0:00";
            long minutes = duration / 60;
            long seconds = duration % 60;
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }

        public String getShortTranscription() {
            if (transcription == null || transcription.trim().isEmpty()) {
                return "No transcription available";
            }
            if (transcription.length() > 100) {
                return transcription.substring(0, 100) + "...";
            }
            return transcription;
        }
    }
}
