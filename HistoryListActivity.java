package com.example.answer;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        loadDataFromFirebase();
    }

    private void loadDataFromFirebase() {
        String userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null) {
            Toast.makeText(this, "로그인 정보가 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String encodedEmail = userEmail.replace(".", ",");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("recordingsHistory").child(encodedEmail);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                historyList.clear();
                for (DataSnapshot snapshot : ds.getChildren()) {
                    HistoryItem item = snapshot.getValue(HistoryItem.class);
                    if (item != null) historyList.add(item);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError dbError) {
                Toast.makeText(HistoryListActivity.this, "DB Error: " + dbError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
