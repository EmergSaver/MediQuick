package com.emergsaver.mediquick;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapView;

import model.Hospital;
import util.MapManager;

public class DetailHospitalActivity extends AppCompatActivity {
    private MapView miniMap;
    private KakaoMap kakaoMap;

    private TextView hospitalName, hospitalAddress, hospitalPhone;
    private Hospital hospital;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_hospital);

        // FusedLocationProviderClient 초기화
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Intent로 전달된 병원 정보 받기
        hospital = (Hospital) getIntent().getSerializableExtra("hospital");

        if(hospital == null) {
            finish();
            return;
        }

        hospitalName = findViewById(R.id.tvHospitalName);
        hospitalAddress = findViewById(R.id.tvAddress);
        hospitalPhone = findViewById(R.id.tvPhone);
        miniMap = findViewById(R.id.miniMap);

        // 병원 정보 세팅
        hospitalName.setText(hospital.getHospital_name());
        hospitalAddress.setText(hospital.getAddress());
        hospitalPhone.setText(hospital.getPhone());

        LatLng hospitalPos = LatLng.from(hospital.getLatitude(), hospital.getLongitude());

        // 미니맵 초기화
        MapManager mapManager = new MapManager(fusedLocationProviderClient);
        mapManager.initMapView(miniMap, hospitalPos, new MapManager.onMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                kakaoMap = map;
                // 병원 위치로 카메라 이동
//                mapManager.moveCameraToHospital(hospital);

                // 마커 추가
                mapManager.addHospitalMarker(hospital);
            }

            @Override
            public void onMarkerClick(Hospital hospital) {
                // 마커 클릭 시 동작 없음
            }
        });
    }
}