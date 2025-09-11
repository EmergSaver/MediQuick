package com.emergsaver.mediquick;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import util.NavigationUtil;

public class DetailHospitalActivity extends AppCompatActivity {
    private MapView miniMap;
    private KakaoMap kakaoMap;

    private TextView hospitalName, hospitalAddress, hospitalPhone, congestion;
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
            NavigationUtil.findRoad(fusedLocationProviderClient, this, hospital, 1001);
        });
    }

    private void initView() {
        hospitalName = findViewById(R.id.tvHospitalName);
        hospitalAddress = findViewById(R.id.tvAddress);
        hospitalPhone = findViewById(R.id.tvPhone);
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
                runOnUiThread(() ->
                        congestion.setText(String.valueOf(peopleCount)));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(congestionManager != null) {
            congestionManager.stopUpdates();
        }
    }
}