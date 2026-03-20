package com.example.trackfit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ActivityTrackingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ChipGroup chipGroupActivity;
    private EditText etWeight, etHours, etMinutes;
    private Button btnCalculateCalories;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // MET values
    private final Map<String, Double> metValues = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_tracking);

        // Initialize MET values
        metValues.put("Walking", 3.5);
        metValues.put("Running", 7.0);
        metValues.put("Cycling", 6.0);
        metValues.put("Swimming", 8.0);
        metValues.put("Hiking", 6.0);
        metValues.put("Football", 7.0);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI
        btnBack = findViewById(R.id.btnBack);
        chipGroupActivity = findViewById(R.id.chipGroupActivity);
        etWeight = findViewById(R.id.etWeight);
        etHours = findViewById(R.id.etHours);
        etMinutes = findViewById(R.id.etMinutes);
        btnCalculateCalories = findViewById(R.id.btnCalculateCalories);

        btnBack.setOnClickListener(v -> finish());

        btnCalculateCalories.setOnClickListener(v -> calculateAndSave());
    }

    private void calculateAndSave() {
        // Validation: Check if activity is selected
        int selectedChipId = chipGroupActivity.getCheckedChipId();
        if (selectedChipId == -1) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip selectedChip = findViewById(selectedChipId);
        String activityName = selectedChip.getText().toString();

        // Validation: Weight
        String weightStr = etWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr);
            if (weight <= 0) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation: Duration
        String hoursStr = etHours.getText().toString().trim();
        String minutesStr = etMinutes.getText().toString().trim();

        int hours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);
        int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);

        int totalMinutes = (hours * 60) + minutes;

        if (totalMinutes <= 0) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculation
        Double metValue = metValues.get(activityName);
        if (metValue == null) metValue = 1.0; // Fallback, though shouldn't happen

        double caloriesRaw = (metValue * weight * totalMinutes) / 60.0;
        long caloriesBurned = Math.round(caloriesRaw);

        // Show toast
        Toast.makeText(this, caloriesBurned + " Calories Burned", Toast.LENGTH_LONG).show();

        // Firebase saving
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        
        Date now = new Date();
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Map<String, Object> activityLog = new HashMap<>();
        activityLog.put("user_id", userId);
        activityLog.put("activity_name", activityName);
        activityLog.put("weight", weight);
        activityLog.put("duration_minutes", totalMinutes);
        activityLog.put("calories_burned", caloriesBurned);
        activityLog.put("date", dfDate.format(now));
        activityLog.put("time", dfTime.format(now));
        activityLog.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("activity_logs")
                .add(activityLog)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(ActivityTrackingActivity.this, "Activity saved successfully", Toast.LENGTH_SHORT).show();
                    clearInputs();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ActivityTrackingActivity.this, "Failed to save activity", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearInputs() {
        chipGroupActivity.clearCheck();
        etWeight.setText("");
        etHours.setText("");
        etMinutes.setText("");
    }
}
