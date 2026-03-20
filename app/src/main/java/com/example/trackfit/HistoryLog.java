package com.example.trackfit;

public class HistoryLog {
    private String displayTime;
    private String displayValue;
    private long timestampInMillis;

    public HistoryLog(String displayTime, String displayValue, long timestampInMillis) {
        this.displayTime = displayTime;
        this.displayValue = displayValue;
        this.timestampInMillis = Math.max(0, timestampInMillis); // Prevent negative sorts
    }

    public String getDisplayTime() { return displayTime; }
    public String getDisplayValue() { return displayValue; }
    public long getTimestampInMillis() { return timestampInMillis; }
}
