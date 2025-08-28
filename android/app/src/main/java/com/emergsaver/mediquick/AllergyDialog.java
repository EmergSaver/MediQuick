package com.emergsaver.mediquick;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllergyDialog extends DialogFragment {

    private static final String TAG = "AllergyDialog";

    private GridLayout gridLayout;
    private EditText etDrugSideEffect;
    private Button btnAddDrugSideEffect;
    private Button btnDeleteDrugSideEffect; // ✨ 추가: 삭제 버튼
    private TextView tvRegisteredAllergies;
    private Button btnConfirmAllergy;

    private List<String> registeredDrugAllergies = new ArrayList<>();
    private FirebaseFirestore db;
    private String userUid;

    public static AllergyDialog newInstance(String userUid) {
        AllergyDialog dialog = new AllergyDialog();
        Bundle args = new Bundle();
        args.putString("userUid", userUid);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_allergy_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridLayout = view.findViewById(R.id.grid_food_allergies);
        etDrugSideEffect = view.findViewById(R.id.et_drug_side_effect);
        btnAddDrugSideEffect = view.findViewById(R.id.btn_add_drug_side_effect);
        btnDeleteDrugSideEffect = view.findViewById(R.id.btn_delete_drug_side_effect); // ✨ 추가: 삭제 버튼 초기화
        tvRegisteredAllergies = view.findViewById(R.id.tv_registered_allergies);
        btnConfirmAllergy = view.findViewById(R.id.btn_confirm_allergy);

        loadAllergyData();

        btnAddDrugSideEffect.setOnClickListener(v -> {
            String drug = etDrugSideEffect.getText().toString().trim();
            if (!drug.isEmpty()) {
                if (!registeredDrugAllergies.contains(drug)) {
                    registeredDrugAllergies.add(drug);
                    updateRegisteredAllergiesText();
                    etDrugSideEffect.setText("");
                } else {
                    Toast.makeText(getContext(), "이미 등록된 약물입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ✨ 추가: 약물 부작용 목록을 길게 누르면 삭제
        tvRegisteredAllergies.setOnLongClickListener(v -> {
            String text = tvRegisteredAllergies.getText().toString();
            if (!text.equals("현재 등록된 정보가 없습니다.")) {
                // 삭제할 항목을 선택하도록 사용자에게 안내
                Toast.makeText(getContext(), "삭제할 항목을 입력창에 다시 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // ✨ 추가: 삭제 버튼 클릭 리스너
        btnDeleteDrugSideEffect.setOnClickListener(v -> {
            String drugToDelete = etDrugSideEffect.getText().toString().trim();
            if (!drugToDelete.isEmpty()) {
                if (registeredDrugAllergies.remove(drugToDelete)) {
                    Toast.makeText(getContext(), "'" + drugToDelete + "'가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    updateRegisteredAllergiesText();
                    etDrugSideEffect.setText("");
                } else {
                    Toast.makeText(getContext(), "해당 약물은 목록에 없습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "삭제할 약물 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        btnConfirmAllergy.setOnClickListener(v -> saveAllergyData());
    }

    private void loadAllergyData() {
        if (userUid == null) {
            Log.e(TAG, "UID is null, cannot load data.");
            return;
        }

        db.collection("users").document(userUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> allergiesMap = (Map<String, Object>) documentSnapshot.get("allergies");

                        if (allergiesMap != null) {
                            Map<String, Boolean> foodAllergies = (Map<String, Boolean>) allergiesMap.get("foodAllergies");
                            if (foodAllergies != null) {
                                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                                    View child = gridLayout.getChildAt(i);
                                    if (child instanceof CheckBox) {
                                        CheckBox checkBox = (CheckBox) child;
                                        String idName = getResources().getResourceEntryName(checkBox.getId());
                                        Boolean isChecked = foodAllergies.get(idName);
                                        if (isChecked != null) {
                                            checkBox.setChecked(isChecked);
                                        }
                                    }
                                }
                            }

                            Object drugAllergies = allergiesMap.get("drugAllergies");
                            if (drugAllergies instanceof List) {
                                registeredDrugAllergies = (List<String>) drugAllergies;
                                updateRegisteredAllergiesText();
                            }
                        }
                    } else {
                        Log.d(TAG, "No allergy data found for user.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading allergy data", e);
                    Toast.makeText(getContext(), "알레르기 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveAllergyData() {
        if (userUid == null) {
            Toast.makeText(getContext(), "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        DocumentReference userDocRef = db.collection("users").document(userUid);

        Map<String, Boolean> foodAllergies = new HashMap<>();
        ArrayList<String> foodAllergyNamesForFragment = new ArrayList<>();

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;
                String idName = getResources().getResourceEntryName(checkBox.getId());
                boolean isChecked = checkBox.isChecked();

                foodAllergies.put(idName, isChecked);

                if (isChecked) {
                    foodAllergyNamesForFragment.add(checkBox.getText().toString());
                }
            }
        }

        Map<String, Object> allergyMap = new HashMap<>();
        allergyMap.put("foodAllergies", foodAllergies);
        allergyMap.put("drugAllergies", registeredDrugAllergies);

        userDocRef.update("allergies", allergyMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "알레르기 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();

                    Bundle result = new Bundle();
                    result.putStringArrayList("food_allergies", foodAllergyNamesForFragment);
                    result.putStringArrayList("drug_allergies", new ArrayList<>(registeredDrugAllergies));
                    getParentFragmentManager().setFragmentResult("allergyRequestKey", result);

                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "정보 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving allergy data", e);
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