package com.example.trackfit;

public class FoodItem {
    private String foodId;
    private String userId;
    private String foodName;
    private double quantityValue;
    private String quantityUnit;
    private int calories;
    private Object createdAt;

    public FoodItem() {
        // Required empty constructor for Firestore
    }

    public FoodItem(String userId, String foodName, double quantityValue, String quantityUnit, int calories) {
        this.userId = userId;
        this.foodName = foodName;
        this.quantityValue = quantityValue;
        this.quantityUnit = quantityUnit;
        this.calories = calories;
    }

    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public double getQuantityValue() { return quantityValue; }
    public void setQuantityValue(double quantityValue) { this.quantityValue = quantityValue; }

    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }
}
