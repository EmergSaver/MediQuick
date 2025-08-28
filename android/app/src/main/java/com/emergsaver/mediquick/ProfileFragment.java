package com.emergsaver.mediquick;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import android.widget.Toast;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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

    private TextView tvFoodAllergy1, tvFoodAllergy2, tvFoodAllergy3;
    private TextView tvDrugAllergy1, tvDrugAllergy2, tvDrugAllergy3;

    private ImageView ivProfileImage;
    private TextView tvName;

    private FirebaseFirestore db;
    private String userUid;

    // Firestore ID와 실제 음식 알레르기 이름 매핑
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

        getParentFragmentManager().setFragmentResultListener("profilePhotoRequestKey", this, (requestKey, result) -> {
            String updatedName = result.getString("updatedName");
            String updatedPhotoUri = result.getString("updatedPhotoUri");

            if (updatedName != null && tvName != null) {
                tvName.setText(updatedName);
            }

            if (updatedPhotoUri != null && ivProfileImage != null) {
                Glide.with(getContext()).load(Uri.parse(updatedPhotoUri)).into(ivProfileImage);
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

        tvFoodAllergy1 = view.findViewById(R.id.tv_food_allergy_1);
        tvFoodAllergy2 = view.findViewById(R.id.tv_food_allergy_2);
        tvFoodAllergy3 = view.findViewById(R.id.tv_food_allergy_3);
        tvDrugAllergy1 = view.findViewById(R.id.tv_drug_allergy_1);
        tvDrugAllergy2 = view.findViewById(R.id.tv_drug_allergy_2);
        tvDrugAllergy3 = view.findViewById(R.id.tv_drug_allergy_3);

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
            EditProfilePhotoDialog dialog = new EditProfilePhotoDialog();
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

                        if (tvName != null) {
                            tvName.setText(name + (gender != null ? " (" + gender + ")" : ""));
                        }
                        if (tvDob != null) tvDob.setText(birth);
                        if (tvBloodType != null) tvBloodType.setText(bloodType);
                        if (tvEmergencyContact != null) tvEmergencyContact.setText(emergencyContact);
                        if (tvGender != null) tvGender.setText(gender);

                        if (profileImageUrl != null && ivProfileImage != null && getContext() != null) {
                            Glide.with(getContext()).load(profileImageUrl).into(ivProfileImage);
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
                            updateAllergiesUI(null, null);
                        }

                    } else {
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

    private void updateAllergiesUI(ArrayList<String> foodAllergies, ArrayList<String> drugAllergies) {
        TextView[] foodTextViews = {tvFoodAllergy1, tvFoodAllergy2, tvFoodAllergy3};
        if (foodAllergies != null) {
            for (int i = 0; i < foodTextViews.length; i++) {
                if (i < foodAllergies.size() && foodTextViews[i] != null) {
                    foodTextViews[i].setText(foodAllergies.get(i));
                } else if (foodTextViews[i] != null) {
                    foodTextViews[i].setText("");
                }
            }
        } else {
            for (TextView tv : foodTextViews) {
                if (tv != null) tv.setText("");
            }
        }

        TextView[] drugTextViews = {tvDrugAllergy1, tvDrugAllergy2, tvDrugAllergy3};
        if (drugAllergies != null) {
            for (int i = 0; i < drugTextViews.length; i++) {
                if (i < drugAllergies.size() && drugTextViews[i] != null) {
                    drugTextViews[i].setText(drugAllergies.get(i));
                } else if (drugTextViews[i] != null) {
                    drugTextViews[i].setText("");
                }
            }
        } else {
            for (TextView tv : drugTextViews) {
                if (tv != null) tv.setText("");
            }
        }
    }
}