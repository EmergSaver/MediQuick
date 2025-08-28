package com.emergsaver.mediquick;

// Firebase 및 Glide 라이브러리 import
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
import android.widget.ImageView;
import android.widget.TextView; // 수정: EditText 대신 TextView를 import 합니다.

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

    // 개인정보를 표시할 TextView
    // 수정: EditText 변수들을 TextView로 변경합니다.
    private TextView tvDob; // 생년월일
    private TextView tvEmergencyContact; // 비상 연락처
    private TextView tvBloodType; // 혈액형
    private TextView tvGender; // 성별

    // 알러지 정보 표시용 TextView들
    // 수정: EditText 변수들을 TextView로 변경합니다.
    private TextView tvFoodAllergy1, tvFoodAllergy2, tvFoodAllergy3;
    private TextView tvDrugAllergy1, tvDrugAllergy2, tvDrugAllergy3;

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

        // MainActivity로부터 전달받은 UID를 가져옵니다.
        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }

        // Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance();

        // "requestKey"라는 키로 결과를 받을 리스너를 등록합니다.
        getParentFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                // getView()가 null이 아닌지 확인하고, tvDob 변수 자체가 null이 아닌지 다시 한번 확인합니다.
                // 수정: etDob 변수를 tvDob로 변경합니다.
                if (getView() != null && tvDob != null) {
                    // EditProfileDialog로부터 받은 데이터를 TextView에 설정
                    String birthdate = result.getString("birthdate");
                    String bloodType = result.getString("bloodType");
                    String emergencyContact = result.getString("emergencyContact");
                    String gender = result.getString("gender"); // 성별 정보 가져오기

                    // 수정: et.setText()를 tv.setText()로 변경합니다.
                    if (tvDob != null) {
                        tvDob.setText(birthdate);
                    }
                    if (tvEmergencyContact != null) {
                        tvEmergencyContact.setText(emergencyContact);
                    }
                    if (tvBloodType != null) {
                        tvBloodType.setText(bloodType);
                    }
                    if (tvGender != null) {
                        tvGender.setText(gender);
                    }
                }
            }
        });

        // 알러지 팝업에서 결과를 받을 리스너
        getParentFragmentManager().setFragmentResultListener("allergyRequestKey", this, (requestKey, result) -> {
            ArrayList<String> foodAllergies = result.getStringArrayList("food_allergies");
            ArrayList<String> drugAllergies = result.getStringArrayList("drug_allergies");

            // 음식 알레르기 TextView 업데이트
            if (foodAllergies != null) {
                // 수정: EditText[]를 TextView[]로 변경합니다.
                TextView[] foodTextViews = {tvFoodAllergy1, tvFoodAllergy2, tvFoodAllergy3};
                for (int i = 0; i < foodTextViews.length; i++) {
                    if (i < foodAllergies.size() && foodTextViews[i] != null) {
                        foodTextViews[i].setText(foodAllergies.get(i));
                    } else if (foodTextViews[i] != null) {
                        foodTextViews[i].setText(""); // 남은 칸은 비움
                    }
                }
            }

            // 약물 알레르기 TextView 업데이트
            if (drugAllergies != null) {
                // 수정: EditText[]를 TextView[]로 변경합니다.
                TextView[] drugTextViews = {tvDrugAllergy1, tvDrugAllergy2, tvDrugAllergy3};
                for (int i = 0; i < drugTextViews.length; i++) {
                    if (i < drugAllergies.size() && drugTextViews[i] != null) {
                        drugTextViews[i].setText(drugAllergies.get(i));
                    } else if (drugTextViews[i] != null) {
                        drugTextViews[i].setText(""); // 남은 칸은 비움
                    }
                }
            }
        });

        // 프로필 사진/이름 팝업에서 결과를 받을 리스너를 추가합니다.
        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            String updatedPhotoUri = result.getString("updatedPhotoUri");

            if (updatedName != null) {
                if (tvName != null) { // tvName이 이미 초기화되었다고 가정
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

        // 뷰들을 한 곳에서 초기화하는 메소드
        bindViews(view);

        // Firestore에서 사용자 데이터를 불러와 UI를 업데이트하는 메소드 호출
        loadUserProfileData();

        // '알러지 정보 수정' 버튼 이벤트 (기존과 동일)
        btnAllergy.setOnClickListener(v -> {
            AllergyDialog dialog = new AllergyDialog();
            dialog.show(getParentFragmentManager(), "allergyDialog");
        });

        // '개인정보 수정' 버튼 이벤트 (팝업 호출로 변경)
        btnProfile.setOnClickListener(v -> {
            EditProfileDialog dialog = EditProfileDialog.newInstance(userUid);
            dialog.show(getParentFragmentManager(), "editProfileDialog");
        });

        // '프로필 수정' 버튼 이벤트 (기존과 동일)
        btnUploadphoto.setOnClickListener(v -> {
            EditProfilePhotoDialog dialog = new EditProfilePhotoDialog();
            dialog.show(getParentFragmentManager(), "editProfilePhotoDialog");
        });

        return view;
    }

    // 뷰들을 한 곳에서 초기화하는 메소드
    private void bindViews(View view) {
        btnAllergy = view.findViewById(R.id.btn_allergy);
        btnProfile = view.findViewById(R.id.btn_profile);
        btnUploadphoto = view.findViewById(R.id.btn_upload_photo);

        // 수정: findViewById의 R.id를 tv_ 로 변경합니다.
        tvDob = view.findViewById(R.id.tv_dob);
        tvEmergencyContact = view.findViewById(R.id.tv_emergency_contact);
        tvBloodType = view.findViewById(R.id.tv_blood_type);
        tvGender = view.findViewById(R.id.tv_gender);

        // 수정: findViewById의 R.id를 tv_ 로 변경합니다.
        tvFoodAllergy1 = view.findViewById(R.id.tv_food_allergy_1);
        tvFoodAllergy2 = view.findViewById(R.id.tv_food_allergy_2);
        tvFoodAllergy3 = view.findViewById(R.id.tv_food_allergy_3);
        tvDrugAllergy1 = view.findViewById(R.id.tv_drug_allergy_1);
        tvDrugAllergy2 = view.findViewById(R.id.tv_drug_allergy_2);
        tvDrugAllergy3 = view.findViewById(R.id.tv_drug_allergy_3);

        ivProfileImage = view.findViewById(R.id.profile_image);
        tvName = view.findViewById(R.id.tv_name);
    }

    // Firestore에서 사용자 데이터를 가져와 UI를 업데이트하는 메소드
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
                        String emergencyContact = documentSnapshot.getString("emergencyContact"); // 비상 연락처 가져오기
                        String gender = documentSnapshot.getString("gender"); // 성별 정보 가져오기
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl"); // 이미지 URL 필드

                        // UI 업데이트
                        if (tvName != null) {
                            if (gender != null) {
                                tvName.setText(name + " (" + gender + ")"); // 이름 옆에 성별 표시
                            } else {
                                tvName.setText(name);
                            }
                        }
                        // 수정: et.setText()를 tv.setText()로 변경합니다.
                        if (tvDob != null) tvDob.setText(birth);
                        if (tvBloodType != null) tvBloodType.setText(bloodType);
                        if (tvEmergencyContact != null) tvEmergencyContact.setText(emergencyContact);
                        if (tvGender != null) tvGender.setText(gender);

                        // Glide 라이브러리를 사용해 URL에서 이미지를 로드
                        if (profileImageUrl != null && ivProfileImage != null && getContext() != null) {
                            Glide.with(getContext()).load(profileImageUrl).into(ivProfileImage);
                        }
                    } else {
                        // Firestore에 사용자 정보가 없을 경우 (예: 첫 로그인)
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "프로필 정보가 없습니다. 새로 등록해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "프로필 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 뷰 바인딩이 아닌 일반 뷰 변수이므로 명시적으로 null 처리할 필요는 없지만,
        // 메모리 관리를 위해 onDestroyView에서 null 처리하는 것이 좋습니다.
    }
}