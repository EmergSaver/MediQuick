package com.emergsaver.mediquick;

import android.content.Intent;
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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;

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

        // 이전 코드에서 이 부분에 userUid를 Bundle에서 가져오는 로직이 있었는데,
        // 이 프래그먼트를 어떻게 인스턴스화하는지에 따라 달라집니다.
        // MainActivity에서 userUid를 넘겨준다면 이 로직은 유지되어야 합니다.
        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }

        db = FirebaseFirestore.getInstance();

        // 이 부분은 EditProfileDialog에서 돌아오는 결과 리스너입니다.
        // 현재 코드는 tvDob 등 텍스트뷰가 아직 null일 수 있으므로 onViewCreated에
        // 리스너를 옮겨서 뷰가 초기화된 후 실행되도록 하는 것이 더 안전합니다.
        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, result) -> {
            if (getView() != null) {
                String birthdate = result.getString("birthdate");
                String bloodType = result.getString("bloodType");
                String emergencyContact = result.getString("emergencyContact");
                String gender = result.getString("gender");

                // ✨ 수정: Null 체크를 통해 안전하게 UI 업데이트
                if (tvDob != null) tvDob.setText(birthdate);
                if (tvEmergencyContact != null) tvEmergencyContact.setText(emergencyContact);
                if (tvBloodType != null) tvBloodType.setText(bloodType);
                if (tvGender != null) tvGender.setText(gender);
            }
        });

        // 이 부분은 AllergyDialog에서 돌아오는 결과 리스너입니다.
        getParentFragmentManager().setFragmentResultListener("allergyRequestKey", this, (requestKey, result) -> {
            ArrayList<String> foodAllergies = result.getStringArrayList("food_allergies");
            ArrayList<String> drugAllergies = result.getStringArrayList("drug_allergies");
            // ✨ 수정: 이 메소드는 UI를 업데이트하므로, onViewCreated 이후에 호출되도록
            // 리스너를 옮기거나, 내부에서 뷰가 유효한지 다시 확인해야 합니다.
            updateAllergiesUI(foodAllergies, drugAllergies);
        });

        // ✨ 수정: 가장 중요한 부분입니다.
        // EditProfilePhotoDialog에서 돌아오는 결과 리스너입니다.
        // 기존 코드에는 이미 수정된 `updatedPhotoUri`를 Glide로 로드하는 로직이 있습니다.
        // 이 로직은 올바르므로 그대로 두시면 됩니다.
        // 단, userUid가 유효한지 확인하고, 다이얼로그를 띄울 때 userUid를 넘겨주는지 확인해야 합니다.
        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            String updatedPhotoUri = result.getString("updatedPhotoUri");

            if (updatedName != null && tvName != null) {
                tvName.setText(updatedName);
            }

            if (updatedPhotoUri != null && ivProfileImage != null) {
                Glide.with(this).load(Uri.parse(updatedPhotoUri)).into(ivProfileImage);
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
        // ✨ 수정: onResume()에서 loadUserProfileData()를 호출하여
        // 프래그먼트가 다시 활성화될 때마다 최신 데이터를 불러오도록 합니다.
        // 이전에 이 코드가 없었다면 추가하면 좋습니다.
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
            AllergyDialog dialog = AllergyDialog.newInstance(userUid);
            dialog.show(getParentFragmentManager(), "allergyDialog");
        });

        btnProfile.setOnClickListener(v -> {
            EditProfileDialog dialog = EditProfileDialog.newInstance(userUid);
            dialog.show(getParentFragmentManager(), "editProfileDialog");
        });

        btnUploadphoto.setOnClickListener(v -> {
            // ✨ 수정: EditProfilePhotoDialog로 userUid를 전달하는 newInstance를 사용
            // 기존에 이 로직이 있다면 올바르게 작동할 것입니다.
            EditProfilePhotoDialog dialog = EditProfilePhotoDialog.newInstance(userUid);
            dialog.show(getParentFragmentManager(), "editProfilePhotoDialog");
        });
    }

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
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // ✨ 수정: tvName에 (성별)이 추가되는 로직이 있었는데,
                        // tvGender가 별도로 존재하므로 이 부분을 name만 표시하도록 간소화할 수 있습니다.
                        if (tvName != null) {
                            tvName.setText(name);
                        }
                        if (tvDob != null) tvDob.setText(birth);
                        if (tvBloodType != null) tvBloodType.setText(bloodType);
                        if (tvEmergencyContact != null) tvEmergencyContact.setText(emergencyContact);
                        // ✨ 수정: tvGender는 이미 별도로 있으므로, gender 값이 있다면 설정
                        if (tvGender != null && gender != null) tvGender.setText(gender);

                        if (profileImageUrl != null && ivProfileImage != null && getContext() != null) {
                            Glide.with(getContext()).load(profileImageUrl).into(ivProfileImage);
                        } else {
                            // ✨ 수정: profileImageUrl이 없을 경우 기본 이미지 설정
                            ivProfileImage.setImageResource(R.drawable.ic_user);
                        }

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
                            // ✨ 수정: 알레르기 데이터가 없는 경우 "정보 없음"으로 표시
                            updateAllergiesUI(null, null);
                        }

                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "프로필 정보가 없습니다. 새로 등록해주세요.", Toast.LENGTH_SHORT).show();
                            // ✨ 추가: 문서가 없을 경우에도 UI 초기화
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
                        // ✨ 추가: 오류 발생 시에도 UI 초기화
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