package com.example.trackfit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    ProgressBar progressBar;
    int progress = 0;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        startProgress();
    }

    private void startProgress() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progress += 1;
                progressBar.setProgress(progress);

                if (progress < 100) {
                    handler.postDelayed(this, 35);
                } else {
                    navigateAfterSplash();
                }
            }
        }, 25);
    }

    private void navigateAfterSplash() {
        android.content.SharedPreferences prefs = getSharedPreferences("TrackFitPrefs", MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("remember_me", false);
        
        if (rememberMe && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, DashboardActivity.class));
        } else {
            // Ensure sign out if remember me was not checked
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
