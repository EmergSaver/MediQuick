package nav;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.R;
import com.emergsaver.mediquick.adapter.CategoryAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import model.Category;

// ✅ HospitalFragment import 추가
import nav.HospitalFragment;

public class CategoryFragment extends Fragment {

    // Firebase 문서 ID 매핑
    private static final Map<String, String> DISPLAY_TO_FIREBASE = new HashMap<String, String>() {{
        put("코&귀", "코_귀");
        put("입&목&얼굴", "입_목_얼굴");
        put("물질오용&중독", "물질오용_중독");
    }};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_category, container, false);

        RecyclerView recycler = root.findViewById(R.id.recycler_categories);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 3));

        List<Category> categories = Arrays.asList(
                new Category(R.drawable.icon_brain, "신경계"),
                new Category(R.drawable.icon_eyes, "눈"),
                new Category(R.drawable.icon_nose, "코&귀"),
                new Category(R.drawable.icon_mouth_neck_face, "입&목&얼굴"),
                new Category(R.drawable.icon_breath, "호흡기계"),
                new Category(R.drawable.icon_heart, "심혈관계"),
                new Category(R.drawable.icon_digestion, "소화기계"),
                new Category(R.drawable.icon_urinary, "비뇨기계"),
                new Category(R.drawable.icon_skin, "피부"),
                new Category(R.drawable.icon_pregnancy, "임신"),
                new Category(R.drawable.icon_bone, "근골격계"),
                new Category(R.drawable.icon_body, "몸통외상"),
                new Category(R.drawable.icon_poison, "물질오용&중독"),
                new Category(R.drawable.icon_general, "일반")
        );

        CategoryAdapter adapter = new CategoryAdapter(categories, item -> {
            String firebaseId = DISPLAY_TO_FIREBASE.getOrDefault(item.getName(), item.getName());
            showCategorySymptomsFromFirebase(firebaseId, item.getName());
        });

        recycler.setAdapter(adapter);

        return root;
    }

    private void showCategorySymptomsFromFirebase(String firebaseId, String displayName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("symptoms").document(firebaseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists() && document.getData() != null) {
                            List<String> subSymptoms = new ArrayList<>(document.getData().keySet());

                            if (subSymptoms.isEmpty()) {
                                openHospitalFragment(displayName);
                                return;
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle(displayName + " 세부 증상")
                                    .setItems(subSymptoms.toArray(new String[0]), (dialog, which) -> {
                                        String selectedSubSymptom = subSymptoms.get(which);
                                        showRelatedSymptomsPopup(firebaseId, displayName, selectedSubSymptom);
                                    })
                                    .setPositiveButton("닫기", null)
                                    .show();
                        } else {
                            openHospitalFragment(displayName);
                        }
                    } else {
                        Toast.makeText(getContext(), "데이터 불러오기 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRelatedSymptomsPopup(String firebaseId, String displayName, String subSymptom) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("symptoms").document(firebaseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        Object obj = document.get(subSymptom);

                        if (obj instanceof Map) {
                            Map<String, Object> symptomData = (Map<String, Object>) obj;
                            Object relatedObj = symptomData.get("related_symptoms");

                            if (relatedObj instanceof List) {
                                List<String> related = (List<String>) relatedObj;
                                if (!related.isEmpty()) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                    builder.setTitle(subSymptom + " 관련 증상")
                                            .setItems(related.toArray(new String[0]), (dialog, which) -> {
                                                openHospitalFragment(displayName);
                                            })
                                            .setPositiveButton("닫기", null)
                                            .show();
                                } else {
                                    openHospitalFragment(displayName);
                                }
                            } else {
                                openHospitalFragment(displayName);
                            }
                        } else {
                            openHospitalFragment(displayName);
                        }
                    }
                });
    }

    // ✅ HospitalFragment 호출
    private void openHospitalFragment(String categoryName) {
        HospitalFragment fragment = HospitalFragment.newInstance(categoryName);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
