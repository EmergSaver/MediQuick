package com.emergsaver.mediquick;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import java.util.ArrayList;
import androidx.fragment.app.DialogFragment;

// OnProfileEditListener 인터페이스를 구현합니다.
public class ProfileFragment extends Fragment {
//        implements EditProfileDialog.OnProfileEditListener

    private Button btnAllergy;
    private Button btnProfile;
    private Button btnUploadphoto;

    // 개인정보를 표시할 EditText
    private EditText etDob; // 생년월일
    private EditText etEmergencyContact; // 비상 연락처
    private EditText etBloodType; // 혈액형

    // 알러지 정보 표시용 EditText들
    private EditText etFoodAllergy1, etFoodAllergy2, etFoodAllergy3;
    private EditText etDrugAllergy1, etDrugAllergy2, etDrugAllergy3;

    // 프로필 이미지를 표시할 ImageView
    private ImageView ivProfileImage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // "requestKey"라는 키로 결과를 받을 리스너를 등록합니다.
        getParentFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                // EditProfileDialog로부터 받은 데이터를 EditText에 설정
                String birthdate = result.getString("birthdate");
                String bloodType = result.getString("bloodType");
                String emergencyContact = result.getString("emergencyContact");

                if (etDob != null) {
                    etDob.setText(birthdate);
                }
                if (etEmergencyContact != null) {
                    etEmergencyContact.setText(emergencyContact);
                }
                if (etBloodType != null) {
                    etBloodType.setText(bloodType);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                String birthdate = result.getString("birthdate");
                String bloodType = result.getString("bloodType");
                String emergencyContact = result.getString("emergencyContact");

                if (etDob != null) {
                    etDob.setText(birthdate);
                }
                if (etEmergencyContact != null) {
                    etEmergencyContact.setText(emergencyContact);
                }
                if (etBloodType != null) {
                    etBloodType.setText(bloodType);
                }
            }
        });

        // 알러지 팝업에서 결과를 받을 리스너
        getParentFragmentManager().setFragmentResultListener("allergyRequestKey", this, (requestKey, result) -> {
            ArrayList<String> foodAllergies = result.getStringArrayList("food_allergies");
            ArrayList<String> drugAllergies = result.getStringArrayList("drug_allergies");

            // 음식 알레르기 EditText 업데이트
            if (foodAllergies != null) {
                EditText[] foodEditTexts = {etFoodAllergy1, etFoodAllergy2, etFoodAllergy3};
                for (int i = 0; i < foodEditTexts.length; i++) {
                    if (i < foodAllergies.size() && foodEditTexts[i] != null) {
                        foodEditTexts[i].setText(foodAllergies.get(i));
                    } else if (foodEditTexts[i] != null) {
                        foodEditTexts[i].setText(""); // 남은 칸은 비움
                    }
                }
            }

            // 약물 알레르기 EditText 업데이트
            if (drugAllergies != null) {
                EditText[] drugEditTexts = {etDrugAllergy1, etDrugAllergy2, etDrugAllergy3};
                for (int i = 0; i < drugEditTexts.length; i++) {
                    if (i < drugAllergies.size() && drugEditTexts[i] != null) {
                        drugEditTexts[i].setText(drugAllergies.get(i));
                    } else if (drugEditTexts[i] != null) {
                        drugEditTexts[i].setText(""); // 남은 칸은 비움
                    }
                }
            }
        });

        //  프로필 사진/이름 팝업에서 결과를 받을 리스너를 추가합니다.
        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            String updatedPhotoUri = result.getString("updatedPhotoUri");

            if (updatedName != null) {
                TextView tvName = getView().findViewById(R.id.tv_name);
                if (tvName != null) {
                    tvName.setText(updatedName);
                }
            }

            if (updatedPhotoUri != null && ivProfileImage != null) {
                ivProfileImage.setImageURI(Uri.parse(updatedPhotoUri));
            }
        });
    }


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

        // 추가: 알러지 관련 EditText 초기화
        etFoodAllergy1 = view.findViewById(R.id.et_food_allergy_1);
        etFoodAllergy2 = view.findViewById(R.id.et_food_allergy_2);
        etFoodAllergy3 = view.findViewById(R.id.et_food_allergy_3);
        etDrugAllergy1 = view.findViewById(R.id.et_drug_allergy_1);
        etDrugAllergy2 = view.findViewById(R.id.et_drug_allergy_2);
        etDrugAllergy3 = view.findViewById(R.id.et_drug_allergy_3);

        // ivProfileImage를 초기화합니다.
        ivProfileImage = view.findViewById(R.id.profile_image);

        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            String updatedPhotoUri = result.getString("updatedPhotoUri");

            if (updatedName != null) {
                TextView tvName = getView().findViewById(R.id.tv_name);
                if (tvName != null) {
                    tvName.setText(updatedName);
                }
            }

            if (updatedPhotoUri != null && ivProfileImage != null) {
                ivProfileImage.setImageURI(Uri.parse(updatedPhotoUri));
            }
        });


        // '알러지 정보 수정' 버튼 이벤트 (기존과 동일)
        btnAllergy.setOnClickListener(v -> {
            // Intent intent = new Intent(getActivity(), AllergyActivity.class);
            // startActivity(intent);
            AllergyDialog dialog = new AllergyDialog();
            dialog.show(getParentFragmentManager(), "allergyDialog");
        });

        // '개인정보 수정' 버튼 이벤트 (팝업 호출로 변경)

        btnProfile.setOnClickListener(v -> {
            EditProfileDialog dialog = new EditProfileDialog();
            //  수정: 팝업창이 닫혔을 때 데이터를 받을 타겟 Fragment를 설정합니다.
//            dialog.setTargetFragment(ProfileFragment.this, 0);
            dialog.show(getParentFragmentManager(), "editProfileDialog");
        });

        // '프로필 수정' 버튼 이벤트 (기존과 동일)
        btnUploadphoto.setOnClickListener(v -> {
            // Intent intent = new Intent(getActivity(), UploadPhotoActivity.class);
            // startActivity(intent);
            // EditProfilePhotoDialog 팝업 띄우기
            EditProfilePhotoDialog dialog = new EditProfilePhotoDialog();
            dialog.show(getParentFragmentManager(), "editProfilePhotoDialog");
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



