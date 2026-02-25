package com.example.trackfit;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

public class UnitConverterActivity extends AppCompatActivity {

    // Weight Converter
    private EditText etWeightInput;
    private Spinner spinnerWeightFrom, spinnerWeightTo;
    private TextView tvWeightResult;

    // Height Converter
    private EditText etHeightInput;
    private Spinner spinnerHeightFrom, spinnerHeightTo;
    private TextView tvHeightResult;

    // Distance Converter
    private EditText etDistanceInput;
    private Spinner spinnerDistanceFrom, spinnerDistanceTo;
    private TextView tvDistanceResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_unit_converter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        initializeViews();

        // Setup Spinners
        setupWeightSpinners();
        setupHeightSpinners();
        setupDistanceSpinners();

        // Setup Text Listeners
        setupTextListeners();

        // Setup Back Button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        // Weight
        etWeightInput = findViewById(R.id.etWeightInput);
        spinnerWeightFrom = findViewById(R.id.spinnerWeightFrom);
        spinnerWeightTo = findViewById(R.id.spinnerWeightTo);
        tvWeightResult = findViewById(R.id.tvWeightResult);

        // Height
        etHeightInput = findViewById(R.id.etHeightInput);
        spinnerHeightFrom = findViewById(R.id.spinnerHeightFrom);
        spinnerHeightTo = findViewById(R.id.spinnerHeightTo);
        tvHeightResult = findViewById(R.id.tvHeightResult);

        // Distance
        etDistanceInput = findViewById(R.id.etDistanceInput);
        spinnerDistanceFrom = findViewById(R.id.spinnerDistanceFrom);
        spinnerDistanceTo = findViewById(R.id.spinnerDistanceTo);
        tvDistanceResult = findViewById(R.id.tvDistanceResult);
    }

    private void setupWeightSpinners() {
        String[] weightUnits = {"kg", "lb"};
        ArrayAdapter<String> adapter = createSpinnerAdapter(weightUnits);
        spinnerWeightFrom.setAdapter(adapter);
        spinnerWeightTo.setAdapter(adapter);
        spinnerWeightTo.setSelection(1); // Default to lb
    }

    private void setupHeightSpinners() {
        String[] heightUnits = {"cm", "inches", "feet"};
        ArrayAdapter<String> adapter = createSpinnerAdapter(heightUnits);
        spinnerHeightFrom.setAdapter(adapter);
        spinnerHeightTo.setAdapter(adapter);
        spinnerHeightTo.setSelection(1); // Default to inches
    }

    private void setupDistanceSpinners() {
        String[] distanceUnits = {"km", "miles", "meters"};
        ArrayAdapter<String> adapter = createSpinnerAdapter(distanceUnits);
        spinnerDistanceFrom.setAdapter(adapter);
        spinnerDistanceTo.setAdapter(adapter);
        spinnerDistanceTo.setSelection(1); // Default to miles
    }

    private ArrayAdapter<String> createSpinnerAdapter(String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.parseColor("#666666"));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void setupTextListeners() {
        etWeightInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                convertWeight();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etHeightInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                convertHeight();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etDistanceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                convertDistance();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Update results when spinners change
        spinnerWeightFrom.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                convertWeight();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerWeightTo.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                convertWeight();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerHeightFrom.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                convertHeight();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerHeightTo.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                convertHeight();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerDistanceFrom.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                convertDistance();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerDistanceTo.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                convertDistance();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void convertWeight() {
        String input = etWeightInput.getText().toString();
        if (input.isEmpty()) {
            tvWeightResult.setText("");
            return;
        }

        double value = Double.parseDouble(input);
        String fromUnit = spinnerWeightFrom.getSelectedItem().toString();
        String toUnit = spinnerWeightTo.getSelectedItem().toString();

        double result = convertWeightValue(value, fromUnit, toUnit);
        tvWeightResult.setText(String.format("%.2f %s", result, toUnit));
    }

    private double convertWeightValue(double value, String fromUnit, String toUnit) {
        // Convert to kg first
        double inKg;
        if (fromUnit.equals("kg")) {
            inKg = value;
        } else { // lb
            inKg = value / 2.2046;
        }

        // Convert from kg to target unit
        if (toUnit.equals("kg")) {
            return inKg;
        } else { // lb
            return inKg * 2.2046;
        }
    }

    private void convertHeight() {
        String input = etHeightInput.getText().toString();
        if (input.isEmpty()) {
            tvHeightResult.setText("");
            return;
        }

        double value = Double.parseDouble(input);
        String fromUnit = spinnerHeightFrom.getSelectedItem().toString();
        String toUnit = spinnerHeightTo.getSelectedItem().toString();

        String result = convertHeightValue(value, fromUnit, toUnit);
        tvHeightResult.setText(result);
    }

    private String convertHeightValue(double value, String fromUnit, String toUnit) {
        // Convert to cm first
        double inCm;
        if (fromUnit.equals("cm")) {
            inCm = value;
        } else if (fromUnit.equals("inches")) {
            inCm = value * 2.54;
        } else { // feet
            inCm = value * 30.48;
        }

        // Convert from cm to target unit
        if (toUnit.equals("cm")) {
            return String.format("%.0f cm", inCm);
        } else if (toUnit.equals("inches")) {
            double inches = inCm / 2.54;
            return String.format("%.1f in", inches);
        } else { // feet
            double feet = inCm / 30.48;
            int wholeFeeet = (int) feet;
            int inches = (int) ((feet - wholeFeeet) * 12);
            return String.format("%d'%d\"", wholeFeeet, inches);
        }
    }

    private void convertDistance() {
        String input = etDistanceInput.getText().toString();
        if (input.isEmpty()) {
            tvDistanceResult.setText("");
            return;
        }

        double value = Double.parseDouble(input);
        String fromUnit = spinnerDistanceFrom.getSelectedItem().toString();
        String toUnit = spinnerDistanceTo.getSelectedItem().toString();

        double result = convertDistanceValue(value, fromUnit, toUnit);
        String unitLabel = toUnit.equals("meters") ? "m" : toUnit;
        tvDistanceResult.setText(String.format("%.2f %s", result, unitLabel));
    }

    private double convertDistanceValue(double value, String fromUnit, String toUnit) {
        // Convert to km first
        double inKm;
        if (fromUnit.equals("km")) {
            inKm = value;
        } else if (fromUnit.equals("miles")) {
            inKm = value / 0.621;
        } else { // meters
            inKm = value / 1000;
        }

        // Convert from km to target unit
        if (toUnit.equals("km")) {
            return inKm;
        } else if (toUnit.equals("miles")) {
            return inKm * 0.621;
        } else { // meters
            return inKm * 1000;
        }
    }
}
