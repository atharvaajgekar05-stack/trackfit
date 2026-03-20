package com.example.trackfit;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Seeds 3 months of realistic dummy data for user "Atharva Ajagekar".
 * Call {@link #seedAll(String)} once after login.
 */
public class DemoDataSeeder {

    private static final Random RNG = new Random(42);
    private static final SimpleDateFormat DF_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DF_TIME = new SimpleDateFormat("HH:mm", Locale.US);

    // Activity names matching the chips in ActivityTrackingActivity
    private static final String[] ACTIVITIES = {"Walking", "Running", "Cycling", "Swimming", "Hiking", "Football"};
    private static final double[] MET_VALUES = {3.5, 7.0, 6.0, 8.0, 6.0, 7.0};

    // Food items to seed
    private static final String[][] FOODS = {
            // {name, calories, unit, quantity}
            {"Rice", "130", "cup", "1"},
            {"Chicken Breast", "165", "piece", "1"},
            {"Banana", "89", "piece", "1"},
            {"Eggs", "78", "piece", "1"},
            {"Milk", "42", "glass", "1"},
            {"Bread", "79", "slice", "2"},
            {"Dal", "120", "bowl", "1"},
            {"Roti", "71", "piece", "1"},
    };

    // Location pairs for step logs
    private static final String[][] LOCATIONS = {
            {"Home", "College"},
            {"College", "Library"},
            {"Home", "Market"},
            {"Park", "Home"},
            {"Bus Stop", "College"},
            {"Home", "Gym"},
    };

    public static void seedAll(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Seed user profile
        seedUserProfile(db, userId);

        // 2. Seed daily goals
        seedDailyGoals(db, userId);

        // 3. Seed food items (one-time, not per day)
        String[] foodIds = seedFoodItems(db, userId);

        // 4. Seed 3 months of daily data: Dec 2025, Jan 2026, Feb 2026
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.DECEMBER, 1, 8, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(2026, Calendar.MARCH, 1, 0, 0, 0);

        while (cal.before(end)) {
            Date day = cal.getTime();
            String dateStr = DF_DATE.format(day);

            seedWaterLog(db, userId, dateStr);
            seedSleepLog(db, userId, dateStr);
            seedWeightLog(db, userId, dateStr, cal);
            seedFoodLogs(db, userId, dateStr, foodIds);
            seedActivityLog(db, userId, dateStr);
            seedStepLog(db, userId, dateStr);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    // ── User Profile ──────────────────────────────────────────────────────
    private static void seedUserProfile(FirebaseFirestore db, String userId) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Atharva Ajagekar");
        user.put("age", 70);
        user.put("gender", "Male");
        user.put("height", "175");
        user.put("heightUnit", "cm");
        user.put("weight", "68");
        user.put("weightUnit", "kg");

        db.collection("users").document(userId).set(user);
    }

    // ── Daily Goals ───────────────────────────────────────────────────────
    private static void seedDailyGoals(FirebaseFirestore db, String userId) {
        Map<String, Object> goals = new HashMap<>();
        goals.put("steps_goal", 8000);
        goals.put("water_goal_ml", 2500);
        goals.put("daily_calorie_intake_goal", 2000);
        goals.put("target_weight", 65.0);
        goals.put("daily_activity_calorie_goal", 400);
        goals.put("sleep_goal_hours", 8);
        goals.put("sleep_goal_minutes", 0);

        db.collection("daily_goals").document(userId).set(goals);
    }

    // ── Food Items ────────────────────────────────────────────────────────
    private static String[] seedFoodItems(FirebaseFirestore db, String userId) {
        String[] ids = new String[FOODS.length];
        for (int i = 0; i < FOODS.length; i++) {
            String id = UUID.randomUUID().toString();
            ids[i] = id;

            Map<String, Object> food = new HashMap<>();
            food.put("userId", userId);
            food.put("foodName", FOODS[i][0]);
            food.put("calories", Integer.parseInt(FOODS[i][1]));
            food.put("quantityUnit", FOODS[i][2]);
            food.put("quantityValue", Double.parseDouble(FOODS[i][3]));
            food.put("createdAt", FieldValue.serverTimestamp());

            db.collection("food_items").document(id).set(food);
        }
        return ids;
    }

    // ── Water Logs ────────────────────────────────────────────────────────
    private static void seedWaterLog(FirebaseFirestore db, String userId, String dateStr) {
        // 1-3 water logs per day
        int logCount = 1 + RNG.nextInt(3);
        for (int i = 0; i < logCount; i++) {
            String id = UUID.randomUUID().toString();
            int glasses = 1 + RNG.nextInt(4);    // 1-4
            int bottles = RNG.nextInt(3);         // 0-2
            int largeBtl = RNG.nextInt(2);        // 0-1
            int total = (glasses * 250) + (bottles * 500) + (largeBtl * 1000);

            String time = String.format(Locale.US, "%02d:%02d", 8 + (i * 4) + RNG.nextInt(3), RNG.nextInt(60));

            Map<String, Object> log = new HashMap<>();
            log.put("water_log_id", id);
            log.put("user_id", userId);
            log.put("glass_count", glasses);
            log.put("bottle_count", bottles);
            log.put("large_bottle_count", largeBtl);
            log.put("total_water_ml", total);
            log.put("date", dateStr);
            log.put("time", time);
            log.put("created_at", FieldValue.serverTimestamp());

            db.collection("water_logs").document(id).set(log);
        }
    }

    // ── Sleep Logs ────────────────────────────────────────────────────────
    private static void seedSleepLog(FirebaseFirestore db, String userId, String dateStr) {
        // 300-540 minutes (5-9 hours)
        int durationMins = 300 + RNG.nextInt(241);

        Map<String, Object> log = new HashMap<>();
        log.put("user_id", userId);
        log.put("duration_minutes", durationMins);
        log.put("date", dateStr);
        log.put("time", "07:00");
        log.put("created_at", FieldValue.serverTimestamp());

        db.collection("sleep_logs").add(log);
    }

    // ── Weight Logs ───────────────────────────────────────────────────────
    private static void seedWeightLog(FirebaseFirestore db, String userId, String dateStr, Calendar cal) {
        // Gradual trend from 70 down to ~67 over 3 months
        long dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        // Use a simple linear descent with noise
        double baseWeight;
        if (year == 2025) {
            // Dec 2025: days 335-365
            baseWeight = 70.0 - ((dayOfYear - 335) * 0.033);
        } else {
            // Jan-Feb 2026: days 1-59
            baseWeight = 69.0 - (dayOfYear * 0.033);
        }
        float weight = (float) (baseWeight + (RNG.nextGaussian() * 0.3));
        weight = Math.round(weight * 10) / 10.0f;

        Map<String, Object> log = new HashMap<>();
        log.put("user_id", userId);
        log.put("weight_kg", weight);
        log.put("date", dateStr);
        log.put("time", "08:00");
        log.put("created_at", FieldValue.serverTimestamp());

        db.collection("weight_logs").add(log);
    }

    // ── Food Logs ─────────────────────────────────────────────────────────
    private static void seedFoodLogs(FirebaseFirestore db, String userId, String dateStr, String[] foodIds) {
        // 2-4 food logs per day
        int logCount = 2 + RNG.nextInt(3);
        String[] mealTimes = {"08:30", "12:30", "16:00", "19:30"};

        for (int i = 0; i < logCount; i++) {
            int foodIdx = RNG.nextInt(foodIds.length);
            int qty = 1 + RNG.nextInt(3);
            int baseCal = Integer.parseInt(FOODS[foodIdx][1]);
            int totalCal = baseCal * qty;

            Map<String, Object> log = new HashMap<>();
            log.put("user_id", userId);
            log.put("food_id", foodIds[foodIdx]);
            log.put("quantity_selected", qty);
            log.put("calories_logged", totalCal);
            log.put("date", dateStr);
            log.put("time", mealTimes[i % mealTimes.length]);
            log.put("created_at", FieldValue.serverTimestamp());

            db.collection("food_logs").add(log);
        }
    }

    // ── Activity Logs ─────────────────────────────────────────────────────
    private static void seedActivityLog(FirebaseFirestore db, String userId, String dateStr) {
        // 60% chance of activity on any given day
        if (RNG.nextDouble() > 0.6) return;

        int actIdx = RNG.nextInt(ACTIVITIES.length);
        double weight = 68.0;
        int durationMins = 20 + RNG.nextInt(41); // 20-60 min
        double met = MET_VALUES[actIdx];
        long caloriesBurned = Math.round((met * weight * durationMins) / 60.0);

        String time = String.format(Locale.US, "%02d:%02d", 6 + RNG.nextInt(3), RNG.nextInt(60));

        Map<String, Object> log = new HashMap<>();
        log.put("user_id", userId);
        log.put("activity_name", ACTIVITIES[actIdx]);
        log.put("weight", weight);
        log.put("duration_minutes", durationMins);
        log.put("calories_burned", caloriesBurned);
        log.put("date", dateStr);
        log.put("time", time);
        log.put("created_at", FieldValue.serverTimestamp());

        db.collection("activity_logs").add(log);
    }

    // ── Step Logs ─────────────────────────────────────────────────────────
    private static void seedStepLog(FirebaseFirestore db, String userId, String dateStr) {
        int locIdx = RNG.nextInt(LOCATIONS.length);
        int steps = 2000 + RNG.nextInt(10001); // 2000-12000
        double distKm = steps * 0.000762; // ~0.762m per step
        distKm = Math.round(distKm * 100) / 100.0;

        String id = UUID.randomUUID().toString();
        String time = String.format(Locale.US, "%02d:%02d", 9 + RNG.nextInt(6), RNG.nextInt(60));

        Map<String, Object> session = new HashMap<>();
        session.put("manual_step_log_id", id);
        session.put("user_id", userId);
        session.put("start_location_name", LOCATIONS[locIdx][0]);
        session.put("destination_location_name", LOCATIONS[locIdx][1]);
        session.put("distance_km", distKm);
        session.put("estimated_steps", steps);
        session.put("date", dateStr);
        session.put("time", time);
        session.put("created_at", FieldValue.serverTimestamp());

        db.collection("manual_step_logs").document(id).set(session);
    }
}
