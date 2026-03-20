package com.example.trackfit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class StepTrackingService extends Service implements SensorEventListener {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_STEPS = "EXTRA_STEPS";
    public static final String BROADCAST_STEPS = "com.example.trackfit.STEPS_UPDATED";

    private SensorManager sensorManager;
    private Sensor stepSensor;

    private int initialSteps = -1;
    private int currentSessionSteps = 0;
    private int accumulatedSteps = 0; // Steps from previous tracking periods in the same session

    private static final String CHANNEL_ID = "StepTrackerChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                startTracking();
            } else if (ACTION_STOP.equals(action)) {
                stopTracking();
            }
        }
        return START_STICKY;
    }

    private void startTracking() {
        startForeground(1, getNotification("Tracking Active"));
        if (sensorManager != null && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        
        // Restore accumulated steps if resumed
        SharedPreferences prefs = getSharedPreferences("TrackFitPrefs", MODE_PRIVATE);
        accumulatedSteps = prefs.getInt("saved_session_steps", 0);
        initialSteps = -1; // Need a new baseline for this run
    }

    private void stopTracking() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Save current total back to prefs
        SharedPreferences prefs = getSharedPreferences("TrackFitPrefs", MODE_PRIVATE);
        prefs.edit().putInt("saved_session_steps", accumulatedSteps + currentSessionSteps).apply();
        
        stopForeground(true);
        stopSelf();
    }

    private Notification getNotification(String text) {
        Intent notificationIntent = new Intent(this, LiveStepTrackingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TrackFit Step Tracker")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_steps) // Make sure this exists, or use ic_activity
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Tracker Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalStepsSinceBoot = (int) event.values[0];

            if (initialSteps == -1) {
                initialSteps = totalStepsSinceBoot;
            }

            currentSessionSteps = totalStepsSinceBoot - initialSteps;
            int displaySteps = accumulatedSteps + currentSessionSteps;

            // Broadcast to activity
            Intent intent = new Intent(BROADCAST_STEPS);
            intent.putExtra(EXTRA_STEPS, displaySteps);
            sendBroadcast(intent);
            
            // Save dynamically in case app killed
            SharedPreferences prefs = getSharedPreferences("TrackFitPrefs", MODE_PRIVATE);
            prefs.edit().putInt("saved_session_steps", displaySteps).apply();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for step counter
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not bound service
    }
}
