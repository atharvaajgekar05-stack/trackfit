package com.example.trackfit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private FloatingActionButton fab;
    private LinearLayout navHome, navGoals, navProfile;
    private SwipeRefreshLayout swipeRefresh;

    // Running totals for the big calorie ring (food calories consumed)
    private int latestFoodCalories = 0;
    private int latestFoodGoal = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // ── Handle system bar insets ──────────────────────────────────────
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.contentRoot), (v, insets) -> {
                    Insets sys = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    // Push scroll content below the status bar
                    v.setPadding(0, sys.top, 0, 0);
                    return insets;
                });

        // Bottom nav must sit above the system gesture bar
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.bottomNav), (v, insets) -> {
                    Insets sys = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    int navBarH = sys.bottom;
                    float density = getResources().getDisplayMetrics().density;
                    int tabBarDp = (int)(72 * density);

                    // Expand bottomNav to cover gesture bar
                    v.setPadding(0, 0, 0, navBarH);
                    v.getLayoutParams().height = tabBarDp + navBarH;
                    v.requestLayout();

                    // Also expand swipeRefresh bottom margin so content isn't hidden
                    android.view.ViewGroup.MarginLayoutParams lp =
                            (android.view.ViewGroup.MarginLayoutParams)
                                    findViewById(R.id.swipeRefresh).getLayoutParams();
                    lp.bottomMargin = tabBarDp + navBarH;
                    findViewById(R.id.swipeRefresh).setLayoutParams(lp);

                    return insets;
                });

        // ScrollView bottom margin must also account for nav bar height
        // (done via layout_marginBottom="72dp" in XML + gesture bar adds to bottomNav)

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Bind views
        fab        = findViewById(R.id.fab);
        navHome    = findViewById(R.id.navHome);
        navGoals   = findViewById(R.id.navGoals);
        navProfile = findViewById(R.id.navProfile);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        swipeRefresh.setColorSchemeColors(android.graphics.Color.parseColor("#39cccc"));

        swipeRefresh.setOnRefreshListener(() -> {
            loadDashboardData();
        });

        fab.setOnClickListener(v -> {
            FabMenuBottomSheet bottomSheet = new FabMenuBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "FAB_MENU");
        });

        navHome.setOnClickListener(v -> { /* Already home */ });

        navGoals.setOnClickListener(v ->
                startActivity(new Intent(this, GoalsActivity.class)));

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
                
        // Launch Stats on Card Clicks
        findViewById(R.id.cardSteps).setOnClickListener(v -> openStats("Steps"));
        findViewById(R.id.cardSleep).setOnClickListener(v -> openStats("Sleep"));
        findViewById(R.id.cardWater).setOnClickListener(v -> openStats("Water"));
        findViewById(R.id.cardFood).setOnClickListener(v -> openStats("Food"));
        findViewById(R.id.cardWeight).setOnClickListener(v -> openStats("Weight"));
        findViewById(R.id.cardCaloriesBurned).setOnClickListener(v -> openStats("Activity"));

        // ── One-time demo data seeding ─────────────────────────────────
        SharedPreferences prefs = getSharedPreferences("trackfit_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("demo_data_seeded", false)) {
            String uid = mAuth.getUid();
            if (uid != null) {
                DemoDataSeeder.seedAll(uid);
                prefs.edit().putBoolean("demo_data_seeded", true).apply();
            }
        }

        loadDashboardData();
    }
    
    private void openStats(String metricType) {
        Intent intent = new Intent(this, StatsActivity.class);
        intent.putExtra(StatsActivity.EXTRA_METRIC_TYPE, metricType);
        startActivity(intent);
    }

    private void loadDashboardData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("daily_goals").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        int stepsGoal = snapshot.contains("steps_goal") ? snapshot.getLong("steps_goal").intValue() : 10000;
                        int waterGoal = snapshot.contains("water_goal_ml") ? snapshot.getLong("water_goal_ml").intValue() : 2000;
                        int foodGoal = snapshot.contains("daily_calorie_intake_goal") ? snapshot.getLong("daily_calorie_intake_goal").intValue() : 2000;
                        float weightGoal = snapshot.contains("target_weight") ? ((Number)snapshot.get("target_weight")).floatValue() : 70.0f;
                        int activityGoal = snapshot.contains("daily_activity_calorie_goal") ? snapshot.getLong("daily_activity_calorie_goal").intValue() : 500;
                        int sleepGoalMins = 480;
                        if (snapshot.contains("sleep_goal_hours") && snapshot.contains("sleep_goal_minutes")) {
                            sleepGoalMins = (snapshot.getLong("sleep_goal_hours").intValue() * 60) + snapshot.getLong("sleep_goal_minutes").intValue();
                        }

                        fetchAndBindDailyActuals(uid, stepsGoal, waterGoal, foodGoal, weightGoal, sleepGoalMins, activityGoal);
                    } else {
                         // Default goals fallback
                         fetchAndBindDailyActuals(uid, 10000, 2000, 2000, 70.0f, 480, 500);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void fetchAndBindDailyActuals(String uid, int stepsGoal, int waterGoal, int foodGoal, float weightGoal, int sleepGoalMins, int activityGoal) {
        String todayDateString = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

        // 1. Water
        db.collection("water_logs")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("date", todayDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int currentWater = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("total_water_ml")) {
                            currentWater += doc.getLong("total_water_ml").intValue();
                        }
                    }
                    updateWaterUI(currentWater, waterGoal);
                });

        // 2. Food (Calories Consumed)
        db.collection("food_logs")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("date", todayDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int currentFood = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("calories_logged")) {
                            currentFood += doc.getLong("calories_logged").intValue();
                        }
                    }
                    updateFoodUI(currentFood, foodGoal);
                });

        // 3. Activity (Calories Burned)
        db.collection("activity_logs")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("date", todayDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int currentActivity = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("calories_burned")) {
                            currentActivity += doc.getLong("calories_burned").intValue();
                        }
                    }
                    updateActivityUI(currentActivity, activityGoal);
                });

        // 4. Steps (Manual + Live)
        final int[] totalSteps = {0};
        final int[] stepCallbacks = {0};

        db.collection("manual_step_logs")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("date", todayDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("estimated_steps")) {
                            totalSteps[0] += doc.getLong("estimated_steps").intValue();
                        }
                    }
                    stepCallbacks[0]++;
                    if (stepCallbacks[0] == 2) updateStepsUI(totalSteps[0], stepsGoal);
                }).addOnFailureListener(e -> {
                    stepCallbacks[0]++;
                    if (stepCallbacks[0] == 2) updateStepsUI(totalSteps[0], stepsGoal);
                });

        db.collection("step_sessions")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("date", todayDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("steps")) {
                            totalSteps[0] += doc.getLong("steps").intValue();
                        }
                    }
                    stepCallbacks[0]++;
                    if (stepCallbacks[0] == 2) updateStepsUI(totalSteps[0], stepsGoal);
                }).addOnFailureListener(e -> {
                    stepCallbacks[0]++;
                    if (stepCallbacks[0] == 2) updateStepsUI(totalSteps[0], stepsGoal);
                });

        // 5. Sleep (assuming a sleep_logs subcollection exists)
        db.collection("sleep_logs")
                 .whereEqualTo("user_id", uid)
                 .whereEqualTo("date", todayDateString)
                 .get()
                 .addOnSuccessListener(queryDocumentSnapshots -> {
                     int currentSleepMins = 0;
                     for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                         if (doc.contains("duration_minutes")) {
                             currentSleepMins += doc.getLong("duration_minutes").intValue();
                         }
                     }
                     updateSleepUI(currentSleepMins, sleepGoalMins);
                 }).addOnFailureListener(e -> updateSleepUI(0, sleepGoalMins));

        // 6. Weight (Assuming weight_logs subcollection exists)
        db.collection("weight_logs")
                  .whereEqualTo("user_id", uid)
                  .whereEqualTo("date", todayDateString)
                  .get()
                  .addOnSuccessListener(queryDocumentSnapshots -> {
                      float weightSum = 0;
                      int weightCount = 0;
                      for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                          if (doc.contains("weight_kg")) {
                              weightSum += ((Number)doc.get("weight_kg")).floatValue();
                              weightCount++;
                          }
                      }
                      float currentWeight = weightCount > 0 ? (weightSum / weightCount) : 0f;
                      updateWeightUI(currentWeight, weightGoal);
                      checkStopRefreshing();
                  }).addOnFailureListener(e -> {
                      updateWeightUI(0f, weightGoal);
                      checkStopRefreshing();
                  });

    }
    
    private void checkStopRefreshing() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void updateStepsUI(int current, int goal) {
        TextView tv = findViewById(R.id.tvStepsValue);
        TextView tvComp = findViewById(R.id.tvStepsCompleted);
        android.widget.ProgressBar pb = findViewById(R.id.pbSteps);
        CircularProgressView cpv = findViewById(R.id.cpvStepsSummary);
        TextView tvSummary = findViewById(R.id.tvStepsSummary);
        if (tv != null) {
            tv.setText(current + " / " + goal + " steps");
            tvComp.setVisibility(current >= goal ? android.view.View.VISIBLE : android.view.View.GONE);
            pb.setMax(goal);
            pb.setProgress(current);
            if (cpv != null) {
                cpv.setMaxProgress(goal);
                cpv.setProgress(current);
            }
            if (tvSummary != null) {
                tvSummary.setText(String.valueOf(current));
            }
        }
    }

    private void updateWaterUI(int current, int goal) {
         TextView tv = findViewById(R.id.tvWaterValue);
         TextView tvComp = findViewById(R.id.tvWaterCompleted);
         android.widget.ProgressBar pb = findViewById(R.id.pbWater);
         CircularProgressView cpv = findViewById(R.id.cpvWaterSummary);
         TextView tvSummary = findViewById(R.id.tvWaterSummary);
         if (tv != null) {
             tv.setText(current + " / " + goal + " ml");
             tvComp.setVisibility(current >= goal ? android.view.View.VISIBLE : android.view.View.GONE);
             pb.setMax(goal);
             pb.setProgress(current);
             if (cpv != null) {
                 cpv.setMaxProgress(goal);
                 cpv.setProgress(current);
             }
             if (tvSummary != null) {
                 float liters = current / 1000f;
                 tvSummary.setText(String.format(java.util.Locale.US, "%.1fL", liters));
             }
         }
    }

    private void updateFoodUI(int current, int goal) {
         TextView tv = findViewById(R.id.tvFoodValue);
         TextView tvComp = findViewById(R.id.tvFoodCompleted);
         android.widget.ProgressBar pb = findViewById(R.id.pbFood);
         CircularProgressView cpv = findViewById(R.id.cpvCalories);
         TextView tvCalCenter = findViewById(R.id.tvCaloriesValue);
         if (tv != null) {
             tv.setText(current + " / " + goal + " kcal");
             tvComp.setVisibility(current >= goal ? android.view.View.VISIBLE : android.view.View.GONE);
             pb.setMax(goal);
             pb.setProgress(current);
             if (cpv != null) {
                 cpv.setMaxProgress(goal);
                 cpv.setProgress(current);
             }
             // Update the big calorie ring center text
             if (tvCalCenter != null) {
                 tvCalCenter.setText(String.valueOf(current));
             }
         }
    }

    private void updateActivityUI(int current, int goal) {
         TextView tv = findViewById(R.id.tvCaloriesBurnedValue);
         TextView tvComp = findViewById(R.id.tvCaloriesBurnedCompleted);
         android.widget.ProgressBar pb = findViewById(R.id.pbCaloriesBurned);
         if (tv != null) {
             tv.setText(current + " / " + goal + " kcal");
             tvComp.setVisibility(current >= goal ? android.view.View.VISIBLE : android.view.View.GONE);
             pb.setMax(goal);
             pb.setProgress(current);
         }
    }

    private void updateSleepUI(int currentMins, int goalMins) {
         TextView tv = findViewById(R.id.tvSleepValue);
         TextView tvComp = findViewById(R.id.tvSleepCompleted);
         android.widget.ProgressBar pb = findViewById(R.id.pbSleep);
         if (tv != null) {
             int hrsC = currentMins / 60;
             int minsC = currentMins % 60;
             int hrsG = goalMins / 60;
             int minsG = goalMins % 60;
             tv.setText(hrsC + "h " + minsC + "m / " + hrsG + "h " + minsG + "m");
             tvComp.setVisibility(currentMins >= goalMins ? android.view.View.VISIBLE : android.view.View.GONE);
             pb.setMax(goalMins);
             pb.setProgress(currentMins);
         }
    }

    private void updateWeightUI(float current, float goal) {
         TextView tv = findViewById(R.id.tvWeightValue);
         TextView tvComp = findViewById(R.id.tvWeightCompleted);
         android.widget.ProgressBar pb = findViewById(R.id.pbWeight);
         CircularProgressView cpv = findViewById(R.id.cpvWeightSummary);
         TextView tvSummary = findViewById(R.id.tvWeightSummary);
         
         if (tv != null) {
             // If weight is 0, user hasn't logged weight today
             if (current == 0f) {
                 tv.setText("No log / " + goal + " kg");
                 tvComp.setVisibility(android.view.View.GONE);
                 pb.setProgress(0);
                 if (cpv != null) cpv.setProgress(0);
                 if (tvSummary != null) tvSummary.setText("--");
                 return;
             }
             
             tv.setText(String.format(java.util.Locale.US, "%.1f / %.1f kg", current, goal));
             pb.setMax(100);
             if (cpv != null) cpv.setMaxProgress(100);
             
             int progress;
             boolean completed;
             if (goal < current) { // Weight Loss Goal
                 completed = current <= goal;
                 progress = completed ? 100 : (int) ((goal / current) * 100);
             } else { // Weight Gain Goal
                 completed = current >= goal;
                 progress = completed ? 100 : (int) ((current / goal) * 100);
             }
             
             tvComp.setVisibility(completed ? android.view.View.VISIBLE : android.view.View.GONE);
             pb.setProgress(progress);
             if (cpv != null) cpv.setProgress(progress);
             if (tvSummary != null) {
                 tvSummary.setText(String.format(java.util.Locale.US, "%.1f", current));
             }
         }
    }
}
