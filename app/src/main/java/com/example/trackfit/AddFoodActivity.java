package com.example.trackfit;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddFoodActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etFoodName, etQuantityValue, etCalories;
    private Spinner spinnerQuantityUnit;
    private Button btnAddFood;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String existingFoodId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        etFoodName = findViewById(R.id.etFoodName);
        etQuantityValue = findViewById(R.id.etQuantityValue);
        spinnerQuantityUnit = findViewById(R.id.spinnerQuantityUnit);
        etCalories = findViewById(R.id.etCalories);
        btnAddFood = findViewById(R.id.btnAddFood);

        // Populate Spinner
        String[] units = new String[]{
                "Serving", "Plate", "Bowl", "Cup", "Piece", "Slice", "Tablespoon (tbsp)", "Teaspoon (tsp)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, units);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerQuantityUnit.setAdapter(adapter);

        TextView tvActivityTitle = findViewById(R.id.tvActivityTitle);

        existingFoodId = getIntent().getStringExtra("foodId");
        if (existingFoodId != null) {
            tvActivityTitle.setText("Update Food");
            btnAddFood.setText("Update Food");

            etFoodName.setText(getIntent().getStringExtra("foodName"));
            etCalories.setText(String.valueOf(getIntent().getIntExtra("baseCalories", 0)));

            double qty = getIntent().getDoubleExtra("defaultQuantityValue", 1.0);
            if (qty == (long) qty) {
                etQuantityValue.setText(String.format("%d", (long) qty));
            } else {
                etQuantityValue.setText(String.valueOf(qty));
            }

            String unitStr = getIntent().getStringExtra("quantityUnit");
            if (unitStr != null) {
                int spinnerPosition = adapter.getPosition(unitStr);
                spinnerQuantityUnit.setSelection(spinnerPosition);
            }
        }

        btnBack.setOnClickListener(v -> finish());
        
        btnAddFood.setOnClickListener(v -> saveFoodItem());
    }

    private void saveFoodItem() {
        String foodName = etFoodName.getText().toString().trim();
        String quantityStr = etQuantityValue.getText().toString().trim();
        String caloriesStr = etCalories.getText().toString().trim();
        String unit = spinnerQuantityUnit.getSelectedItem().toString();

        if (foodName.isEmpty() || quantityStr.isEmpty() || caloriesStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double quantityValue;
        int calories;
        try {
            quantityValue = Double.parseDouble(quantityStr);
            calories = Integer.parseInt(caloriesStr);
            if (calories <= 0 || quantityValue <= 0) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        FoodItem newFood = new FoodItem(userId, foodName, quantityValue, unit, calories);

        if (existingFoodId != null) {
            db.collection("food_items").document(existingFoodId)
                    .set(newFood)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddFoodActivity.this, "Food updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddFoodActivity.this, "Failed to update food", Toast.LENGTH_SHORT).show());
        } else {
            newFood.setCreatedAt(com.google.firebase.firestore.FieldValue.serverTimestamp());
            db.collection("food_items")
                    .add(newFood)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddFoodActivity.this, "Food added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddFoodActivity.this, "Failed to save food", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
