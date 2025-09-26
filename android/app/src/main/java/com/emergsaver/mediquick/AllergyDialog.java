package com.emergsaver.mediquick;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;

public class AllergyDialog extends DialogFragment {

    private static final String TAG = "AllergyDialog";

    private LinearLayout gridLayout;
    private EditText etDrugSideEffect;
    private Button btnAddDrugSideEffect;
//    private Button btnDeleteDrugSideEffect; // 추가: 삭제 버튼
    private LinearLayout noInfo;
    private TextView tvWarnRegistedAllergy;
    private TextView tvRegisteredAllergies;
    private Button btnConfirmAllergy;
    private FlexboxLayout drugAllergyContainer;
    private ScrollView scrollView;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_allergy_dialog, container, false);

        drugAllergyContainer = view.findViewById(R.id.drug_allergy_container);
        noInfo = view.findViewById(R.id.no_info_linear); // 기존 "정보 없음" 레이아웃

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // 팝업 창의 가로와 세로 크기를 설정하는 코드
        if (getDialog() != null) {
            Dialog dialog = getDialog();
            Window window = dialog.getWindow();
            if (window != null) {

                window.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
                WindowManager.LayoutParams params = window.getAttributes();

                // 가로를 화면 전체 폭의 90%로 설정
                params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);

                // 높이를 WRAP_CONTENT로 설정 (내용에 맞게 자동 조절)
                params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.9);
//                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                window.setAttributes(params);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridLayout = view.findViewById(R.id.food_allergies_linear);
        etDrugSideEffect = view.findViewById(R.id.et_drug_side_effect);
        btnAddDrugSideEffect = view.findViewById(R.id.btn_add_drug_side_effect);
        noInfo = view.findViewById(R.id.no_info_linear);
        tvWarnRegistedAllergy = view.findViewById(R.id.warn_register_allergy);
        tvRegisteredAllergies = view.findViewById(R.id.registered_allergies);
        btnConfirmAllergy = view.findViewById(R.id.btn_confirm_allergy);
        scrollView = view.findViewById(R.id.allergy_scroll);

        loadAllergyData();

        btnAddDrugSideEffect.setOnClickListener(v -> {
            String drug = etDrugSideEffect.getText().toString().trim();
            if (!drug.isEmpty()) {
                if (!registeredDrugAllergies.contains(drug)) {
                    if(tvWarnRegistedAllergy.getVisibility() == View.VISIBLE)
                        tvWarnRegistedAllergy.setVisibility(View.GONE);
                    registeredDrugAllergies.add(drug);
                    // 동적 UI 추가
                    addDrugItem(drug);
                    etDrugSideEffect.setText("");
                } else {
                    tvWarnRegistedAllergy.setVisibility(View.VISIBLE);
                }
            }
        });

        // 추가: 약물 부작용 목록을 길게 누르면 삭제
        tvRegisteredAllergies.setOnLongClickListener(v -> {
            String text = tvRegisteredAllergies.getText().toString();
            if (!text.equals("현재 등록된 정보가 없습니다.")) {
                // 삭제할 항목을 선택하도록 사용자에게 안내
                Toast.makeText(getContext(), "삭제할 항목을 입력창에 다시 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
            return true;
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
                            // 음식 알레르기 체크박스 상태 적용
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

                            // 약물 부작용 리스트 적용
                            Object drugAllergies = allergiesMap.get("drugAllergies");
                            if (drugAllergies instanceof List) {
                                registeredDrugAllergies = (List<String>) drugAllergies;

                                if (!registeredDrugAllergies.isEmpty()) {
                                    noInfo.setVisibility(View.GONE);
                                    for (String drug : registeredDrugAllergies) {
                                        addDrugItem(drug); // 동적 UI 추가
                                    }
                                } else {
                                    noInfo.setVisibility(View.VISIBLE);
                                }
                            } else {
                                noInfo.setVisibility(View.VISIBLE);
                            }
                        } else {
                            noInfo.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d(TAG, "No allergy data found for user.");
                        noInfo.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading allergy data", e);
                    Toast.makeText(getContext(), "알레르기 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    noInfo.setVisibility(View.VISIBLE);
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

    private void addDrugItem(String drugName) {
        // "현재 등록된 정보 없음" 숨기기
        noInfo.setVisibility(View.GONE);

        // 아이템 전체 레이아웃 (가로 정렬)
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 8, 16, 8);

        FlexboxLayout.LayoutParams itemParams = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(8, 8, 8, 8);
        itemLayout.setLayoutParams(itemParams);

        // 동그란 이미지
        ImageView iv = new ImageView(getContext());
        iv.setImageResource(R.drawable.ic_circle);
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(40, 40);
        ivParams.setMargins(0, 10, 15, 10);
        iv.setLayoutParams(ivParams);

        // 텍스트
        TextView tv = new TextView(getContext());
        tv.setText(drugName);
        tv.setTextSize(17);
        tv.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tvParams.setMargins(15, 0, 15, 0);
        tv.setLayoutParams(tvParams);

        // 삭제 버튼 (X 아이콘)
        ImageButton btnDelete = new ImageButton(getContext());
        btnDelete.setImageResource(R.drawable.ic_cancel);

        // FlexboxLayout 안이므로 FlexboxLayout.LayoutParams 사용
        FlexboxLayout.LayoutParams btnParams =
                new FlexboxLayout.LayoutParams(dpToPx(24), dpToPx(24));
        btnDelete.setLayoutParams(btnParams);

        // 버튼 배경 제거 + 패딩 제거 + 아이콘 꽉 차게
        btnDelete.setBackground(null);
        btnDelete.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btnDelete.setPadding(10, 10, 0, 0);

        btnDelete.setOnClickListener(v -> {
            registeredDrugAllergies.remove(drugName);
            drugAllergyContainer.removeView(itemLayout);
            if (registeredDrugAllergies.isEmpty()) {
                noInfo.setVisibility(View.VISIBLE);
            }
        });

        // 순서대로 추가
        itemLayout.addView(iv);
        itemLayout.addView(tv);
        itemLayout.addView(btnDelete);

        drugAllergyContainer.addView(itemLayout);

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


}