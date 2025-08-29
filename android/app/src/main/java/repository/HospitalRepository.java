package repository;

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

    public void searchHospitalByName(String query, OnHospitalsLoaded callback) {
        if(query == null || query.trim().isEmpty()) {
            // 검색어가 없는 경우 빈리스트 반환
            return;
        }

        db.collection("hospitals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Hospital> hospitals = new ArrayList<>();
                    for(var document: queryDocumentSnapshots.getDocuments()) {
                        String jsonStr = document.getString("hospital_json");
                        if(jsonStr != null) {
                            Gson gson = new Gson();
                            Hospital hospital = gson.fromJson(jsonStr, Hospital.class);

                            // 검색어 포함 여부 확인 (대소문자 무시)
                            if(hospital.getHospital_name() != null && hospital.getHospital_name().
                                    toLowerCase().contains(query.toLowerCase())) {
                                hospitals.add(hospital);
                            }
                        }
                    }
                    callback.onLoaded(hospitals);
                })
                .addOnFailureListener(callback::onError);
    }
}
