package com.emergsaver.mediquick;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AllergyDialog extends DialogFragment {

    private GridLayout gridLayout;
    private EditText etDrugSideEffect;
    private Button btnAddDrugSideEffect;
    private TextView tvRegisteredAllergies;
    private Button btnConfirmAllergy;

    private List<String> registeredDrugAllergies = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_allergy_dialog, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridLayout = view.findViewById(R.id.grid_food_allergies);
        etDrugSideEffect = view.findViewById(R.id.et_drug_side_effect);
        btnAddDrugSideEffect = view.findViewById(R.id.btn_add_drug_side_effect);
        tvRegisteredAllergies = view.findViewById(R.id.tv_registered_allergies);
        btnConfirmAllergy = view.findViewById(R.id.btn_confirm_allergy);

        // '등록' 버튼 클릭 리스너
        btnAddDrugSideEffect.setOnClickListener(v -> {
            String drug = etDrugSideEffect.getText().toString().trim();
            if (!drug.isEmpty()) {
                registeredDrugAllergies.add(drug);
                updateRegisteredAllergiesText();
                etDrugSideEffect.setText(""); // 입력 필드 초기화
            }
        });

        // '확인' 버튼 클릭 리스너
        btnConfirmAllergy.setOnClickListener(v -> {
            // 1. 선택된 음식 알레르기 목록을 가져옵니다.
            List<String> selectedFoodAllergies = new ArrayList<>();
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                View child = gridLayout.getChildAt(i);
                if (child instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) child;
                    if (checkBox.isChecked()) {
                        selectedFoodAllergies.add(checkBox.getText().toString());
                    }
                }
            }

            // 2. 결과를 Bundle에 담아 FragmentResult API로 전달합니다.
            Bundle result = new Bundle();
            result.putStringArrayList("food_allergies", new ArrayList<>(selectedFoodAllergies));
            result.putStringArrayList("drug_allergies", new ArrayList<>(registeredDrugAllergies));

            getParentFragmentManager().setFragmentResult("allergyRequestKey", result);
            dismiss();
        });
    }

    private void updateRegisteredAllergiesText() {
        if (registeredDrugAllergies.isEmpty()) {
            tvRegisteredAllergies.setText("현재 등록된 정보가 없습니다.");
        } else {
            StringBuilder sb = new StringBuilder("등록된 약물 부작용:\n");
            for (String drug : registeredDrugAllergies) {
                sb.append("• ").append(drug).append("\n");
            }
            tvRegisteredAllergies.setText(sb.toString().trim());
        }
    }
}