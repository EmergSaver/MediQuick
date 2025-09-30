package nav;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.R;
import com.emergsaver.mediquick.adapter.HospitalAdapter;
import com.google.android.gms.location.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

import model.Hospital;
import model.Specialty;

public class HospitalFragment extends Fragment {

    private static final String ARG_CATEGORY_NAME = "category_name";
    private String categoryName;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private double userLat;
    private double userLng;

    private List<Hospital> hospitalList = new ArrayList<>();
    private RecyclerView recyclerView;

    private static final Map<String, List<String>> CATEGORY_TO_DEPTS = new HashMap<String, List<String>>() {{
        put("신경계", Arrays.asList("신경"));
        put("눈", Arrays.asList("안과"));
        put("코&귀", Arrays.asList("이비인후과"));
        put("입&목&얼굴", Arrays.asList("이비인후과","피부과","구강","치과","치의학"));
        put("호흡기계", Arrays.asList("이비인후과","내과"));
        put("심혈관계", Arrays.asList("흉부"));
        put("소화기계", Arrays.asList("내과"));
        put("비뇨기계", Arrays.asList("비뇨"));
        put("피부", Arrays.asList("피부과","성형"));
        put("임신", Arrays.asList("산부인과"));
        put("근골격계", Arrays.asList("정형외과"));
        put("몸통외상", Arrays.asList("외과"));
        put("물질오용&중독", Arrays.asList("정신과","신경과","가정의학과"));
        put("일반", Arrays.asList("내과"));
    }};

    public HospitalFragment() { }

    public static HospitalFragment newInstance(String categoryName) {
        HospitalFragment fragment = new HospitalFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_NAME, categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
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

        recyclerView = view.findViewById(R.id.recycler_hospitals);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        // Firestore에서 병원 데이터 최초 1회만 로드
        loadHospitalsFromFirestore();

        // 위치 요청 설정 (1초 간격)
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // 1초마다 업데이트
        locationRequest.setFastestInterval(500); // 최소 0.5초
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (android.location.Location location : locationResult.getLocations()) {
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();

                    // 위치가 바뀔 때마다 거리만 재계산 후 RecyclerView 갱신
                    updateHospitalDistances();
                }
            }
        };

        // 권한 체크 후 즉시 마지막 위치 가져오기 + 실시간 업데이트 시작
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            userLat = location.getLatitude();
                            userLng = location.getLongitude();
                            updateHospitalDistances(); // 즉시 거리 계산 후 화면 반영
                        }
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 위치 업데이트 중지
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // Firestore에서 병원 데이터 1회 로드
    private void loadHospitalsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<String> targetDepts = CATEGORY_TO_DEPTS.getOrDefault(categoryName, new ArrayList<>());

        db.collection("hospitals")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Set<String> addedHospitalIds = new HashSet<>();

                        for (DocumentSnapshot doc : task.getResult()) {
                            String json = doc.getString("hospital_json");
                            Double lat = doc.getDouble("latitude");
                            Double lng = doc.getDouble("longitude");

                            if (json != null && lat != null && lng != null) {
                                Hospital hospital = new com.google.gson.Gson().fromJson(json, Hospital.class);
                                if (hospital == null || addedHospitalIds.contains(hospital.getId())) continue;

                                hospital.setLatitude(lat);
                                hospital.setLongitude(lng);

                                boolean matches = false;
                                if (hospital.getSpecialties() != null) {
                                    for (Specialty s : hospital.getSpecialties()) {
                                        if (targetDepts.stream().anyMatch(d -> s.getDept_name().contains(d))) {
                                            matches = true;
                                            break;
                                        }
                                    }
                                }
                                if (!matches) continue;

                                hospitalList.add(hospital);
                                addedHospitalIds.add(hospital.getId());
                            }
                        }

                        if(userLat != 0 && userLng != 0) {
                            updateHospitalDistances();
                        }

                        recyclerView.setAdapter(new HospitalAdapter(hospitalList));
                    }
                });
    }

    // 거리만 재계산 후 RecyclerView 갱신
    private void updateHospitalDistances() {
        if (hospitalList.isEmpty()) return;

        for (Hospital hospital : hospitalList) {
            double distanceKm = distance(userLat, userLng, hospital.getLatitude(), hospital.getLongitude()) / 1000.0;
            hospital.setDistanceToUser(distanceKm);
        }

        Collections.sort(hospitalList, Comparator.comparingDouble(Hospital::getDistanceToUser));

        HospitalAdapter adapter = (HospitalAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // 거리 계산 (미터 단위)
    private double distance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }
}
