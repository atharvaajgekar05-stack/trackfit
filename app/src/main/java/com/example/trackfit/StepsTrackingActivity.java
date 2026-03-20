package com.example.trackfit;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class StepsTrackingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps_tracking);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        android.widget.LinearLayout cardLiveTracking = findViewById(R.id.cardLiveTracking);
        cardLiveTracking.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(StepsTrackingActivity.this, LiveStepTrackingActivity.class);
            startActivity(intent);
        });

        android.widget.LinearLayout cardManualTracking = findViewById(R.id.cardManualTracking);
        cardManualTracking.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(StepsTrackingActivity.this, ManualStepTrackingActivity.class);
            startActivity(intent);
        });
    }
}
