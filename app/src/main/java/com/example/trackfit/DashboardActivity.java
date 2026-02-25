package com.example.trackfit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private FloatingActionButton fab;
    private LinearLayout navHome, navGoals, navProfile;

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

                    // Also expand scrollView bottom margin so content isn't hidden
                    android.view.ViewGroup.MarginLayoutParams lp =
                            (android.view.ViewGroup.MarginLayoutParams)
                                    findViewById(R.id.scrollView).getLayoutParams();
                    lp.bottomMargin = tabBarDp + navBarH;
                    findViewById(R.id.scrollView).setLayoutParams(lp);

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

        fab.setOnClickListener(v -> {
            FabMenuBottomSheet bottomSheet = new FabMenuBottomSheet();
            bottomSheet.show(getSupportFragmentManager(), "FAB_MENU");
        });

        navHome.setOnClickListener(v -> { /* Already home */ });

        navGoals.setOnClickListener(v ->
                Toast.makeText(this, "Goals page coming soon!", Toast.LENGTH_SHORT).show());

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        loadDashboardData();
    }

    private void loadDashboardData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Future: bind live Firestore values to TextViews/CPVs here
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
