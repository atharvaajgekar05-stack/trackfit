package com.example.trackfit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FabMenuBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_fab_menu, container, false);

        // BMI Calculator
        view.findViewById(R.id.card_bmi).setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(requireActivity(), BmiCalculatorActivity.class);
            startActivity(intent);
        });

        // Unit Convertor
        view.findViewById(R.id.card_unit).setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(requireActivity(), UnitConverterActivity.class);
            startActivity(intent);
        });

        // Activity
        view.findViewById(R.id.card_activity).setOnClickListener(v ->
                showToast("Activity"));

        // Food
        view.findViewById(R.id.card_food).setOnClickListener(v ->
                showToast("Food"));

        // Water
        view.findViewById(R.id.card_water).setOnClickListener(v ->
                showToast("Water"));

        // Sleep
        view.findViewById(R.id.card_sleep).setOnClickListener(v ->
                showToast("Sleep"));

        // Weight
        view.findViewById(R.id.card_weight).setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(requireActivity(), WeightLoggerActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message + " coming soon!", Toast.LENGTH_SHORT).show();
    }
}
