package com.emergsaver.mediquick;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.emergsaver.mediquick.databinding.FragmentEditProfileDialogBinding;

public class EditProfileDialog extends DialogFragment {

    // 뷰 바인딩 선언
    private FragmentEditProfileDialogBinding binding;

    // 콜백 인터페이스 정의 (팝업과 메인 화면 간 데이터 통신을 위함)
    public interface OnProfileEditListener {
        void onProfileEdited(String birthdate, String bloodType, String emergencyContact);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // fragment_edit_profile_dialog.xml을 인플레이트하고 뷰 바인딩 초기화
        binding = FragmentEditProfileDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 스피너 데이터 설정
        setupSpinners();

        // '확인' 버튼 클릭 리스너 설정
        binding.btnConfirm.setOnClickListener(v -> {
            // 1. 입력된 데이터 가져오기
            String birthdate = binding.spinnerYear.getSelectedItem().toString() + "년 "
                    + binding.spinnerMonth.getSelectedItem().toString() + "월 "
                    + binding.spinnerDay.getSelectedItem().toString() + "일";

            String bloodType = binding.spinnerBloodType.getSelectedItem().toString();

            String emergencyContact = binding.etContact1.getText().toString() + "-"
                    + binding.etContact2.getText().toString() + "-"
                    + binding.etContact3.getText().toString();

            // 2. 콜백 리스너를 통해 데이터 전달
            OnProfileEditListener listener = null;
            if (getTargetFragment() instanceof OnProfileEditListener) {
                listener = (OnProfileEditListener) getTargetFragment();
            } else if (getActivity() instanceof OnProfileEditListener) {
                listener = (OnProfileEditListener) getActivity();
            }

            if (listener != null) {
                listener.onProfileEdited(birthdate, bloodType, emergencyContact);
            }

            // 3. 팝업 닫기
            dismiss();
        });
    }

    private void setupSpinners() {
        // 년도 스피너 설정 (현재 년도부터 1900년까지)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        ArrayList<String> years = new ArrayList<>();
        for (int i = currentYear; i >= 1900; i--) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerYear.setAdapter(yearAdapter);

        // 월 스피너 설정 (1월부터 12월까지)
        ArrayList<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            months.add(String.valueOf(i));
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMonth.setAdapter(monthAdapter);

        // 일 스피너 설정 (1일부터 31일까지)
        ArrayList<String> days = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            days.add(String.valueOf(i));
        }
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDay.setAdapter(dayAdapter);

        // 혈액형 스피너 설정 (A, B, O, AB)
        String[] bloodTypes = {"A형", "B형", "O형", "AB형"};
        ArrayAdapter<String> bloodTypeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, bloodTypes);
        bloodTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBloodType.setAdapter(bloodTypeAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지를 위해 바인딩 해제
    }
}