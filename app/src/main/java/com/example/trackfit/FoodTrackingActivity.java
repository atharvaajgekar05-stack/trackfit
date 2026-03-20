package com.example.trackfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FoodTrackingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private LinearLayout btnAddCustomFood;
    private RecyclerView rvFoods;
    private TextView tvEmptyState;

    private FoodAdapter foodAdapter;
    private List<FoodItem> foodList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_tracking);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        btnAddCustomFood = findViewById(R.id.btnAddCustomFood);
        rvFoods = findViewById(R.id.rvFoods);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        btnBack.setOnClickListener(v -> finish());

        btnAddCustomFood.setOnClickListener(v -> {
            startActivity(new Intent(FoodTrackingActivity.this, AddFoodActivity.class));
        });

        foodList = new ArrayList<>();
        foodAdapter = new FoodAdapter(foodList, food -> {
            LogFoodBottomSheet bottomSheet = LogFoodBottomSheet.newInstance(food.getFoodId(), food.getFoodName(), food.getCalories(), food.getQuantityUnit(), food.getQuantityValue());
            bottomSheet.show(getSupportFragmentManager(), "LogFoodBottomSheet");
        });

        rvFoods.setLayoutManager(new LinearLayoutManager(this));
        rvFoods.setAdapter(foodAdapter);

        loadFoods();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFoods();
    }

    public void loadFoods() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();

        db.collection("food_items")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    foodList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FoodItem food = document.toObject(FoodItem.class);
                        food.setFoodId(document.getId());
                        foodList.add(food);
                    }
                    
                    java.util.Collections.sort(foodList, (f1, f2) -> {
                        Object t1 = f1.getCreatedAt();
                        Object t2 = f2.getCreatedAt();
                        if (t1 instanceof Comparable && t2 instanceof Comparable) {
                            return ((Comparable) t2).compareTo(t1);
                        }
                        return 0;
                    });
                    
                    foodAdapter.notifyDataSetChanged();
                    
                    if (foodList.isEmpty()) {
                        rvFoods.setVisibility(View.GONE);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvFoods.setVisibility(View.VISIBLE);
                        tvEmptyState.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load foods", Toast.LENGTH_SHORT).show();
                });
    }
}
