package com.emergsaver.mediquick;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

// OnProfileEditListener 인터페이스를 구현합니다.
public class ProfileFragment extends DialogFragment implements EditProfileDialog.OnProfileEditListener {

    private Button btnAllergy;
    private Button btnProfile;
    private Button btnUploadphoto;

    // 개인정보를 표시할 EditText
    private EditText etDob; // 생년월일
    private EditText etEmergencyContact; // 비상 연락처
    private EditText etBloodType; // 혈액형

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // fragment_profile.xml 연결
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        btnAllergy = view.findViewById(R.id.btn_allergy);
        btnProfile = view.findViewById(R.id.btn_profile);
        btnUploadphoto = view.findViewById(R.id.btn_upload_photo);

        // 개인 정보 표시 EditText
        etDob = view.findViewById(R.id.et_dob);
        etEmergencyContact = view.findViewById(R.id.et_emergency_contact);
        etBloodType = view.findViewById(R.id.et_blood_type);

        // '알러지 정보 수정' 버튼 이벤트 (기존과 동일)
        btnAllergy.setOnClickListener(v -> {
            // Intent intent = new Intent(getActivity(), AllergyActivity.class);
            // startActivity(intent);
        });

        // '개인정보 수정' 버튼 이벤트 (팝업 호출로 변경)

        btnProfile.setOnClickListener(v -> {
            EditProfileDialog dialog = new EditProfileDialog();
            dialog.show(getChildFragmentManager(), "editProfileDialog");
        });

        // '프로필 수정' 버튼 이벤트 (기존과 동일)
        btnUploadphoto.setOnClickListener(v -> {
            // Intent intent = new Intent(getActivity(), UploadPhotoActivity.class);
            // startActivity(intent);
        });

        return view;
    }

    // 팝업에서 데이터가 수정되면 호출되는 콜백 메서드
    @Override
    public void onProfileEdited(String birthdate, String bloodType, String emergencyContact) {
        // 전달받은 데이터를 UI의 EditText에 반영
        etDob.setText(birthdate);
        etEmergencyContact.setText(emergencyContact);
        etBloodType.setText(bloodType);
    }
}
