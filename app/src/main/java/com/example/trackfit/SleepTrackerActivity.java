package com.example.trackfit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SleepTrackerActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnStartStop;
    private Button btnReset;
    private Button btnStop;
    private Button btnSave;
    private TextView tvTimer;
    private LinearLayout successMessageContainer;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private long elapsedTimeMillis = 0;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sleep_tracker);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnReset = findViewById(R.id.btnReset);
        btnStop = findViewById(R.id.btnStop);
        btnSave = findViewById(R.id.btnSave);
        tvTimer = findViewById(R.id.tvTimer);
        successMessageContainer = findViewById(R.id.successMessageContainer);

        // Setup listeners
        setupBackButton();
        setupStartStopButton();
        setupResetButton();
        setupStopButton();
        setupSaveButton();
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupStartStopButton() {
        btnStartStop.setOnClickListener(v -> {
            if (!isTimerRunning) {
                startTimer();
            }
        });
    }

    private void setupResetButton() {
        btnReset.setOnClickListener(v -> resetTimer());
    }

    private void setupStopButton() {
        btnStop.setOnClickListener(v -> stopTimer());
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            saveSleepLog();
        });
    }

    private void startTimer() {
        isTimerRunning = true;
        btnSave.setVisibility(android.view.View.GONE);
        successMessageContainer.setVisibility(android.view.View.GONE);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning) {
                    elapsedTimeMillis += 1000;
                    updateTimerDisplay();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        btnSave.setVisibility(android.view.View.VISIBLE);
    }

    private void updateTimerDisplay() {
        long hours = (elapsedTimeMillis / 1000) / 3600;
        long minutes = ((elapsedTimeMillis / 1000) % 3600) / 60;
        long seconds = (elapsedTimeMillis / 1000) % 60;

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        tvTimer.setText(timeString);
    }

    private void saveSleepLog() {
        // Show success message
        successMessageContainer.setVisibility(android.view.View.VISIBLE);
        Toast.makeText(this, "Sleep info logged successfully!", Toast.LENGTH_SHORT).show();

        // Reset the timer after 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            resetTimer();
        }, 2000);
    }

    private void resetTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        elapsedTimeMillis = 0;
        tvTimer.setText("00:00:00");
        btnSave.setVisibility(android.view.View.GONE);
        successMessageContainer.setVisibility(android.view.View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
