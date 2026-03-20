package com.example.trackfit;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class GoalsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    private TextView tvEmptyGoals;
    private TextView tvGoalSteps, tvGoalSleep, tvGoalWater, tvGoalFood, tvGoalWeight, tvGoalActivity;

    // Current Values
    private Integer currentSteps = null;
    private Integer currentSleepHours = null;
    private Integer currentSleepMins = null;
    private Integer currentWaterMl = null;
    private Integer currentFoodKcal = null;
    private Float currentWeight = null;
    private Integer currentActivityKcal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvEmptyGoals = findViewById(R.id.tvEmptyGoals);

        tvGoalSteps = findViewById(R.id.tvGoalSteps);
        tvGoalSleep = findViewById(R.id.tvGoalSleep);
        tvGoalWater = findViewById(R.id.tvGoalWater);
        tvGoalFood = findViewById(R.id.tvGoalFood);
        tvGoalWeight = findViewById(R.id.tvGoalWeight);
        tvGoalActivity = findViewById(R.id.tvGoalActivity);

        LinearLayout cardSteps = findViewById(R.id.cardGoalSteps);
        LinearLayout cardSleep = findViewById(R.id.cardGoalSleep);
        LinearLayout cardWater = findViewById(R.id.cardGoalWater);
        LinearLayout cardFood = findViewById(R.id.cardGoalFood);
        LinearLayout cardWeight = findViewById(R.id.cardGoalWeight);
        LinearLayout cardActivity = findViewById(R.id.cardGoalActivity);

        cardSteps.setOnClickListener(v -> showSingleInputDialog("Daily Steps Goal", "Enter daily step goal", currentSteps, "steps_goal"));
        cardFood.setOnClickListener(v -> showSingleInputDialog("Daily Calorie Intake Goal", "e.g. 2000", currentFoodKcal, "daily_calorie_intake_goal"));
        cardWeight.setOnClickListener(v -> showSingleInputDialog("Target Weight", "e.g. 70(kg)", currentWeight == null ? null : currentWeight.intValue(), "target_weight"));
        cardActivity.setOnClickListener(v -> showSingleInputDialog("Daily Activity Goal", "e.g. 500(kcal)", currentActivityKcal, "daily_activity_calorie_goal"));

        cardSleep.setOnClickListener(v -> showDoubleInputDialog(
                "Sleep Goal", "Hours", "Minutes",
                currentSleepHours, currentSleepMins,
                "sleep_goal_hours", "sleep_goal_minutes"
        ));

        cardWater.setOnClickListener(v -> {
            Integer liters = currentWaterMl == null ? null : currentWaterMl / 1000;
            Integer mls = currentWaterMl == null ? null : currentWaterMl % 1000;
            showDoubleInputDialog(
                    "Daily Water Goal", "Liters", "Milliliters",
                    liters, mls,
                    "water_goal_liters", "water_goal_ml_remainder" // Internal mock keys
            );
        });

        fetchGoals();
    }

    private void fetchGoals() {
        if (userId == null) return;

        db.collection("daily_goals").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvEmptyGoals.setVisibility(View.GONE);
                        updateLocalValues(documentSnapshot);
                        updateUI();
                    } else {
                        tvEmptyGoals.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load goals", Toast.LENGTH_SHORT).show());
    }

    private void updateLocalValues(DocumentSnapshot doc) {
        currentSteps = doc.contains("steps_goal") ? doc.getLong("steps_goal").intValue() : null;
        currentSleepHours = doc.contains("sleep_goal_hours") ? doc.getLong("sleep_goal_hours").intValue() : null;
        currentSleepMins = doc.contains("sleep_goal_minutes") ? doc.getLong("sleep_goal_minutes").intValue() : null;
        currentWaterMl = doc.contains("water_goal_ml") ? doc.getLong("water_goal_ml").intValue() : null;
        currentFoodKcal = doc.contains("daily_calorie_intake_goal") ? doc.getLong("daily_calorie_intake_goal").intValue() : null;
        
        if (doc.contains("target_weight")) {
            Object w = doc.get("target_weight");
            if (w instanceof Double) currentWeight = ((Double) w).floatValue();
            else if (w instanceof Long) currentWeight = ((Long) w).floatValue();
        } else {
            currentWeight = null;
        }

        currentActivityKcal = doc.contains("daily_activity_calorie_goal") ? doc.getLong("daily_activity_calorie_goal").intValue() : null;
    }

    private void updateUI() {
        tvGoalSteps.setText(currentSteps != null ? currentSteps + " steps" : "Set Goal");
        
        if (currentSleepHours != null && currentSleepMins != null) {
            tvGoalSleep.setText(currentSleepHours + "h " + currentSleepMins + "m");
        } else {
            tvGoalSleep.setText("Set Goal");
        }
        
        tvGoalWater.setText(currentWaterMl != null ? currentWaterMl + " ml" : "Set Goal");
        tvGoalFood.setText(currentFoodKcal != null ? currentFoodKcal + " kcal" : "Set Goal");
        tvGoalWeight.setText(currentWeight != null ? currentWeight + " kg" : "Set Goal");
        tvGoalActivity.setText(currentActivityKcal != null ? currentActivityKcal + " kcal" : "Set Goal");
    }

    private void showSingleInputDialog(String title, String hint, Integer currentValue, String dbKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_goal_single_input, null);
        builder.setView(view);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        EditText etInput = view.findViewById(R.id.etGoalInput);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        tvTitle.setText(title);
        etInput.setHint(hint);
        if (currentValue != null) {
            etInput.setText(String.valueOf(currentValue));
        }

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String valStr = etInput.getText().toString().trim();
            if (valStr.isEmpty()) {
                Toast.makeText(this, "Please enter a valid value", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                if (dbKey.equals("target_weight")) {
                    float val = Float.parseFloat(valStr);
                    saveGoal(dbKey, val);
                } else {
                    int val = Integer.parseInt(valStr);
                    saveGoal(dbKey, val);
                }
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showDoubleInputDialog(String title, String label1, String label2, Integer val1, Integer val2, String key1, String key2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_goal_double_input, null);
        builder.setView(view);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvL1 = view.findViewById(R.id.tvLabel1);
        TextView tvL2 = view.findViewById(R.id.tvLabel2);
        EditText et1 = view.findViewById(R.id.etInput1);
        EditText et2 = view.findViewById(R.id.etInput2);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        tvTitle.setText(title);
        tvL1.setText(label1);
        tvL2.setText(label2);
        
        if (val1 != null) et1.setText(String.valueOf(val1));
        if (val2 != null) et2.setText(String.valueOf(val2));

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String s1 = et1.getText().toString().trim();
            String s2 = et2.getText().toString().trim();

            if (s1.isEmpty() && s2.isEmpty()) {
                Toast.makeText(this, "Please enter a valid value", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int v1 = s1.isEmpty() ? 0 : Integer.parseInt(s1);
                int v2 = s2.isEmpty() ? 0 : Integer.parseInt(s2);

                if (title.contains("Water")) {
                    // Custom logic for water
                    int totalMl = (v1 * 1000) + v2;
                    saveGoal("water_goal_ml", totalMl);
                } else {
                    // Sleep
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(key1, v1);
                    updates.put(key2, v2);
                    saveMultipleGoals(updates);
                }
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void saveGoal(String key, Object value) {
        Map<String, Object> update = new HashMap<>();
        update.put(key, value);
        saveMultipleGoals(update);
    }

    private void saveMultipleGoals(Map<String, Object> updates) {
        if (userId == null) return;
        
        updates.put("updated_at", com.google.firebase.Timestamp.now());
        updates.put("user_id", userId);

        DocumentReference docRef = db.collection("daily_goals").document(userId);

        docRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Goal saved", Toast.LENGTH_SHORT).show();
                    // Just re-fetch to ensure local sync
                    fetchGoals();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save goal", Toast.LENGTH_SHORT).show());
    }
}
