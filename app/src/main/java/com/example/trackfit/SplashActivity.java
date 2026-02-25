package com.example.trackfit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Already signed in → go straight to Dashboard
            startActivity(new Intent(this, DashboardActivity.class));
        } else {
            // Not signed in → show Login (which has link to Signup)
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
