package com.example.trackfit;

import android.content.Intent;
import android.graphics.Color;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HealthMetricsActivity extends AppCompatActivity {

    private RadioGroup genderGroup;
    private RadioButton rbMale, rbFemale;
    private EditText etAge, etHeight, etWeight;
    private Spinner spinnerHeightUnit, spinnerWeightUnit;
    private Button btnSave;
    private ImageButton btnCamera;
    private ImageView ivProfilePic;
    private android.net.Uri selectedImageUri = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_health_metrics);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        genderGroup = findViewById(R.id.genderGroup);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        
        spinnerHeightUnit = findViewById(R.id.spinnerHeightUnit);
        spinnerWeightUnit = findViewById(R.id.spinnerWeightUnit);
        
        btnSave = findViewById(R.id.btnSave);
        btnCamera = findViewById(R.id.btnCamera);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        
        // Initialize Launchers early
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.graphics.Bitmap photo = (android.graphics.Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        ivProfilePic.setImageBitmap(photo);
                        ivProfilePic.setPadding(0, 0, 0, 0);
                        ivProfilePic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivProfilePic.clearColorFilter();
                        // Save to cache and get URI
                        try {
                            java.io.File file = new java.io.File(getCacheDir(), "profile_pic.jpg");
                            java.io.FileOutputStream fOut = new java.io.FileOutputStream(file);
                            photo.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fOut);
                            fOut.flush();
                            fOut.close();
                            selectedImageUri = android.net.Uri.fromFile(file);
                        } catch (Exception e) {}
                    }
                }
            }
        );

        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivProfilePic.setImageURI(selectedImageUri);
                    ivProfilePic.setPadding(0, 0, 0, 0);
                    ivProfilePic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivProfilePic.clearColorFilter();
                }
            }
        );

        setupSpinners();
        setupGenderRadios();
        setupCameraButton();
        setupSaveButton();
    }

    private void setupSpinners() {
        // Height Spinner (cm, ft)
        ArrayAdapter<String> heightAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"cm", "ft"}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.parseColor("#666666")); // Match hint color
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                return view;
            }
        };
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHeightUnit.setAdapter(heightAdapter);

        // Weight Spinner (kg, lb)
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"kg", "lb"}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.parseColor("#666666")); // Match hint color
                return view;
            }

             @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                return view;
            }
        };
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeightUnit.setAdapter(weightAdapter);
    }

    private void setupGenderRadios() {
        // Set styled text for RadioButtons to match visual design
        // ♂ (20sp) \n Male (Bold)
        setBottonText(rbMale, "♂", "Male");
        setBottonText(rbFemale, "♀", "Female");
        
        // Add checked change listener to update text color
        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateRadioButtonColors();
        });
        
        // Initial color update
        updateRadioButtonColors();
    }
    
    private void setBottonText(RadioButton rb, String icon, String text) {
        String fullText = icon + "\n" + text;
        SpannableString spannable = new SpannableString(fullText);
        
        // Size for Icon (20sp approx)
        spannable.setSpan(new AbsoluteSizeSpan(20, true), 0, icon.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // Bold for Text
        spannable.setSpan(new StyleSpan(Typeface.BOLD), icon.length() + 1, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        rb.setText(spannable);
    }
    
    private void updateRadioButtonColors() {
        // Selected -> Cyan (#00E5FF), Unselected -> White (or Grey per design)
        // Checking previous logic: Male (Selected Cyan), Female (Unselected Grey)
        // But user asked for Unselected to be White in later steps. Let's stick to White for unselected.
        // Actually, user said "Male and Female text are both White when unselected".
        // And when Selected? Following the Selector logic: border is Cyan. Text should probably be Cyan too?
        // Let's implement Cyan for Selected, White for Unselected.
        
        int selectedId = genderGroup.getCheckedRadioButtonId();
        
        if (selectedId == R.id.rbMale) {
            setSpannableColor(rbMale, Color.parseColor("#00E5FF")); // Cyan
            setSpannableColor(rbFemale, Color.WHITE);
        } else if (selectedId == R.id.rbFemale) {
            setSpannableColor(rbMale, Color.WHITE);
            setSpannableColor(rbFemale, Color.parseColor("#00E5FF"));
        } else {
             setSpannableColor(rbMale, Color.WHITE);
             setSpannableColor(rbFemale, Color.WHITE);
        }
    }
    
    private void setSpannableColor(RadioButton rb, int color) {
        SpannableString spannable = new SpannableString(rb.getText());
        spannable.setSpan(new ForegroundColorSpan(color), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Re-apply size/bold spans if lost, or just blindly apply color to the existing text
        // Note: setText(spannable) might clear existing spans if we create a NEW SpannableString from .getText().toString()
        // Better: cast rb.getText() to Spannable if possible or just re-create.
        
        // Safe approach: Re-create full spannable
        String rawText = rb.getText().toString();
        String[] parts = rawText.split("\n");
        if (parts.length >= 2) {
             String icon = parts[0];
             // Re-call setup logic but with specific color
             // ... Or simplified: just setTextColor?
             // RadioButton setTextColor sets everything.
             rb.setTextColor(color);
        }
    }

    private void setupCameraButton() {
        btnCamera.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Profile Picture");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) { // Camera
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(cameraIntent);
                } else if (which == 1) { // Gallery
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    galleryLauncher.launch(galleryIntent);
                } else {
                    dialog.dismiss();
                }
            });
            builder.show();
        });
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            String ageStr = etAge.getText().toString().trim();
            String heightStr = etHeight.getText().toString().trim();
            String weightStr = etWeight.getText().toString().trim();

            if (ageStr.isEmpty()) {
                etAge.setError("Age is required");
                return;
            }
            
            int age = Integer.parseInt(ageStr);
            if (age < 0) {
                 etAge.setError("Age must be valid");
                 return;
            }

            if (heightStr.isEmpty()) {
                etHeight.setError("Height is required");
                return;
            }

            if (weightStr.isEmpty()) {
                etWeight.setError("Weight is required");
                return;
            }

            int selectedId = genderGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String gender = (selectedId == R.id.rbMale) ? "Male" : "Female";
            String userId = mAuth.getUid();

            if (userId == null) {
                Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare data for Firestore
            Map<String, Object> userMetrics = new HashMap<>();
            userMetrics.put("age", age);
            userMetrics.put("height", heightStr);
            userMetrics.put("heightUnit", spinnerHeightUnit.getSelectedItem().toString());
            userMetrics.put("weight", weightStr);
            userMetrics.put("weightUnit", spinnerWeightUnit.getSelectedItem().toString());
            userMetrics.put("gender", gender);
            if (selectedImageUri != null) {
                userMetrics.put("profileImgUri", selectedImageUri.toString());
            }

            db.collection("users").document(userId)
                .set(userMetrics)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        });
    }
}