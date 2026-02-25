package com.example.trackfit;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BmiCalculatorActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RadioGroup genderGroup;
    private RadioButton rbMale, rbFemale;
    private EditText etHeight, etWeight;
    private Button btnHeightUnit, btnWeightUnit;
    private Button btnCalculate;
    private LinearLayout resultContainer;
    private TextView tvBmiValue, tvBmiCategory, tvBmiFeedback;
    private LinearLayout bmiRangeBar;

    private String heightUnit = "cm";
    private String weightUnit = "kg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bmi_calculator);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        genderGroup = findViewById(R.id.genderGroup);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        btnHeightUnit = findViewById(R.id.btnHeightUnit);
        btnWeightUnit = findViewById(R.id.btnWeightUnit);
        btnCalculate = findViewById(R.id.btnCalculate);
        resultContainer = findViewById(R.id.resultContainer);
        tvBmiValue = findViewById(R.id.tvBmiValue);
        tvBmiCategory = findViewById(R.id.tvBmiCategory);
        tvBmiFeedback = findViewById(R.id.tvBmiFeedback);
        bmiRangeBar = findViewById(R.id.bmiRangeBar);

        // Setup listeners
        setupBackButton();
        setupGenderRadios();
        setupUnitButtons();
        setupCalculateButton();
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupGenderRadios() {
        setRadioButtonText(rbMale, "♂", "Male");
        setRadioButtonText(rbFemale, "♀", "Female");

        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateRadioButtonColors();
        });

        updateRadioButtonColors();
    }

    private void setRadioButtonText(RadioButton rb, String icon, String text) {
        String fullText = icon + "\n" + text;
        SpannableString spannable = new SpannableString(fullText);

        spannable.setSpan(new AbsoluteSizeSpan(20, true), 0, icon.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), icon.length() + 1, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        rb.setText(spannable);
    }

    private void updateRadioButtonColors() {
        int selectedId = genderGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.rbMale) {
            rbMale.setTextColor(Color.parseColor("#00E5FF"));
            rbFemale.setTextColor(Color.WHITE);
        } else if (selectedId == R.id.rbFemale) {
            rbMale.setTextColor(Color.WHITE);
            rbFemale.setTextColor(Color.parseColor("#00E5FF"));
        } else {
            rbMale.setTextColor(Color.WHITE);
            rbFemale.setTextColor(Color.WHITE);
        }
    }

    private void setupUnitButtons() {
        btnHeightUnit.setOnClickListener(v -> {
            if (heightUnit.equals("cm")) {
                heightUnit = "ft/in";
                btnHeightUnit.setText("ft/in");
            } else {
                heightUnit = "cm";
                btnHeightUnit.setText("cm");
            }
            etHeight.setText("");
        });

        btnWeightUnit.setOnClickListener(v -> {
            if (weightUnit.equals("kg")) {
                weightUnit = "lb";
                btnWeightUnit.setText("lb");
            } else {
                weightUnit = "kg";
                btnWeightUnit.setText("kg");
            }
            etWeight.setText("");
        });
    }

    private void setupCalculateButton() {
        btnCalculate.setOnClickListener(v -> calculateBmi());
    }

    private void calculateBmi() {
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (heightStr.isEmpty()) {
            etHeight.setError("Height is required");
            return;
        }

        if (weightStr.isEmpty()) {
            etWeight.setError("Weight is required");
            return;
        }

        try {
            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);

            // Convert to metric if needed
            if (heightUnit.equals("ft/in")) {
                height = height * 0.3048; // feet to meters
            } else {
                height = height / 100; // cm to meters
            }

            if (weightUnit.equals("lb")) {
                weight = weight * 0.453592; // lb to kg
            }

            // Calculate BMI
            double bmi = weight / (height * height);

            // Display result
            displayBmiResult(bmi);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input. Please enter valid numbers.", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayBmiResult(double bmi) {
        // Format BMI to 1 decimal place
        String bmiFormatted = String.format("%.1f", bmi);
        tvBmiValue.setText(bmiFormatted);

        // Determine category and color
        String category;
        int color;
        String feedback;

        if (bmi < 18.5) {
            category = "Underweight";
            color = Color.parseColor("#1E88E5"); // Blue
            feedback = "You are underweight. Consider consulting a healthcare provider for guidance.";
        } else if (bmi < 25) {
            category = "Normal weight";
            color = Color.parseColor("#00E5FF"); // Cyan
            feedback = "You have a healthy weight. Great job! Keep up the good work.";
        } else if (bmi < 30) {
            category = "Overweight";
            color = Color.parseColor("#FFA500"); // Orange
            feedback = "You are overweight. Focus on regular physical activity and a balanced diet.";
        } else {
            category = "Obese";
            color = Color.parseColor("#EF5350"); // Red
            feedback = "You have obesity. Consider consulting a healthcare provider for personalized guidance.";
        }

        tvBmiCategory.setText(category);
        tvBmiCategory.setTextColor(color);
        tvBmiFeedback.setText(feedback);

        // Show result container
        resultContainer.setVisibility(LinearLayout.VISIBLE);

        // Update BMI range bar indicator
        updateBmiRangeBar(bmi);
    }

    private void updateBmiRangeBar(double bmi) {
        // This will be styled in the layout with a visual indicator
        // For now, we update the appearance based on bmi value
        if (bmi < 18.5) {
            bmiRangeBar.setBackgroundColor(Color.parseColor("#1E88E5"));
        } else if (bmi < 24.9) {
            bmiRangeBar.setBackgroundColor(Color.parseColor("#00E5FF"));
        } else if (bmi < 29.9) {
            bmiRangeBar.setBackgroundColor(Color.parseColor("#FFA500"));
        } else {
            bmiRangeBar.setBackgroundColor(Color.parseColor("#EF5350"));
        }
    }
}
