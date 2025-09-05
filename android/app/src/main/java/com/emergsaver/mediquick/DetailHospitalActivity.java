package com.emergsaver.mediquick;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;

import model.Hospital;
import repository.CongestionRepository;
import util.MapManager;

public class DetailHospitalActivity extends AppCompatActivity {
    private MapView miniMap;
    private KakaoMap kakaoMap;

    private TextView hospitalName, hospitalAddress, hospitalPhone, congestion;
    private Hospital hospital;

    private Handler handler = new Handler();
    private final int REFRESH_INTERVAL_MS = 3 * 60 * 1000;     // 3분

    private CongestionRepository congestionRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_hospital);

        // Intent로 전달된 병원 정보 받기
        hospital = (Hospital) getIntent().getSerializableExtra("hospital");

        Log.d("DETAIL_HOSPITAL", "lat=" + hospital.getLatitude() + " lng=" + hospital.getLongitude());

        if(hospital == null) {
            finish();
            return;
        }

        hospitalName = findViewById(R.id.tvHospitalName);
        hospitalAddress = findViewById(R.id.tvAddress);
        hospitalPhone = findViewById(R.id.tvPhone);
        congestion = findViewById(R.id.tvCongestion);
        miniMap = findViewById(R.id.miniMap);

        // 병원 정보 세팅
        hospitalName.setText(hospital.getHospital_name());
        hospitalAddress.setText(hospital.getAddress());
        hospitalPhone.setText(hospital.getPhone());

        LatLng hospitalPos = LatLng.from(hospital.getLatitude(), hospital.getLongitude());

        // 미니맵 초기화
        MapManager mapManager = new MapManager(null);
        mapManager.initMapView(miniMap, hospitalPos, new MapManager.onMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                kakaoMap = map;
                // 병원 위치로 카메라 이동
                if(hospitalPos != null) {
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

        congestionRepository = new CongestionRepository();
        startCongestionUpdates();
    }

    private void startCongestionUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchCongestion();
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        }, 0); // 첫 실행 바로
    }

    private void fetchCongestion() {
        congestionRepository.fetchLatestAnalysis(new CongestionRepository.OnAnalysisLoaded() {
            @Override
            public void onLoaded(Object peopleCount) {
                runOnUiThread(() -> {
                    if (peopleCount != null) {
                        // 타입 상관없이 문자열로 변환 후 표시
                        congestion.setText(String.valueOf(peopleCount));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("DETAIL_HOSPITAL", "Error fetching congestion", e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Handler 해제
    }
}