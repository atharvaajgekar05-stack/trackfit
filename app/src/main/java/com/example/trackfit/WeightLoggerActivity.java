package com.example.trackfit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class WeightLoggerActivity extends AppCompatActivity {

    private EditText etWeight;
    private Button btnWeightUnit, btnSelectDate, btnSelectTime, btnSaveWeight;
    private TextView tvSelectedDate;
    private LinearLayout llSuccessMessage;
    private ImageButton btnBack;
    
    private String selectedUnit = "kg";
    private String selectedDate = "";
    private String selectedTime = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_logger);
        
        // Initialize views
        etWeight = findViewById(R.id.etWeight);
        btnWeightUnit = findViewById(R.id.btnWeightUnit);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSaveWeight = findViewById(R.id.btnSaveWeight);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        llSuccessMessage = findViewById(R.id.llSuccessMessage);
        btnBack = findViewById(R.id.btnBack);
        
        // Set default date to today
        updateDateDisplay(Calendar.getInstance());
        
        // Set default time to current time
        updateTimeDisplay(Calendar.getInstance());
        
        // Weight unit toggle
        btnWeightUnit.setOnClickListener(v -> toggleWeightUnit());
        
        // Date picker
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        
        // Time picker
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        
        // Save weight button
        btnSaveWeight.setOnClickListener(v -> saveWeight());
        
        // Back button
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void toggleWeightUnit() {
        if (selectedUnit.equals("kg")) {
            selectedUnit = "lb";
            btnWeightUnit.setText("lb");
        } else {
            selectedUnit = "kg";
            btnWeightUnit.setText("kg");
        }
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                updateDateDisplay(selectedCalendar);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
    
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedCalendar.set(Calendar.MINUTE, minute);
                updateTimeDisplay(selectedCalendar);
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        );
        dialog.show();
    }
    
    private void updateDateDisplay(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        selectedDate = String.format("%02d/%02d/%04d", day, month, year);
        btnSelectDate.setText(selectedDate);
    }
    
    private void updateTimeDisplay(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        selectedTime = String.format("%02d:%02d", hour, minute);
        btnSelectTime.setText(selectedTime);
    }
    
    private void saveWeight() {
        String weight = etWeight.getText().toString().trim();
        
        // Validation
        if (weight.isEmpty()) {
            Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: Save to database or Firebase
        // For now, show success message
        showSuccessMessage();
        
        // Clear inputs after 2 seconds
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            clearInputs();
            finish();
        }, 2000);
    }
    
    private void showSuccessMessage() {
        llSuccessMessage.setVisibility(android.view.View.VISIBLE);
    }
    
    private void clearInputs() {
        etWeight.setText("");
        selectedUnit = "kg";
        btnWeightUnit.setText("kg");
        updateDateDisplay(Calendar.getInstance());
        updateTimeDisplay(Calendar.getInstance());
        llSuccessMessage.setVisibility(android.view.View.GONE);
    }
}
