package com.example.trackfit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogFoodBottomSheet extends BottomSheetDialogFragment {

    private String foodId;
    private String foodName;
    private int baseCalories;
    private String quantityUnit;
    private double defaultQuantityValue;

    private int currentQuantity = 1;

    private TextView tvSheetFoodName, tvSheetCaloriesPerUnit, tvQuantitySelected, tvSheetUnit;
    private ImageButton btnMinus, btnPlus;
    private Button btnLogCalories;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public static LogFoodBottomSheet newInstance(String foodId, String foodName, int baseCalories, String quantityUnit, double defaultQuantityValue) {
        LogFoodBottomSheet fragment = new LogFoodBottomSheet();
        Bundle args = new Bundle();
        args.putString("foodId", foodId);
        args.putString("foodName", foodName);
        args.putInt("baseCalories", baseCalories);
        args.putString("quantityUnit", quantityUnit);
        args.putDouble("defaultQuantityValue", defaultQuantityValue);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            foodId = getArguments().getString("foodId");
            foodName = getArguments().getString("foodName");
            baseCalories = getArguments().getInt("baseCalories");
            quantityUnit = getArguments().getString("quantityUnit");
            defaultQuantityValue = getArguments().getDouble("defaultQuantityValue");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_log_food, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvSheetFoodName = view.findViewById(R.id.tvSheetFoodName);
        tvSheetCaloriesPerUnit = view.findViewById(R.id.tvSheetCaloriesPerUnit);
        tvQuantitySelected = view.findViewById(R.id.tvQuantitySelected);
        tvSheetUnit = view.findViewById(R.id.tvSheetUnit);
        btnMinus = view.findViewById(R.id.btnMinus);
        btnPlus = view.findViewById(R.id.btnPlus);
        btnLogCalories = view.findViewById(R.id.btnLogCalories);

        tvSheetFoodName.setText(foodName);
        
        String qtyStr;
        if (defaultQuantityValue == (long) defaultQuantityValue) {
            qtyStr = String.format("%d", (long) defaultQuantityValue);
        } else {
            qtyStr = String.format("%s", defaultQuantityValue);
        }
        tvSheetCaloriesPerUnit.setText(baseCalories + " kcal per " + qtyStr + " " + quantityUnit);
        tvSheetUnit.setText(quantityUnit);
        
        updateQuantityDisplay();

        btnMinus.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                updateQuantityDisplay();
            }
        });

        btnPlus.setOnClickListener(v -> {
            currentQuantity++;
            updateQuantityDisplay();
        });

        btnLogCalories.setOnClickListener(v -> logFood());

        ImageButton btnEditFood = view.findViewById(R.id.btnEditFood);
        ImageButton btnDeleteFood = view.findViewById(R.id.btnDeleteFood);

        btnEditFood.setOnClickListener(v -> {
            dismiss();
            android.content.Intent intent = new android.content.Intent(getContext(), AddFoodActivity.class);
            intent.putExtra("foodId", foodId);
            intent.putExtra("foodName", foodName);
            intent.putExtra("baseCalories", baseCalories);
            intent.putExtra("quantityUnit", quantityUnit);
            intent.putExtra("defaultQuantityValue", defaultQuantityValue);
            startActivity(intent);
        });

        btnDeleteFood.setOnClickListener(v -> {
            db.collection("food_items").document(foodId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Food deleted", Toast.LENGTH_SHORT).show();
                        if (getActivity() instanceof FoodTrackingActivity) {
                            ((FoodTrackingActivity) getActivity()).loadFoods();
                        }
                        dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
        });

        return view;
    }

    private void updateQuantityDisplay() {
        tvQuantitySelected.setText(String.valueOf(currentQuantity));
    }

    private void logFood() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        int totalCalories = baseCalories * currentQuantity;

        Date now = new Date();
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Map<String, Object> foodLog = new HashMap<>();
        foodLog.put("user_id", currentUser.getUid());
        foodLog.put("food_id", foodId);
        foodLog.put("quantity_selected", currentQuantity);
        foodLog.put("calories_logged", totalCalories);
        foodLog.put("date", dfDate.format(now));
        foodLog.put("time", dfTime.format(now));
        foodLog.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("food_logs")
                .add(foodLog)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), totalCalories + " Calories Logged", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to log calories", Toast.LENGTH_SHORT).show();
                });
    }
}
