package com.emergsaver.mediquick;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.emergsaver.mediquick.databinding.FragmentEditProfileDialogBinding;
//  Firebase Firestore import
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;

public class EditProfileDialog extends DialogFragment {

    private FragmentEditProfileDialogBinding binding;

    // Firebase Firestore 인스턴스
    private FirebaseFirestore db;
    // 사용자 UID를 저장할 변수
    private String userUid;

    /**
     * DialogFragment를 생성하고 UID를 전달하는 팩토리 메소드 (권장)
     */
    public static EditProfileDialog newInstance(String userUid) {
        EditProfileDialog dialog = new EditProfileDialog();
        Bundle args = new Bundle();
        args.putString("userUid", userUid);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 뷰 바인딩 인스턴스가 유효한지 확인
        if (binding == null) {
            return; // 뷰 바인딩이 null이면 아무것도 하지 않고 함수 종료
        }

        // Firebase 인스턴스 초기화
        db = FirebaseFirestore.getInstance();

        // Bundle에서 UID 가져오기
        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }

        // 스피너 데이터 설정
        setupSpinners();

        // Firebase에서 기존 데이터를 불러와 UI에 표시
        loadUserDataFromFirebase();

        binding.btnConfirm.setOnClickListener(v -> {
            // 1. 입력된 데이터 가져오기
            String birthdate = binding.spinnerYear.getSelectedItem().toString() + "년 "
                    + binding.spinnerMonth.getSelectedItem().toString() + "월 "
                    + binding.spinnerDay.getSelectedItem().toString() + "일";

            String bloodType = binding.spinnerBloodType.getSelectedItem().toString();

            //  성별 RadioButton에서 선택된 값 가져오기
            String gender = "";
            int selectedGenderId = binding.radioGroupGender.getCheckedRadioButtonId();
            if (selectedGenderId == R.id.radio_male) {
                gender = "남성";
            } else if (selectedGenderId == R.id.radio_female) {
                gender = "여성";
            }

            String emergencyContact = binding.etContact1.getText().toString() + "-"
                    + binding.etContact2.getText().toString() + "-"
                    + binding.etContact3.getText().toString();

            // 수정된 정보를 Firebase에 저장
            saveUserDataToFirebase(birthdate, bloodType, emergencyContact, gender);

            // 2. Fragment Result를 통해 데이터 전달 (UI 즉시 업데이트용)
            Bundle result = new Bundle();
            result.putString("birthdate", birthdate);
            result.putString("bloodType", bloodType);
            result.putString("emergencyContact", emergencyContact);
            result.putString("gender", gender); //  성별 정보 전달
            getParentFragmentManager().setFragmentResult("requestKey", result);

            // 3. 팝업 닫기
            dismiss();
        });
    }

    /**
     * Firebase에서 사용자 정보를 불러와 EditText와 Spinner, RadioGroup을 채웁니다.
     */
    private void loadUserDataFromFirebase() {
        if (userUid == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "사용자 UID가 없습니다. 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        db.collection("users").document(userUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String birth = documentSnapshot.getString("birth");
                        String bloodType = documentSnapshot.getString("bloodType");
                        String emergencyContact = documentSnapshot.getString("emergencyContact");
                        String gender = documentSnapshot.getString("gender"); //  성별 정보 가져오기

                        // 생년월일 파싱 및 스피너 설정
                        if (birth != null && !birth.isEmpty()) {
                            try {
                                String[] parts = birth.split("년 |월 |일");
                                int year = Integer.parseInt(parts[0]);
                                int month = Integer.parseInt(parts[1]);
                                int day = Integer.parseInt(parts[2]);

                                // 스피너에서 해당 값의 위치를 찾아 설정
                                setSpinnerSelection(binding.spinnerYear, String.valueOf(year));
                                setSpinnerSelection(binding.spinnerMonth, String.valueOf(month));
                                setSpinnerSelection(binding.spinnerDay, String.valueOf(day));
                            } catch (Exception e) {
                                // 파싱 오류 시 처리
                            }
                        }

                        // 혈액형 스피너 설정
                        if (bloodType != null) {
                            setSpinnerSelection(binding.spinnerBloodType, bloodType);
                        }

                        // 비상 연락처 파싱 및 EditText 설정
                        if (emergencyContact != null && !emergencyContact.isEmpty()) {
                            String[] parts = emergencyContact.split("-");
                            if (parts.length == 3) {
                                binding.etContact1.setText(parts[0]);
                                binding.etContact2.setText(parts[1]);
                                binding.etContact3.setText(parts[2]);
                            }
                        }

                        // 성별 RadioButton 설정
                        if (gender != null) {
                            if (gender.equals("남성")) {
                                binding.radioMale.setChecked(true);
                            } else if (gender.equals("여성")) {
                                binding.radioFemale.setChecked(true);
                            }
                        }
                    }
                });
    }

    /**
     * 스피너에서 특정 값의 인덱스를 찾아 설정하는 헬퍼 메소드
     */
    private void setSpinnerSelection(android.widget.Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    /**
     * 수정된 사용자 정보를 Firebase에 저장합니다.
     */
    private void saveUserDataToFirebase(String birthdate, String bloodType, String emergencyContact, String gender) {
        if (userUid == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "사용자 UID가 없어 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("birth", birthdate);
        updates.put("bloodType", bloodType);
        updates.put("emergencyContact", emergencyContact);
        updates.put("gender", gender); //  성별 정보도 저장

        db.collection("users").document(userUid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "개인정보가 성공적으로 업데이트되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "업데이트 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSpinners() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        ArrayList<String> years = new ArrayList<>();
        years.add("년"); // 초기값 변경
        for (int i = currentYear; i >= 1900; i--) years.add(String.valueOf(i));
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerYear.setAdapter(yearAdapter);
        binding.spinnerYear.setSelection(0);

        ArrayList<String> months = new ArrayList<>();
        months.add("월"); // 초기값 변경
        for (int i = 1; i <= 12; i++) months.add(String.valueOf(i));
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMonth.setAdapter(monthAdapter);
        binding.spinnerMonth.setSelection(0);

        ArrayList<String> days = new ArrayList<>();
        days.add("일"); // 초기값 변경
        for (int i = 1; i <= 31; i++) days.add(String.valueOf(i));
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDay.setAdapter(dayAdapter);
        binding.spinnerDay.setSelection(0);

        String[] bloodTypes = {"A형", "B형", "O형", "AB형"};
        ArrayAdapter<String> bloodTypeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, bloodTypes);
        bloodTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBloodType.setAdapter(bloodTypeAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}