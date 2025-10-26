package com.example.answer;

public class HistoryItem {
    public String smsReport;
    public String recognizedText;
    public String chatGPTResponse;
    public int sessionDuration;
    public long timestamp;
    public boolean reportedTo911;



    public HistoryItem() {} 

    public String getDateFormatted() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
}
