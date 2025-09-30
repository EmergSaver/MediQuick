package com.emergsaver.mediquick;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

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
import util.NavigationUtil;

public class DetailHospitalActivity extends AppCompatActivity {
    private MapView miniMap;
    private KakaoMap kakaoMap;

    private TextView hospitalName, hospitalAddress, hospitalPhone, congestion;
    private View congestionDot;
    private Hospital hospital;
    private CongestionManager congestionManager;
    private TableLayout tableLayout;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private AppCompatButton findBtn;
    private ImageButton backBtn;


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

        backBtn.setOnClickListener(v -> {
            finish();
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
        backBtn = findViewById(R.id.backBtn);

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
                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(hospitalPos, 17));
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

        // 제목 행
        TableRow headerRow = new TableRow(this);

        TextView headerDept = new TextView(this);
        headerDept.setText("진료과");
        headerDept.setPadding(24, 30, 24, 30);
        headerDept.setTypeface(null, Typeface.BOLD);
        headerDept.setGravity(Gravity.CENTER);
        headerDept.setBackgroundResource(R.drawable.rounded_table_header_left);
        headerDept.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));

        TextView headerDoctor = new TextView(this);
        headerDoctor.setText("전문의 수");
        headerDoctor.setPadding(24, 30, 24, 30);
        headerDoctor.setTypeface(null, Typeface.BOLD);
        headerDoctor.setGravity(Gravity.CENTER);
        headerDoctor.setBackgroundResource(R.drawable.rounded_table_header_right);
        headerDoctor.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f));

        headerRow.addView(headerDept);
        headerRow.addView(headerDoctor);
        tableLayout.addView(headerRow);

        // 일반 데이터 행
        if(hospital.getSpecialties() == null) {
            return;
        }

        for(int i = 0; i < hospital.getSpecialties().size(); i++) {
            TableRow row = new TableRow(this);

            TextView deptName = new TextView(this);
            deptName.setText(hospital.getSpecialties().get(i).getDept_name());
            deptName.setPadding(24, 30, 24, 30);
            deptName.setGravity(Gravity.CENTER);

            TextView doctorCount = new TextView(this);
            doctorCount.setText(hospital.getSpecialties().get(i).getDoctor_count() + " 명");
            doctorCount.setPadding(24, 30, 24, 30);
            doctorCount.setGravity(Gravity.CENTER);

            // 마지막 행일 경우 → 하단 모서리 둥글게
            if (i == hospital.getSpecialties().size() - 1) {
                deptName.setBackgroundResource(R.drawable.rounded_table_left);
                doctorCount.setBackgroundResource(R.drawable.rounded_table_right);
            } else {
                deptName.setBackgroundResource(R.drawable.rounded_table);
                doctorCount.setBackgroundResource(R.drawable.rounded_table);
            }

            deptName.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
            doctorCount.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f));

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