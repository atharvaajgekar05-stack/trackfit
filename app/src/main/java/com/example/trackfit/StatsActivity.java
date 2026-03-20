package com.example.trackfit;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    public static final String EXTRA_METRIC_TYPE = "EXTRA_METRIC_TYPE";

    private String metricType = "Steps"; // "Steps", "Water", "Sleep", "Weight", "Food", "Activity"
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvTitle;
    private ImageView ivBack;
    private TabLayout tabLayout;
    private LineChart lineChart;
    private RecyclerView rvHistory;

    private HistoryAdapter historyAdapter;
    private List<HistoryLog> allRawLogs = new ArrayList<>();

    // Aggregation modes
    private enum AggregationMode { DAY, WEEKLY, MONTHLY, YEARLY }
    private AggregationMode currentMode = AggregationMode.DAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stats);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contentRoot), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, sys.top, 0, sys.bottom);
            return insets;
        });

        if (getIntent().hasExtra(EXTRA_METRIC_TYPE)) {
            metricType = getIntent().getStringExtra(EXTRA_METRIC_TYPE);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tvTitle);
        ivBack = findViewById(R.id.ivBack);
        tabLayout = findViewById(R.id.tabLayout);
        lineChart = findViewById(R.id.lineChart);
        rvHistory = findViewById(R.id.rvHistory);

        tvTitle.setText(metricType + " Stats");
        ivBack.setOnClickListener(v -> finish());

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter();
        rvHistory.setAdapter(historyAdapter);

        setupChart();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentMode = AggregationMode.DAY; break;
                    case 1: currentMode = AggregationMode.WEEKLY; break;
                    case 2: currentMode = AggregationMode.MONTHLY; break;
                    case 3: currentMode = AggregationMode.YEARLY; break;
                }
                updateChartData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fetchData();
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setExtraBottomOffset(10f);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#888888"));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#888888"));
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f); // default

        lineChart.getAxisRight().setEnabled(false);
    }

    private void fetchData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        allRawLogs.clear();

        // Determine collection path and fields based on metric length
        String collectionPath;
        String valueField;
        String unitName;
        boolean isAveragedOverDay = false;

        switch (metricType) {
            case "Steps":
                // Special case for manual steps + live steps
                db.collection("manual_step_logs")
                        .whereEqualTo("user_id", uid)
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                if (doc.contains("estimated_steps") && doc.contains("date")) {
                                    long steps = doc.getLong("estimated_steps");
                                    String dateStr = doc.getString("date"); // "yyyy-MM-dd"
                                    String timeStr = doc.contains("time") ? doc.getString("time") : "00:00";
                                    long ts = parseTimestamp(dateStr, timeStr);
                                    
                                    allRawLogs.add(new HistoryLog(formatDisplayTime(dateStr, timeStr), steps + " steps", ts));
                                    rawRecords.add(new RawDataRecord(ts, steps, formatDisplayTime(dateStr, timeStr), steps + " steps"));
                                }
                            }
                            
                            db.collection("step_sessions")
                                    .whereEqualTo("user_id", uid)
                                    .get()
                                    .addOnSuccessListener(snapshots2 -> {
                                        for (QueryDocumentSnapshot doc : snapshots2) {
                                            if (doc.contains("steps") && doc.contains("date")) {
                                                long steps = doc.getLong("steps");
                                                String dateStr = doc.getString("date");
                                                String timeStr = doc.contains("time") ? doc.getString("time") : "00:00";
                                                long ts = parseTimestamp(dateStr, timeStr);
                                                
                                                allRawLogs.add(new HistoryLog(formatDisplayTime(dateStr, timeStr), steps + " steps", ts));
                                                rawRecords.add(new RawDataRecord(ts, steps, formatDisplayTime(dateStr, timeStr), steps + " steps"));
                                            }
                                        }
                                        processFetchedData();
                                    }).addOnFailureListener(e -> processFetchedData());
                        }).addOnFailureListener(e -> {
                            db.collection("step_sessions")
                                    .whereEqualTo("user_id", uid)
                                    .get()
                                    .addOnSuccessListener(snapshots2 -> {
                                        for (QueryDocumentSnapshot doc : snapshots2) {
                                            if (doc.contains("steps") && doc.contains("date")) {
                                                long steps = doc.getLong("steps");
                                                String dateStr = doc.getString("date");
                                                String timeStr = doc.contains("time") ? doc.getString("time") : "00:00";
                                                long ts = parseTimestamp(dateStr, timeStr);
                                                
                                                allRawLogs.add(new HistoryLog(formatDisplayTime(dateStr, timeStr), steps + " steps", ts));
                                                rawRecords.add(new RawDataRecord(ts, steps, formatDisplayTime(dateStr, timeStr), steps + " steps"));
                                            }
                                        }
                                        processFetchedData();
                                    }).addOnFailureListener(e2 -> processFetchedData());
                        });
                return;
            case "Water":
                collectionPath = "water_logs";
                valueField = "total_water_ml";
                unitName = "ml";
                break;
            case "Sleep":
                collectionPath = "sleep_logs";
                valueField = "duration_minutes";
                unitName = "m"; // Special format handled later
                break;
            case "Weight":
                collectionPath = "weight_logs";
                valueField = "weight_kg";
                unitName = "kg";
                isAveragedOverDay = true;
                break;
            case "Food":
            case "Calories":
                collectionPath = "food_logs";
                valueField = "calories_logged";
                unitName = "kcal";
                break;
            case "Activity":
                collectionPath = "activity_logs";
                valueField = "calories_burned";
                unitName = "kcal";
                break;
            default:
                return;
        }

        final String finalUnit = unitName;

        db.collection(collectionPath)
                .whereEqualTo("user_id", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (doc.contains(valueField) && doc.contains("date")) {
                            float val = ((Number) doc.get(valueField)).floatValue();
                            String dateStr = doc.getString("date");
                            String timeStr = doc.contains("time") ? doc.getString("time") : "00:00";
                            long ts = parseTimestamp(dateStr, timeStr);
                            
                            String displayVal = (metricType.equals("Sleep")) ? formatSleepMins((int)val) : (val + " " + finalUnit);
                            if (metricType.equals("Weight")) {
                                displayVal = String.format(Locale.US, "%.1f kg", val);
                            } else if (metricType.equals("Steps") || metricType.equals("Water") || metricType.equals("Food") || metricType.equals("Activity") || metricType.equals("Calories")) {
                                displayVal = ((int)val) + " " + finalUnit;
                            }

                            RawDataRecord record = new RawDataRecord(ts, val, formatDisplayTime(dateStr, timeStr), displayVal);
                            rawRecords.add(record);
                        }
                    }
                    processFetchedData();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load history.", Toast.LENGTH_SHORT).show());
    }

    // Extended structure to hold numeric raw val for charting
    private List<RawDataRecord> rawRecords = new ArrayList<>();

    private static class RawDataRecord {
        long timestamp;
        float value;
        String displayTime;
        String displayValue;

        RawDataRecord(long timestamp, float value, String displayTime, String displayValue) {
            this.timestamp = Math.max(0, timestamp);
            this.value = value;
            this.displayTime = displayTime;
            this.displayValue = displayValue;
        }
    }

    private void processFetchedData() {
        // Build HistoryLogs from rawRecords, but group them by day
        allRawLogs.clear();
        
        // Map to hold aggregated data per date string (e.g. "2023-10-27")
        Map<String, AggregatedDay> dayMap = new HashMap<>();

        for (RawDataRecord r : rawRecords) {
            String dateKey = getDateKeyFromTimestamp(r.timestamp);
            if (!dayMap.containsKey(dateKey)) {
                dayMap.put(dateKey, new AggregatedDay(dateKey, r.timestamp));
            }
            AggregatedDay ad = dayMap.get(dateKey);
            ad.valSum += r.value;
            ad.count++;
            ad.latestTimestamp = Math.max(ad.latestTimestamp, r.timestamp);
        }

        // Convert aggregated map back to HistoryLogs
        for (AggregatedDay ad : dayMap.values()) {
            float finalVal = metricType.equals("Weight") && ad.count > 0 ? (ad.valSum / ad.count) : ad.valSum;
            
            String unit = getUnitForMetric();
            String displayVal;
            if (metricType.equals("Sleep")) {
                displayVal = formatSleepMins((int)finalVal);
            } else if (metricType.equals("Weight")) {
                displayVal = String.format(Locale.US, "%.1f %s", finalVal, unit);
            } else {
                displayVal = ((int)finalVal) + " " + unit;
            }

            allRawLogs.add(new HistoryLog(formatDisplayDateKey(ad.dateKey), displayVal, ad.latestTimestamp));
        }

        // Sort descending (newest first)
        Collections.sort(allRawLogs, (a, b) -> Long.compare(b.getTimestampInMillis(), a.getTimestampInMillis()));
        historyAdapter.setLogs(allRawLogs);

        // Update Chart
        updateChartData();
    }
    
    private String getUnitForMetric() {
        switch (metricType) {
            case "Water": return "ml";
            case "Weight": return "kg";
            case "Food": case "Calories": case "Activity": return "kcal";
            case "Steps": return "steps";
            default: return "";
        }
    }
    
    private static class AggregatedDay {
        String dateKey;
        long latestTimestamp;
        float valSum = 0f;
        int count = 0;
        
        AggregatedDay(String dateKey, long initialTs) {
            this.dateKey = dateKey;
            this.latestTimestamp = initialTs;
        }
    }

    private String getDateKeyFromTimestamp(long ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date(ts));
    }

    private String formatDisplayDateKey(String dateKey) {
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdfInput.parse(dateKey);
            if (date == null) return dateKey;

            Calendar calDoc = Calendar.getInstance();
            calDoc.setTime(date);

            Calendar calNow = Calendar.getInstance();

            if (calDoc.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDoc.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)) {
                return "Today";
            }
            
            calNow.add(Calendar.DAY_OF_YEAR, -1);
            if (calDoc.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDoc.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)) {
                return "Yesterday";
            }

            SimpleDateFormat out = new SimpleDateFormat("d MMM yyyy", Locale.US);
            return out.format(date);
        } catch (Exception e) {
            return dateKey;
        }
    }

    private void updateChartData() {
        if (rawRecords.isEmpty()) {
            lineChart.clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        
        Calendar cal = Calendar.getInstance();

        if (currentMode == AggregationMode.DAY) {
            // Group by Day of Current Week (Sun - Sat)
            int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
            int currentYear = cal.get(Calendar.YEAR);
            
            String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            float[] sums = new float[7];
            int[] counts = new int[7];

            for (RawDataRecord r : rawRecords) {
                cal.setTimeInMillis(r.timestamp);
                if (cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.YEAR) == currentYear) {
                    int dayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun, 6=Sat
                    sums[dayIdx] += r.value;
                    counts[dayIdx]++;
                }
            }
            
            for (int i=0; i<7; i++) {
                xLabels.add(days[i]);
                float val = metricType.equals("Weight") && counts[i] > 0 ? (sums[i] / counts[i]) : sums[i];
                entries.add(new Entry(i, val));
            }
            
        } else if (currentMode == AggregationMode.WEEKLY) {
            // Group by Weeks in Current Month
            int currentMonth = cal.get(Calendar.MONTH);
            int currentYear = cal.get(Calendar.YEAR);
            
            float[] sums = new float[4];
            int[] rawCounts = new int[4];
            java.util.Set<String>[] loggedDays = new java.util.HashSet[4];
            for(int i=0; i<4; i++) loggedDays[i] = new java.util.HashSet<>();
            
            for (RawDataRecord r : rawRecords) {
                cal.setTimeInMillis(r.timestamp);
                if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                    int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                    int weekIdx = (dayOfMonth - 1) / 7;
                    if (weekIdx > 3) weekIdx = 3; // Clamp remaining days to week 4
                    sums[weekIdx] += r.value;
                    rawCounts[weekIdx]++;
                    loggedDays[weekIdx].add(cal.get(Calendar.DAY_OF_YEAR) + "-" + cal.get(Calendar.YEAR));
                }
            }
            
            for (int i=0; i<4; i++) {
                xLabels.add("W" + (i+1));
                float val;
                if (metricType.equals("Weight")) {
                    val = rawCounts[i] > 0 ? (sums[i] / rawCounts[i]) : 0f;
                } else {
                    int daysCount = loggedDays[i].size();
                    val = daysCount > 0 ? (sums[i] / daysCount) : 0f;
                }
                entries.add(new Entry(i, val));
            }

        } else if (currentMode == AggregationMode.MONTHLY) {
            // Group by Month of Current Year
            int currentYear = cal.get(Calendar.YEAR);
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            
            float[] sums = new float[12];
            int[] rawCounts = new int[12];
            java.util.Set<String>[] loggedWeeks = new java.util.HashSet[12];
            for(int i=0; i<12; i++) loggedWeeks[i] = new java.util.HashSet<>();

            for (RawDataRecord r : rawRecords) {
                cal.setTimeInMillis(r.timestamp);
                if (cal.get(Calendar.YEAR) == currentYear) {
                    int monthIdx = cal.get(Calendar.MONTH);
                    sums[monthIdx] += r.value;
                    rawCounts[monthIdx]++;
                    loggedWeeks[monthIdx].add(cal.get(Calendar.WEEK_OF_YEAR) + "-" + cal.get(Calendar.YEAR));
                }
            }

            for (int i=0; i<12; i++) {
                xLabels.add(months[i]);
                float val;
                if (metricType.equals("Weight")) {
                    val = rawCounts[i] > 0 ? (sums[i] / rawCounts[i]) : 0f;
                } else {
                    int weeksCount = loggedWeeks[i].size();
                    val = weeksCount > 0 ? (sums[i] / weeksCount) : 0f;
                }
                entries.add(new Entry(i, val));
            }

        } else if (currentMode == AggregationMode.YEARLY) {
            // Last 5 Years
            int currentYear = cal.get(Calendar.YEAR);
            int startYear = currentYear - 4;
            
            float[] sums = new float[5];
            int[] rawCounts = new int[5];
            java.util.Set<String>[] loggedMonths = new java.util.HashSet[5];
            for(int i=0; i<5; i++) loggedMonths[i] = new java.util.HashSet<>();

            for (RawDataRecord r : rawRecords) {
                cal.setTimeInMillis(r.timestamp);
                int y = cal.get(Calendar.YEAR);
                if (y >= startYear && y <= currentYear) {
                    int idx = y - startYear;
                    sums[idx] += r.value;
                    rawCounts[idx]++;
                    loggedMonths[idx].add(cal.get(Calendar.MONTH) + "-" + y);
                }
            }

            for (int i=0; i<5; i++) {
                xLabels.add(String.valueOf(startYear + i));
                float val;
                if (metricType.equals("Weight")) {
                    val = rawCounts[i] > 0 ? (sums[i] / rawCounts[i]) : 0f;
                } else {
                    int monthsCount = loggedMonths[i].size();
                    val = monthsCount > 0 ? (sums[i] / monthsCount) : 0f;
                }
                entries.add(new Entry(i, val));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, metricType);
        dataSet.setColor(Color.parseColor("#39cccc"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#39cccc"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.parseColor("#FFFFFF"));
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth lines

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        lineChart.invalidate(); // refresh
    }

    private long parseTimestamp(String dateStr, String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date date = sdf.parse(dateStr + " " + timeStr);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDisplayTime(String dateStr, String timeStr) {
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdfInput.parse(dateStr);
            if (date == null) return dateStr + " " + timeStr;

            Calendar calDoc = Calendar.getInstance();
            calDoc.setTime(date);

            Calendar calNow = Calendar.getInstance();

            if (calDoc.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDoc.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)) {
                return "Today " + timeStr;
            }
            
            calNow.add(Calendar.DAY_OF_YEAR, -1);
            if (calDoc.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDoc.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)) {
                return "Yesterday " + timeStr;
            }

            SimpleDateFormat out = new SimpleDateFormat("d MMM yyyy", Locale.US);
            return out.format(date) + " " + timeStr;
        } catch (Exception e) {
            return dateStr + " " + timeStr;
        }
    }

    private String formatSleepMins(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        return h + "h " + m + "m";
    }
}
