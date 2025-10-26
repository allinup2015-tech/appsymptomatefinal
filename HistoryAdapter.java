package com.example.answer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<HistoryItem> historyList;

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recordingDate, recordingDuration, recordingTranscription, statusBadge, emergencyBadge;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recordingDate = itemView.findViewById(R.id.recording_date);
            recordingDuration = itemView.findViewById(R.id.recording_duration);
            recordingTranscription = itemView.findViewById(R.id.recording_transcription);
            statusBadge = itemView.findViewById(R.id.status_badge);
            emergencyBadge = itemView.findViewById(R.id.emergency_badge);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_history_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        holder.recordingDate.setText(item.getDateFormatted());
        int min = item.sessionDuration / 60;
        int sec = item.sessionDuration % 60;
        holder.recordingDuration.setText("🎙️ " + min + ":" + String.format("%02d", sec));
        holder.recordingTranscription.setText(item.recognizedText != null ? item.recognizedText : "");
        holder.statusBadge.setText("✅ Analyzed");
        if (item.reportedTo911) {
            holder.emergencyBadge.setVisibility(View.VISIBLE);
        } else {
            holder.emergencyBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }
}
