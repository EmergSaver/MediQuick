package com.emergsaver.mediquick;

//  Firebase 및 Glide 라이브러리 import
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import android.widget.Toast;
import android.content.Context;
import android.content.SharedPreferences;


import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import java.util.ArrayList;
import androidx.fragment.app.DialogFragment;


// OnProfileEditListener 인터페이스를 구현합니다.
public class ProfileFragment extends Fragment {

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
    // 프로필 이름 표시용 TextView
    private TextView tvName;

    // Firestore 및 사용자 ID 변수 선언
    private FirebaseFirestore db;
    private String userUid;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // : MainActivity로부터 전달받은 UID를 가져옵니다.
        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }

        //  Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance();

        // "requestKey"라는 키로 결과를 받을 리스너를 등록합니다.
        // 중복된 리스너 코드를 하나만 남기고 삭제했습니다.
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

        // 프로필 사진/이름 팝업에서 결과를 받을 리스너를 추가합니다.
        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            String updatedPhotoUri = result.getString("updatedPhotoUri");

            if (updatedName != null) {
                if (tvName != null) { //  tvName이 이미 초기화되었다고 가정
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

        //  bindViews 메소드를 호출하여 뷰를 한 곳에서 초기화
        bindViews(view);

        //  Firestore에서 사용자 데이터를 불러와 UI를 업데이트하는 메소드 호출
        loadUserProfileData();

        // '알러지 정보 수정' 버튼 이벤트 (기존과 동일)
        btnAllergy.setOnClickListener(v -> {
            AllergyDialog dialog = new AllergyDialog();
            dialog.show(getParentFragmentManager(), "allergyDialog");
        });

        // '개인정보 수정' 버튼 이벤트 (팝업 호출로 변경)
        btnProfile.setOnClickListener(v -> {
            EditProfileDialog dialog = new EditProfileDialog();
            dialog.show(getParentFragmentManager(), "editProfileDialog");
        });

        // '프로필 수정' 버튼 이벤트 (기존과 동일)
        btnUploadphoto.setOnClickListener(v -> {
            EditProfilePhotoDialog dialog = new EditProfilePhotoDialog();
            dialog.show(getParentFragmentManager(), "editProfilePhotoDialog");
        });

        return view;
    }

    //  뷰들을 한 곳에서 초기화하는 메소드
    private void bindViews(View view) {
        btnAllergy = view.findViewById(R.id.btn_allergy);
        btnProfile = view.findViewById(R.id.btn_profile);
        btnUploadphoto = view.findViewById(R.id.btn_upload_photo);

        etDob = view.findViewById(R.id.et_dob);
        etEmergencyContact = view.findViewById(R.id.et_emergency_contact);
        etBloodType = view.findViewById(R.id.et_blood_type);

        etFoodAllergy1 = view.findViewById(R.id.et_food_allergy_1);
        etFoodAllergy2 = view.findViewById(R.id.et_food_allergy_2);
        etFoodAllergy3 = view.findViewById(R.id.et_food_allergy_3);
        etDrugAllergy1 = view.findViewById(R.id.et_drug_allergy_1);
        etDrugAllergy2 = view.findViewById(R.id.et_drug_allergy_2);
        etDrugAllergy3 = view.findViewById(R.id.et_drug_allergy_3);

        ivProfileImage = view.findViewById(R.id.profile_image);
        // Assuming your profile layout has a TextView for the name
        tvName = view.findViewById(R.id.tv_name);
    }

    //  Firestore에서 사용자 데이터를 가져와 UI를 업데이트하는 메소드
    private void loadUserProfileData() {
        if (userUid == null) {
            Toast.makeText(getContext(), "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String birth = documentSnapshot.getString("birth");
                        String bloodType = documentSnapshot.getString("bloodType");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl"); // 이미지 URL 필드

                        // UI 업데이트
                        if (tvName != null) tvName.setText(name);
                        if (etDob != null) etDob.setText(birth);
                        if (etBloodType != null) etBloodType.setText(bloodType);

                        // Glide 라이브러리를 사용해 URL에서 이미지를 로드
                        if (profileImageUrl != null && ivProfileImage != null && getContext() != null) {
                            Glide.with(getContext()).load(profileImageUrl).into(ivProfileImage);
                        }
                    } else {
                        // Firestore에 사용자 정보가 없을 경우 (예: 첫 로그인)
                        Toast.makeText(getContext(), "프로필 정보가 없습니다. 새로 등록해주세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "프로필 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    // 팝업에서 데이터가 수정되면 호출되는 콜백 메서드
//    @Override
//    public void onProfileEdited(String birthdate, String bloodType, String emergencyContact) {
//        // 전달받은 데이터를 UI의 EditText에 반영
//        etDob.setText(birthdate);
//        etEmergencyContact.setText(emergencyContact);
//        etBloodType.setText(bloodType);
//    }
}