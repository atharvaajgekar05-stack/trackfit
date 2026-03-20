package com.example.trackfit;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class WaterIntakeActivity extends AppCompatActivity {

    private ImageButton btnBack;
    
    private ImageButton btnGlassMinus, btnGlassPlus;
    private TextView tvGlassCount;
    private int glassCount = 0;

    private ImageButton btnBottleMinus, btnBottlePlus;
    private TextView tvBottleCount;
    private int bottleCount = 0;

    private ImageButton btnLargeBottleMinus, btnLargeBottlePlus;
    private TextView tvLargeBottleCount;
    private int largeBottleCount = 0;

    private LinearLayout btnLogWater;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_intake);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI
        btnBack = findViewById(R.id.btnBack);
        
        btnGlassMinus = findViewById(R.id.btnGlassMinus);
        btnGlassPlus = findViewById(R.id.btnGlassPlus);
        tvGlassCount = findViewById(R.id.tvGlassCount);

        btnBottleMinus = findViewById(R.id.btnBottleMinus);
        btnBottlePlus = findViewById(R.id.btnBottlePlus);
        tvBottleCount = findViewById(R.id.tvBottleCount);

        btnLargeBottleMinus = findViewById(R.id.btnLargeBottleMinus);
        btnLargeBottlePlus = findViewById(R.id.btnLargeBottlePlus);
        tvLargeBottleCount = findViewById(R.id.tvLargeBottleCount);

        btnLogWater = findViewById(R.id.btnLogWater);

        // Click listeners
        btnBack.setOnClickListener(v -> finish());

        // Glass
        btnGlassMinus.setOnClickListener(v -> {
            if (glassCount > 0) {
                glassCount--;
                tvGlassCount.setText(String.valueOf(glassCount));
                updateMinusButtonColors();
            }
        });
        btnGlassPlus.setOnClickListener(v -> {
            glassCount++;
            tvGlassCount.setText(String.valueOf(glassCount));
            updateMinusButtonColors();
        });

        // Bottle
        btnBottleMinus.setOnClickListener(v -> {
            if (bottleCount > 0) {
                bottleCount--;
                tvBottleCount.setText(String.valueOf(bottleCount));
                updateMinusButtonColors();
            }
        });
        btnBottlePlus.setOnClickListener(v -> {
            bottleCount++;
            tvBottleCount.setText(String.valueOf(bottleCount));
            updateMinusButtonColors();
        });

        // Large Bottle
        btnLargeBottleMinus.setOnClickListener(v -> {
            if (largeBottleCount > 0) {
                largeBottleCount--;
                tvLargeBottleCount.setText(String.valueOf(largeBottleCount));
                updateMinusButtonColors();
            }
        });
        btnLargeBottlePlus.setOnClickListener(v -> {
            largeBottleCount++;
            tvLargeBottleCount.setText(String.valueOf(largeBottleCount));
            updateMinusButtonColors();
        });

        btnLogWater.setOnClickListener(v -> logWaterIntake());
        
        // Initial setup
        updateMinusButtonColors();
    }

    private void updateMinusButtonColors() {
        int disabledColor = android.graphics.Color.parseColor("#9AAEB3");
        int enabledColor = android.graphics.Color.parseColor("#000000"); // Same as plus button icon
        int btnEnabledColor = android.graphics.Color.parseColor("#00E5FF"); // Same as plus button bg
        int btnDisabledColor = android.graphics.Color.parseColor("#2A3441"); // Original background for disabled

        // Glass
        if (glassCount > 0) {
            btnGlassMinus.setColorFilter(enabledColor);
            btnGlassMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(btnEnabledColor));
        } else {
            btnGlassMinus.setColorFilter(disabledColor);
            btnGlassMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(btnDisabledColor));
        }

        // Bottle
        if (bottleCount > 0) {
            btnBottleMinus.setColorFilter(enabledColor);
            btnBottleMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(btnEnabledColor));
        } else {
            btnBottleMinus.setColorFilter(disabledColor);
            btnBottleMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(btnDisabledColor));
        }

        // Large Bottle
        if (largeBottleCount > 0) {
            btnLargeBottleMinus.setColorFilter(enabledColor);
            btnLargeBottleMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(btnEnabledColor));
        } else {
            btnLargeBottleMinus.setColorFilter(disabledColor);
            btnLargeBottleMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(btnDisabledColor));
        }
    }

    private void logWaterIntake() {
        int totalWater = (glassCount * 250) + (bottleCount * 500) + (largeBottleCount * 1000);

        if (totalWater == 0) {
            Toast.makeText(this, "Please add water intake before logging", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String waterLogId = UUID.randomUUID().toString();

        Date now = new Date();
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Map<String, Object> waterLog = new HashMap<>();
        waterLog.put("water_log_id", waterLogId);
        waterLog.put("user_id", userId);
        waterLog.put("glass_count", glassCount);
        waterLog.put("bottle_count", bottleCount);
        waterLog.put("large_bottle_count", largeBottleCount);
        waterLog.put("total_water_ml", totalWater);
        waterLog.put("date", dfDate.format(now));
        waterLog.put("time", dfTime.format(now));
        waterLog.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("water_logs")
                .document(waterLogId)
                .set(waterLog)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(WaterIntakeActivity.this, totalWater + " ml water logged", Toast.LENGTH_SHORT).show();
                    resetQuantities();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(WaterIntakeActivity.this, "Failed to log water", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetQuantities() {
        glassCount = 0;
        bottleCount = 0;
        largeBottleCount = 0;

        tvGlassCount.setText("0");
        tvBottleCount.setText("0");
        tvLargeBottleCount.setText("0");
        
        updateMinusButtonColors();
    }
}
