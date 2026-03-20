package com.example.trackfit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private com.google.android.material.imageview.ShapeableImageView ivProfilePic;
    private TextView tvGender, tvAge, tvHeight, tvWeight, tvDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.profileRoot), (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                });

        // Initialize UI Elements
        ivProfilePic = findViewById(R.id.ivProfilePic);
        tvGender = findViewById(R.id.tvGender);
        tvAge = findViewById(R.id.tvAge);
        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);
        tvDisplayName = findViewById(R.id.tvDisplayName);

        // Show current user email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView tvEmail = findViewById(R.id.tvUserEmail);
        if (user != null && user.getEmail() != null) {
            tvEmail.setText(user.getEmail());
            fetchUserMetrics(user.getUid());
        }

        // Logout
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchUserMetrics(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Gender
                        String gender = documentSnapshot.getString("gender");
                        if (gender != null) tvGender.setText(gender);

                        // Name
                        String name = documentSnapshot.getString("name");
                        if (name != null && tvDisplayName != null) {
                            tvDisplayName.setText(name);
                        }

                        // Age
                        Long age = documentSnapshot.getLong("age");
                        if (age != null) tvAge.setText(String.valueOf(age));

                        // Height
                        String height = documentSnapshot.getString("height");
                        String heightUnit = documentSnapshot.getString("heightUnit");
                        if (height != null && heightUnit != null) {
                            tvHeight.setText(height + " " + heightUnit);
                        } else if (height != null) {
                            tvHeight.setText(height);
                        }

                        // Weight
                        String weight = documentSnapshot.getString("weight");
                        String weightUnit = documentSnapshot.getString("weightUnit");
                        if (weight != null && weightUnit != null) {
                            tvWeight.setText(weight + " " + weightUnit);
                        } else if (weight != null) {
                            tvWeight.setText(weight);
                        }

                        // Profile Picture
                        String profileImgUriStr = documentSnapshot.getString("profileImgUri");
                        if (profileImgUriStr != null && !profileImgUriStr.isEmpty()) {
                            try {
                                Uri profileUri = Uri.parse(profileImgUriStr);
                                ivProfilePic.setImageURI(profileUri);
                                ivProfilePic.setPadding(0, 0, 0, 0);
                                ivProfilePic.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load metrics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
