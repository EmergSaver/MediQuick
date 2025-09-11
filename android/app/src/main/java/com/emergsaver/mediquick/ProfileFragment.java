package com.emergsaver.mediquick;

import android.util.Log;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private ImageButton btnAllergy;
    private Button btnProfile;
//    private Button btnUploadphoto; // `fragment_profile.xml`에서 삭제했으므로, 이 변수도 삭제하는 것이 좋습니다.

    private TextView tvDob;
    private TextView tvEmergencyContact;
    private TextView tvBloodType;
    private TextView tvGender;
    private ImageButton btnSettings;
    private LinearLayout llFoodAllergies;
    private LinearLayout llDrugAllergies;

    private ImageView ivProfileImage;
    private TextView tvName;

    private FirebaseFirestore db;
    private String userUid;

    // Kakao/Firebase 캐시 키
    private static final String PREF_KAKAO = "kakao_user";
    private static final String KEY_K_NICK = "nickname";
    private static final String KEY_K_IMG  = "profileImg";
    private static final String KEY_K_MAIL = "email";

    private static final String PREF_AUTH = "auth_cache";
    private static final String KEY_FB_UID = "firebase_uid";

    // ✨ 수정: `bindViews` 내에서 지역 변수로 재선언하지 않고, 클래스 멤버 변수를 사용하도록 수정
    private Button btnEditProfileIcon;
    private Button btnModifyInfo;

    private static final Map<String, String> FOOD_ALLERGY_MAP = new HashMap<>();
    static {
        FOOD_ALLERGY_MAP.put("cb_egg", "난류(계란)");
        FOOD_ALLERGY_MAP.put("cb_wheat", "밀");
        FOOD_ALLERGY_MAP.put("cb_peach", "복숭아");
        FOOD_ALLERGY_MAP.put("cb_milk", "우유");
        FOOD_ALLERGY_MAP.put("cb_sesame", "참깨");
        FOOD_ALLERGY_MAP.put("cb_tomato", "토마토");
        FOOD_ALLERGY_MAP.put("cb_buckwheat", "메밀");
        FOOD_ALLERGY_MAP.put("cb_almond", "아몬드");
        FOOD_ALLERGY_MAP.put("cb_sulfur_dioxide", "아황산류");
        FOOD_ALLERGY_MAP.put("cb_peanut", "땅콩");
        FOOD_ALLERGY_MAP.put("cb_mackerel", "고등어");
        FOOD_ALLERGY_MAP.put("cb_pork", "돼지고기");
        FOOD_ALLERGY_MAP.put("cb_soybean", "대두");
        FOOD_ALLERGY_MAP.put("cb_crab", "꽃게");
        FOOD_ALLERGY_MAP.put("cb_chicken", "닭고기");
        FOOD_ALLERGY_MAP.put("cb_walnut", "호두");
        FOOD_ALLERGY_MAP.put("cb_shrimp", "새우");
        FOOD_ALLERGY_MAP.put("cb_beef", "쇠고기");
        FOOD_ALLERGY_MAP.put("cb_clam", "잣");
        FOOD_ALLERGY_MAP.put("cb_squid", "오징어");
        FOOD_ALLERGY_MAP.put("cb_shellfish", "조개류");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // 1) Fragment arguments 우선
        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }

        // 2) MainActivity Intent 보조
        if (userUid == null && getActivity() != null && getActivity().getIntent() != null) {
            userUid = getActivity().getIntent().getStringExtra("uid");
        }

        // 3) ★ Functions 성공 시 넣어둔 캐시 UID 최종 보강
        if (userUid == null && getContext() != null) {
            userUid = getContext()
                    .getSharedPreferences(PREF_AUTH, AppCompatActivity.MODE_PRIVATE)
                    .getString(KEY_FB_UID, null);
        }
        Log.d(TAG, "onCreate() userUid=" + userUid);

        // 개인정보/알러지 편집 다이얼로그 결과 수신
        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, result) -> {
            if (getView() != null) {
                String birthdate = result.getString("birthdate");
                String bloodType = result.getString("bloodType");
                String emergencyContact = result.getString("emergencyContact");
                String gender = result.getString("gender");

                if (tvDob != null && birthdate != null) tvDob.setText(birthdate);
                if (tvEmergencyContact != null && emergencyContact != null) tvEmergencyContact.setText(emergencyContact);
                if (tvBloodType != null && bloodType != null) tvBloodType.setText(bloodType);
                if (tvGender != null && gender != null) tvGender.setText(gender);
            }
        });

        getParentFragmentManager().setFragmentResultListener("allergyRequestKey", this, (requestKey, result) -> {
            ArrayList<String> foodAllergies = result.getStringArrayList("food_allergies");
            ArrayList<String> drugAllergies = result.getStringArrayList("drug_allergies");
            updateAllergiesUI(foodAllergies, drugAllergies);
        });

        // 프로필 사진/이름 편집 후 성공 시 → Firestore 재조회
        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            boolean photoUpdated = result.getBoolean("photoUpdated", false);

            if (updatedName != null) {
                if (tvName != null) {
                    tvName.setText(updatedName);
                }
            }

            if (photoUpdated) {
                loadUserProfileData();
            }
            if (updatedName != null && tvName != null) tvName.setText(updatedName);
            if (photoUpdated && userUid != null) loadUserProfileData();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        bindViews(view);

        // 프로필 이미지 뷰 모양 고정(Glide/Bitmap 어느 경우든)
        ivProfileImage.setBackgroundResource(R.drawable.circular_background);
        ivProfileImage.setClipToOutline(true);

        // Kakao 로그인 캐시가 있으면 우선 표시
        SharedPreferences pref = requireContext().getSharedPreferences(PREF_KAKAO, AppCompatActivity.MODE_PRIVATE);
        String nickname  = pref.getString(KEY_K_NICK, null);
        String profileImg= pref.getString(KEY_K_IMG,  null);
        String email     = pref.getString(KEY_K_MAIL, null);

        if (nickname != null) tvName.setText(nickname);
        if (email != null)    tvEmergencyContact.setText(email);
        if (profileImg != null && !profileImg.isEmpty()) {
            Glide.with(this).load(profileImg).circleCrop().into(ivProfileImage);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfileData();

        // 늦게 저장된 UID가 있으면 재보강
        if (userUid == null && getContext() != null) {
            userUid = getContext()
                    .getSharedPreferences(PREF_AUTH, AppCompatActivity.MODE_PRIVATE)
                    .getString(KEY_FB_UID, null);
            Log.d(TAG, "onResume() re-check cached uid: " + userUid);
        }

        if (userUid != null) {
            Log.d(TAG, "onResume() -> loadUserProfileData()");
            loadUserProfileData();
        } else {
            // Kakao 전용 표기: 부족한 필드 기본값
            if (isEmpty(tvDob.getText()))       tvDob.setText("정보 없음");
            if (isEmpty(tvBloodType.getText())) tvBloodType.setText("정보 없음");
            if (isEmpty(tvGender.getText()))    tvGender.setText("정보 없음");
            updateAllergiesUI(null, null);
        }
    }

    private boolean isEmpty(CharSequence cs) {
        return cs == null || cs.toString().trim().isEmpty();
    }

    private void bindViews(View view) {
        btnAllergy = view.findViewById(R.id.btn_allergy);
        btnProfile = view.findViewById(R.id.btn_profile);

        // ✨ 수정: `Button` 타입을 제거하여 클래스 멤버 변수에 할당하도록 변경
        // 이렇게 해야 이전에 선언한 `btnEditProfileIcon`과 `btnModifyInfo` 변수가 초기화됩니다.
        btnEditProfileIcon = view.findViewById(R.id.btn_edit_profile_icon);
        btnModifyInfo = view.findViewById(R.id.btn_modify_info);

        tvDob = view.findViewById(R.id.tv_dob);
        tvEmergencyContact = view.findViewById(R.id.tv_emergency_contact);
        tvBloodType = view.findViewById(R.id.tv_blood_type);
        tvGender = view.findViewById(R.id.tv_gender);

        llFoodAllergies = view.findViewById(R.id.ll_food_allergies);
        llDrugAllergies = view.findViewById(R.id.ll_drug_allergies);

        ivProfileImage = view.findViewById(R.id.profile_image);
        tvName = view.findViewById(R.id.tv_name);
        //
        btnSettings = view.findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), SettingsActivity.class));
            });
            //막힘대비
            btnSettings.bringToFront();
            btnSettings.setElevation(12f);
        }

        // ✨ 수정: `btnAllergy`에 대한 null 체크 추가. 안전성을 높입니다.
        if (btnAllergy != null) {
            btnAllergy.setOnClickListener(v -> {
                if (userUid != null) {
                    AllergyDialog dialog = AllergyDialog.newInstance(userUid);
                    dialog.show(getParentFragmentManager(), "allergyDialog");
                }
            });
        }

        // ✨ 수정: `btnProfile`에 대한 null 체크 추가.
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (userUid != null) {
                    EditProfileDialog dialog = EditProfileDialog.newInstance(userUid);
                    dialog.show(getParentFragmentManager(), "editProfileDialog");
                }
            });
        }

        // `btnUploadphoto` 관련 리스너는 XML에 해당 ID가 없으므로 삭제합니다.

        // ✨ 수정: 새로운 버튼들에 대한 리스너 설정
        if (btnEditProfileIcon != null) {
            btnEditProfileIcon.setOnClickListener(v -> {
                if (userUid != null) {
                    EditProfilePhotoDialog dialog = EditProfilePhotoDialog.newInstance(userUid);
                    dialog.show(getParentFragmentManager(), "editProfilePhotoDialog");
                }
            });
        }

        if (btnModifyInfo != null) {
            btnModifyInfo.setOnClickListener(v -> {
                if (userUid != null) {
                    EditProfileDialog dialog = EditProfileDialog.newInstance(userUid);
                    dialog.show(getParentFragmentManager(), "editProfileDialog");
                }
            });
        }
    }


    private void loadUserProfileData() {
        if (userUid == null) {
            Log.d(TAG, "loadUserProfileData(): userUid is null, skip Firestore");
            return;
        }

        db.collection("users").document(userUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String birth = documentSnapshot.getString("birth");
                        String bloodType = documentSnapshot.getString("bloodType");
                        String emergencyContact = documentSnapshot.getString("emergencyContact");
                        String gender = documentSnapshot.getString("gender");
                        Blob profileImageBlob = documentSnapshot.getBlob("profileImage");

                        if (name != null) tvName.setText(name);
                        tvDob.setText(birth != null ? birth : "정보 없음");
                        tvBloodType.setText(bloodType != null ? bloodType : "정보 없음");
                        tvEmergencyContact.setText(emergencyContact != null ? emergencyContact : "정보 없음");
                        tvGender.setText(gender != null ? gender : "정보 없음");

                        if (profileImageBlob != null && ivProfileImage != null) {
                            try {
                                byte[] imageData = profileImageBlob.toBytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                                ivProfileImage.setImageBitmap(bitmap);
                            } catch (Exception e) {
                                // 이미지 변환 실패 시 기본 이미지로 설정
                                ivProfileImage.setImageResource(R.drawable.ic_user);
                                Toast.makeText(getContext(), "프로필 사진 불러오기 실패", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            ivProfileImage.setImageResource(R.drawable.ic_user);
                        }
                        ivProfileImage.setBackgroundResource(R.drawable.circular_background);
                        ivProfileImage.setClipToOutline(true);

                        Map<String, Object> allergies = (Map<String, Object>) documentSnapshot.get("allergies");
                        if (allergies != null) {
                            List<String> foodAllergies = new ArrayList<>();
                            @SuppressWarnings("unchecked")
                            Map<String, Boolean> foodMap = (Map<String, Boolean>) allergies.get("foodAllergies");
                            if (foodMap != null) {
                                for (Map.Entry<String, Boolean> entry : foodMap.entrySet()) {
                                    if (entry.getValue() != null && entry.getValue()) {
                                        String allergyName = FOOD_ALLERGY_MAP.get(entry.getKey());
                                        if (allergyName != null) foodAllergies.add(allergyName);
                                    }
                                }
                            }
                            @SuppressWarnings("unchecked")
                            List<String> drugAllergies = (List<String>) allergies.get("drugAllergies");
                            if (drugAllergies == null) drugAllergies = new ArrayList<>();
                            updateAllergiesUI(new ArrayList<>(foodAllergies), new ArrayList<>(drugAllergies));
                        } else {
                            updateAllergiesUI(null, null);
                        }
                    } else {
                        // 문서 없음 → 기본값 유지
                        if (isEmpty(tvDob.getText()))       tvDob.setText("정보 없음");
                        if (isEmpty(tvBloodType.getText())) tvBloodType.setText("정보 없음");
                        if (isEmpty(tvEmergencyContact.getText())) tvEmergencyContact.setText("정보 없음");
                        if (isEmpty(tvGender.getText()))    tvGender.setText("정보 없음");
                        if (ivProfileImage.getDrawable() == null) {
                            ivProfileImage.setImageResource(R.drawable.ic_user);
                        }
                        updateAllergiesUI(null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore get user failed", e);
                    Toast.makeText(getContext(), "프로필 불러오기 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAllergiesUI(@Nullable ArrayList<String> foodAllergies, @Nullable ArrayList<String> drugAllergies) {
        if (llFoodAllergies != null) {
            llFoodAllergies.removeAllViews();
            if (foodAllergies != null && !foodAllergies.isEmpty()) {
                for (String allergy : foodAllergies) llFoodAllergies.addView(createAllergyChip(allergy));
            } else {
                llFoodAllergies.addView(createAllergyChip("정보 없음"));
            }
        }

        if (llDrugAllergies != null) {
            llDrugAllergies.removeAllViews();
            if (drugAllergies != null && !drugAllergies.isEmpty()) {
                for (String allergy : drugAllergies) llDrugAllergies.addView(createAllergyChip(allergy));
            } else {
                llDrugAllergies.addView(createAllergyChip("정보 없음"));
            }
        }
    }

    private TextView createAllergyChip(String text) {
        TextView tv = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 4;
        tv.setLayoutParams(params);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        tv.setBackgroundResource(R.drawable.rounded_background);
        return tv;
    }
}
