package util;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import model.Hospital;
import model.Specialty;

public class HospitalRepository {
    // Firebase db 선언
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnHospitalsLoaded {
        void onLoaded(List<Hospital> hospitals);

        void onError(Exception e);
    }

    public void fetchHospitals(OnHospitalsLoaded callback) {
        // 문서 id 가져오기
        db.collection("hospitals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Hospital> hospitals = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        String jsonStr = document.getString("hospital_json");
                        if (jsonStr != null) {
                            // JSON 파싱
                            Gson gson = new Gson();
                            Hospital hospital = gson.fromJson(jsonStr, Hospital.class);

                            // 전문의 수 합계 계산
                            int totalDoctors = 0;
                            if (hospital.getSpecialties() != null) {
                                for (Specialty s : hospital.getSpecialties()) {
                                    totalDoctors += s.getDoctor_count();
                                }
                            }
                            hospital.setDoctor_count(totalDoctors);

                            // 주소 있으면 가져오기
                            Double lat = document.getDouble("latitude");
                            Double lon = document.getDouble("longitude");

                            if (lat != null && lon != null) {
                                hospital.setLatitude(lat);
                                hospital.setLongitude(lon);
                            }
                            hospitals.add(hospital);
                        }
                    }
                    callback.onLoaded(hospitals);
                })
                .addOnFailureListener(callback::onError);
    }
}
