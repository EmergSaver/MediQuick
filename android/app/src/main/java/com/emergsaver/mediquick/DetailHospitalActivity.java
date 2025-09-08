package com.emergsaver.mediquick;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;

import model.Hospital;
import model.Specialty;
import util.CongestionManager;
import util.MapManager;
import util.NavigationManager;

public class DetailHospitalActivity extends AppCompatActivity {
    private MapView miniMap;
    private KakaoMap kakaoMap;

    private TextView hospitalName, hospitalAddress, hospitalPhone, congestion;
    private View congestionDot;
    private Hospital hospital;
    private CongestionManager congestionManager;
    private TableLayout tableLayout;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Button findBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_hospital);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Intent로 전달된 병원 정보 받기
        hospital = (Hospital) getIntent().getSerializableExtra("hospital");

        if(hospital == null) {
            finish();
            return;
        }

        initView();
        initMiniMap();
        initCongestion();
        addTable();

        findBtn.setOnClickListener(v -> {
            findRoad();
        });
    }

    private void initView() {
        hospitalName = findViewById(R.id.tvHospitalName);
        hospitalAddress = findViewById(R.id.tvAddress);
        hospitalPhone = findViewById(R.id.tvPhone);
        congestionDot = findViewById(R.id.viewCongestionDot);
        congestion = findViewById(R.id.tvCongestion);
        miniMap = findViewById(R.id.miniMap);
        findBtn = findViewById(R.id.btnStartNavi);

        // 병원 정보 세팅
        hospitalName.setText(hospital.getHospital_name());
        hospitalAddress.setText(hospital.getAddress());
        hospitalPhone.setText(hospital.getPhone());
    }

    private void initMiniMap() {
        LatLng hospitalPos = LatLng.from(hospital.getLatitude(), hospital.getLongitude());

        // 미니맵 초기화
        MapManager mapManager = new MapManager(null);
        mapManager.initMapView(miniMap, hospitalPos, new MapManager.onMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                kakaoMap = map;
                // 병원 위치로 카메라 이동
                if (hospitalPos != null) {
                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(hospitalPos, 18));
                }

                // 마커 추가
                mapManager.addHospitalMarker(hospital);
            }

            @Override
            public void onMarkerClick(Hospital hospital) {
                // 마커 클릭 시 동작 없음
            }
        });
    }

    private void initCongestion() {
        // 혼잡도 초기화
        congestionManager = new CongestionManager();
        congestionManager.startCongestionUpdates(new CongestionManager.OnCongestionUpdateListener() {
            @Override
            public void onUpdate(Object peopleCount) {
                runOnUiThread(() -> {
                    String congestionStatus;
                    int people = 0;
                    int color;

                    if (peopleCount instanceof Number) {
                        people = ((Number) peopleCount).intValue();
                    }

                    if (people <= 20) {
                        congestionStatus = "원활";
                        color = getResources().getColor(R.color.lime_green);
                    } else if (people <= 40) {
                        congestionStatus = "보통";
                        color = getResources().getColor(R.color.orange);
                    } else {
                        congestionStatus = "혼잡";
                        color = getResources().getColor(R.color.red);
                    }

                    // 색상과 상태를 TextView에 적용
                    if(congestionDot != null) {
                        congestionDot.setBackgroundColor(color);
                    }
                    congestion.setText(congestionStatus + "\t(" + people + " 명)");
                });
            }

            @Override
            public void onError(Exception e) {
                Log.d("DETAIL_HOSPITAL", "ERROR : ", e);
            }
        });
    }

    // 진료과 & 진료인 수 테이블 생성
    private void addTable() {
        tableLayout = findViewById(R.id.tableDepts);
        tableLayout.removeAllViews();

        if(hospital.getSpecialties() == null) {
            return;
        }

        for(Specialty s : hospital.getSpecialties()) {
            TableRow row = new TableRow(this);

            TextView deptName = new TextView(this);
            deptName.setText(s.getDept_name());
            deptName.setPadding(8, 8, 8, 8);

            TextView doctorCount = new TextView(this);
            doctorCount.setText(s.getDoctor_count() + " 명");
            doctorCount.setPadding(8, 8, 8, 8);

            row.addView(deptName);
            row.addView(doctorCount);

            tableLayout.addView(row);
        }
    }

    // 길찾기 버튼 클릭 시
    private void findRoad() {
        // 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
            return;
        }

        // 권한이 있는 경우에만 위치 가져오기
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if(location != null) {
                        double startLat = location.getLatitude();
                        double startLng = location.getLongitude();

                        // 병원 좌표 + 이름
                        double destLat = hospital.getLatitude();
                        double destLng = hospital.getLongitude();
                        String destName = hospital.getHospital_name();

                        // 네비게이션 호출
                        NavigationManager.startNavigation(
                                DetailHospitalActivity.this,
                                startLat,
                                startLng,
                                destLat,
                                destLng,
                                destName
                        );
                    } else {
                        Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findRoad(); // 권한 허용 시 길찾기 재실행
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(congestionManager != null) {
            congestionManager.stopUpdates();
        }
    }
}