package nav;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.R;
import com.emergsaver.mediquick.adapter.HospitalAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import model.Hospital;

public class HospitalFragment extends Fragment {

    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_SYMPTOM = "selected_symptom";

    private String categoryId;
    private String selectedSymptom;

    public HospitalFragment() {
    }

    public static HospitalFragment newInstance(String categoryId, String selectedSymptom) {
        HospitalFragment fragment = new HospitalFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_SYMPTOM, selectedSymptom);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
            selectedSymptom = getArguments().getString(ARG_SYMPTOM);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hospital, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recycler_hospitals);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        loadHospitals(recycler);
    }

    private void loadHospitals(RecyclerView recyclerView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("hospitals")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Hospital> hospitalList = new ArrayList<>();


                        for (DocumentSnapshot doc : task.getResult()) {
                            String json = doc.getString("hospital_json");
                            if (json != null) {
                                // Hospital 객체로 변환 (간단히 Gson 활용 가능)
                                Hospital hospital = parseHospitalJson(json);
                                hospitalList.add(hospital);
                            }
                        }

                        if (!hospitalList.isEmpty()) {
                            HospitalAdapter adapter = new HospitalAdapter(hospitalList);
                            recyclerView.setAdapter(adapter);
                        } else {
                            Toast.makeText(getContext(), "추천 병원이 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "병원 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Hospital parseHospitalJson(String json) {
        // Gson이나 org.json으로 변환
        try {
            return new com.google.gson.Gson().fromJson(json, Hospital.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
