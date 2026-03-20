package com.example.trackfit;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LiveStepTrackingActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView tvSteps, tvDistance, tvCalories, tvStatus;
    private View statusIndicator;
    private Button btnStartTracking, btnStopTracking, btnResetTracking, btnSaveSession;

    private boolean isTracking = false;
    private int currentSteps = 0;

    private SharedPreferences prefs;

    private BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepTrackingService.BROADCAST_STEPS.equals(intent.getAction())) {
                int steps = intent.getIntExtra(StepTrackingService.EXTRA_STEPS, 0);
                updateUI(steps);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_step_tracking);

        prefs = getSharedPreferences("TrackFitPrefs", MODE_PRIVATE);

        initViews();
        restoreState();
        updateStatusUI();

        btnStartTracking.setOnClickListener(v -> checkPermissionsAndStart());
        btnStopTracking.setOnClickListener(v -> stopTracking());
        btnResetTracking.setOnClickListener(v -> resetTracking());
        btnSaveSession.setOnClickListener(v -> saveSession());
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvSteps = findViewById(R.id.tvSteps);
        tvDistance = findViewById(R.id.tvDistance);
        tvCalories = findViewById(R.id.tvCalories);
        tvStatus = findViewById(R.id.tvStatus);
        statusIndicator = findViewById(R.id.statusIndicator);

        btnStartTracking = findViewById(R.id.btnStartTracking);
        btnStopTracking = findViewById(R.id.btnStopTracking);
        btnResetTracking = findViewById(R.id.btnResetTracking);
        btnSaveSession = findViewById(R.id.btnSaveSession);
    }

    private void restoreState() {
        isTracking = prefs.getBoolean("step_tracking_active", false);
        currentSteps = prefs.getInt("saved_session_steps", 0);
        updateUI(currentSteps);
    }

    private void updateUI(int steps) {
        currentSteps = steps;
        tvSteps.setText(String.format(Locale.getDefault(), "%,d", steps));

        double distanceMeters = steps * 0.75;
        double distanceKm = distanceMeters / 1000.0;
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));

        int calories = (int) Math.round(steps * 0.04);
        tvCalories.setText(String.format(Locale.getDefault(), "%d kcal", calories));
    }

    private void updateStatusUI() {
        if (isTracking) {
            tvStatus.setText("Status: Tracking Active");
            statusIndicator.setBackgroundResource(R.drawable.bg_circle_cyan);
            statusIndicator.getBackground().setTint(Color.parseColor("#39cccc")); // cyan
            
            btnStartTracking.setVisibility(View.GONE);
            btnSaveSession.setVisibility(View.VISIBLE);
        } else {
            if (currentSteps > 0) {
                tvStatus.setText("Status: Tracking Paused");
                statusIndicator.setBackgroundResource(R.drawable.bg_circle_cyan);
                statusIndicator.getBackground().setTint(Color.parseColor("#FFC107")); // yellow/orange
                
                btnStartTracking.setVisibility(View.VISIBLE);
                btnSaveSession.setVisibility(View.VISIBLE);
            } else {
                tvStatus.setText("Status: Tracking Stopped");
                statusIndicator.setBackgroundResource(R.drawable.bg_circle_cyan);
                statusIndicator.getBackground().setTint(Color.parseColor("#9AAEB3")); // grey
                
                btnStartTracking.setVisibility(View.VISIBLE);
                btnSaveSession.setVisibility(View.GONE);
            }
        }
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{
                        Manifest.permission.ACTIVITY_RECOGNITION,
                        Manifest.permission.POST_NOTIFICATIONS
                };
            } else {
                permissions = new String[]{Manifest.permission.ACTIVITY_RECOGNITION};
            }

            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                return;
            }
        }
        startTracking();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                Toast.makeText(this, "Permissions required for tracking", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTracking() {
        isTracking = true;
        prefs.edit().putBoolean("step_tracking_active", true).apply();
        updateStatusUI();

        Intent serviceIntent = new Intent(this, StepTrackingService.class);
        serviceIntent.setAction(StepTrackingService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopTracking() {
        isTracking = false;
        prefs.edit().putBoolean("step_tracking_active", false).apply();
        updateStatusUI();

        Intent serviceIntent = new Intent(this, StepTrackingService.class);
        serviceIntent.setAction(StepTrackingService.ACTION_STOP);
        startService(serviceIntent); // Also triggers stopTracking in service
    }

    private void resetTracking() {
        stopTracking();
        currentSteps = 0;
        prefs.edit().putInt("saved_session_steps", 0).apply();
        updateUI(0);
        updateStatusUI();
    }

    private void saveSession() {
        if (currentSteps == 0) {
            Toast.makeText(this, "No steps recorded", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        double distanceMeters = currentSteps * 0.75;
        double distanceKm = distanceMeters / 1000.0;
        int calories = (int) Math.round(currentSteps * 0.04);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();

        Map<String, Object> session = new HashMap<>();
        session.put("step_session_id", UUID.randomUUID().toString());
        session.put("user_id", mAuth.getCurrentUser().getUid());
        session.put("steps", currentSteps);
        session.put("distance_km", distanceKm);
        session.put("calories_burned", calories);
        session.put("date", dateFormat.format(now));
        session.put("time", timeFormat.format(now));
        session.put("created_at", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("step_sessions")
                .add(session)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(LiveStepTrackingActivity.this, "Steps session saved successfully", Toast.LENGTH_SHORT).show();
                    resetTracking(); // Resets and stops tracking properly
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LiveStepTrackingActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stepReceiver, new IntentFilter(StepTrackingService.BROADCAST_STEPS), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stepReceiver, new IntentFilter(StepTrackingService.BROADCAST_STEPS));
        }
        restoreState(); // Refresh in case service updated it
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(stepReceiver);
    }
}
