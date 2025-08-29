package com.emergsaver.mediquick;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.Blob; // Blob 타입 임포트
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide; // Glide는 이제 프로필 이미지에는 사용하지 않음

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

public class ProfileFragment extends Fragment {

    private Button btnAllergy;
    private Button btnProfile;
    private Button btnUploadphoto;

    private TextView tvDob;
    private TextView tvEmergencyContact;
    private TextView tvBloodType;
    private TextView tvGender;

    private LinearLayout llFoodAllergies;
    private LinearLayout llDrugAllergies;

    private ImageView ivProfileImage;
    private TextView tvName;

    private FirebaseFirestore db;
    private String userUid;

    private static final Map<String, String> FOOD_ALLERGY_MAP = new HashMap<>();
    static {
        FOOD_ALLERGY_MAP.put("cb_egg", "난류(계란)");
        FOOD_ALLERGY_MAP.put("cb_milk", "밀");
        FOOD_ALLERGY_MAP.put("cb_peach", "복숭아");
        FOOD_ALLERGY_MAP.put("cb_dairy", "우유");
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

        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }

        db = FirebaseFirestore.getInstance();

        // 다른 다이얼로그에서 돌아오는 결과 리스너들 (기존 로직 유지)
        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, result) -> {
            if (getView() != null) {
                String birthdate = result.getString("birthdate");
                String bloodType = result.getString("bloodType");
                String emergencyContact = result.getString("emergencyContact");
                String gender = result.getString("gender");

                if (tvDob != null) tvDob.setText(birthdate);
                if (tvEmergencyContact != null) tvEmergencyContact.setText(emergencyContact);
                if (tvBloodType != null) tvBloodType.setText(bloodType);
                if (tvGender != null) tvGender.setText(gender);
            }
        });

        getParentFragmentManager().setFragmentResultListener("allergyRequestKey", this, (requestKey, result) -> {
            ArrayList<String> foodAllergies = result.getStringArrayList("food_allergies");
            ArrayList<String> drugAllergies = result.getStringArrayList("drug_allergies");
            updateAllergiesUI(foodAllergies, drugAllergies);
        });

        // ✨ 수정: EditProfilePhotoDialog에서 돌아오는 결과 리스너 수정
        // 바이트 배열을 직접 전달받는 대신, 업데이트가 성공했음을 알리고 다시 Firestore를 불러옴
        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            boolean photoUpdated = result.getBoolean("photoUpdated", false);

            if (updatedName != null) {
                if (tvName != null) {
                    tvName.setText(updatedName);
                }
            }

            if (photoUpdated) {
                // 사진이 업데이트된 경우, Firestore에서 최신 데이터를 다시 불러와서 UI를 갱신합니다.
                loadUserProfileData();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        bindViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 프래그먼트가 화면에 나타날 때마다 데이터를 다시 불러와서 최신 상태를 유지합니다.
        loadUserProfileData();
    }

    private void bindViews(View view) {
        btnAllergy = view.findViewById(R.id.btn_allergy);
        btnProfile = view.findViewById(R.id.btn_profile);
        btnUploadphoto = view.findViewById(R.id.btn_upload_photo);

        tvDob = view.findViewById(R.id.tv_dob);
        tvEmergencyContact = view.findViewById(R.id.tv_emergency_contact);
        tvBloodType = view.findViewById(R.id.tv_blood_type);
        tvGender = view.findViewById(R.id.tv_gender);

        llFoodAllergies = view.findViewById(R.id.ll_food_allergies);
        llDrugAllergies = view.findViewById(R.id.ll_drug_allergies);

        ivProfileImage = view.findViewById(R.id.profile_image);
        tvName = view.findViewById(R.id.tv_name);

        btnAllergy.setOnClickListener(v -> {
            if (userUid != null) {
                AllergyDialog dialog = AllergyDialog.newInstance(userUid);
                dialog.show(getParentFragmentManager(), "allergyDialog");
            }
        });

        btnProfile.setOnClickListener(v -> {
            if (userUid != null) {
                EditProfileDialog dialog = EditProfileDialog.newInstance(userUid);
                dialog.show(getParentFragmentManager(), "editProfileDialog");
            }
        });

        btnUploadphoto.setOnClickListener(v -> {
            if (userUid != null) {
                EditProfilePhotoDialog dialog = EditProfilePhotoDialog.newInstance(userUid);
                dialog.show(getParentFragmentManager(), "editProfilePhotoDialog");
            }
        });
    }

    /**
     * Firebase Firestore에서 현재 사용자의 프로필 데이터를 불러와 UI를 업데이트하는 메소드입니다.
     */
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
                        String emergencyContact = documentSnapshot.getString("emergencyContact");
                        String gender = documentSnapshot.getString("gender");
                        // ✨ 수정: 프로필 이미지를 Blob 타입으로 가져옴
                        Blob profileImageBlob = documentSnapshot.getBlob("profileImage");

                        if (tvName != null) {
                            tvName.setText(name);
                        }
                        if (tvDob != null) tvDob.setText(birth);
                        if (tvBloodType != null) tvBloodType.setText(bloodType);
                        if (tvEmergencyContact != null) tvEmergencyContact.setText(emergencyContact);
                        if (tvGender != null && gender != null) tvGender.setText(gender);

                        // ✨ 수정: Blob을 비트맵으로 변환하여 ImageView에 설정
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
                            // 프로필 이미지가 없는 경우 기본 이미지로 설정
                            ivProfileImage.setImageResource(R.drawable.ic_user);
                        }
                        // ✨ 추가: 뷰에 동그란 배경을 적용하여 이미지를 동그랗게 만듭니다.
                        // XML에서 ImageView의 background를 @drawable/circular_background로 설정해야 합니다.
                        ivProfileImage.setBackgroundResource(R.drawable.circular_background);
                        ivProfileImage.setClipToOutline(true);


                        Map<String, Object> allergies = (Map<String, Object>) documentSnapshot.get("allergies");
                        if (allergies != null) {
                            List<String> foodAllergies = new ArrayList<>();
                            Map<String, Boolean> foodMap = (Map<String, Boolean>) allergies.get("foodAllergies");
                            if (foodMap != null) {
                                for (Map.Entry<String, Boolean> entry : foodMap.entrySet()) {
                                    if (entry.getValue()) {
                                        String allergyName = FOOD_ALLERGY_MAP.get(entry.getKey());
                                        if (allergyName != null) {
                                            foodAllergies.add(allergyName);
                                        }
                                    }
                                }
                            }

                            List<String> drugAllergies = (List<String>) allergies.get("drugAllergies");
                            if (drugAllergies == null) {
                                drugAllergies = new ArrayList<>();
                            }

                            updateAllergiesUI(new ArrayList<>(foodAllergies), new ArrayList<>(drugAllergies));
                        } else {
                            updateAllergiesUI(null, null);
                        }

                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "프로필 정보가 없습니다. 새로 등록해주세요.", Toast.LENGTH_SHORT).show();
                            tvName.setText("새로운 사용자");
                            ivProfileImage.setImageResource(R.drawable.ic_user);
                            tvDob.setText("정보 없음");
                            tvBloodType.setText("정보 없음");
                            tvEmergencyContact.setText("정보 없음");
                            tvGender.setText("정보 없음");
                            updateAllergiesUI(null, null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "프로필 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                        tvName.setText("새로운 사용자");
                        ivProfileImage.setImageResource(R.drawable.ic_user);
                        tvDob.setText("정보 없음");
                        tvBloodType.setText("정보 없음");
                        tvEmergencyContact.setText("정보 없음");
                        tvGender.setText("정보 없음");
                        updateAllergiesUI(null, null);
                    }
                });
    }

    private void updateAllergiesUI(ArrayList<String> foodAllergies, ArrayList<String> drugAllergies) {
        if (llFoodAllergies != null) {
            llFoodAllergies.removeAllViews();
            if (foodAllergies != null && !foodAllergies.isEmpty()) {
                for (String allergy : foodAllergies) {
                    TextView tv = createAllergyTextView(allergy);
                    llFoodAllergies.addView(tv);
                }
            } else {
                TextView tv = createAllergyTextView("정보 없음");
                llFoodAllergies.addView(tv);
            }
        }

        if (llDrugAllergies != null) {
            llDrugAllergies.removeAllViews();
            if (drugAllergies != null && !drugAllergies.isEmpty()) {
                for (String allergy : drugAllergies) {
                    TextView tv = createAllergyTextView(allergy);
                    llDrugAllergies.addView(tv);
                }
            } else {
                TextView tv = createAllergyTextView("정보 없음");
                llDrugAllergies.addView(tv);
            }
        }
    }

    private TextView createAllergyTextView(String text) {
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