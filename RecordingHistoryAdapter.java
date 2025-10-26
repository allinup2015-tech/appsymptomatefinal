package com.example.answer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecordingHistoryAdapter extends RecyclerView.Adapter<RecordingHistoryAdapter.RecordingViewHolder> {

    private final List<RecordingHistoryActivity.RecordingItem> recordings;
    private OnRecordingClickListener listener;

    public interface OnRecordingClickListener {
        void onRecordingClick(RecordingHistoryActivity.RecordingItem recording);
    }

    public RecordingHistoryAdapter(List<RecordingHistoryActivity.RecordingItem> recordings) {
        this.recordings = recordings;
    }

    public void setOnRecordingClickListener(OnRecordingClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording_history, parent, false);
        return new RecordingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position) {
        RecordingHistoryActivity.RecordingItem recording = recordings.get(position);
        holder.bind(recording);
    }

    @Override
    public int getItemCount() {
        return recordings != null ? recordings.size() : 0;
    }

    class RecordingViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final TextView durationText;
        private final TextView transcriptionText;
        private final TextView statusBadge;
        private final TextView emergencyBadge;
        private final Context context;

        public RecordingViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext(); // Context 참조 저장
            dateText = itemView.findViewById(R.id.recording_date);
            durationText = itemView.findViewById(R.id.recording_duration);
            transcriptionText = itemView.findViewById(R.id.recording_transcription);
            statusBadge = itemView.findViewById(R.id.status_badge);
            emergencyBadge = itemView.findViewById(R.id.emergency_badge);

            // 클릭 리스너 설정
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onRecordingClick(recordings.get(position));
                }
            });
        }

        public void bind(final RecordingHistoryActivity.RecordingItem recording) {
            dateText.setText(recording.getFormattedDate());
            durationText.setText(String.format("🎙️ %s", recording.getFormattedDuration()));
            transcriptionText.setText(recording.getShortTranscription());

            // 분석 상태 뱃지 설정
            if (recording.analysis != null && !recording.analysis.isEmpty()) {
                statusBadge.setText("Analyzed");
                statusBadge.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                statusBadge.setVisibility(View.VISIBLE);
            } else {
                statusBadge.setText("Processing");
                statusBadge.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                statusBadge.setVisibility(View.VISIBLE);
            }

            // 긴급 신고 뱃지 설정
            if (recording.reportedTo911) {
                emergencyBadge.setText("🚨 Reported");
                emergencyBadge.setVisibility(View.VISIBLE);
            } else {
                emergencyBadge.setVisibility(View.GONE);
            }
        }
    }
}