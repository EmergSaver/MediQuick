package nav;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.emergsaver.mediquick.R;
import com.google.android.gms.location.DeviceOrientation;
import com.google.android.gms.location.DeviceOrientationListener;
import com.google.android.gms.location.DeviceOrientationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraPosition;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.TrackingManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.Hospital;
import model.Specialty;
import util.HospitalUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private MapView mapView;
    // 구글에서 제공하는 위치 서비스 API
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    // 지도 객체 저장
    private KakaoMap kakaoMap;

    // BottomSheet 제어 변수 선언
    private BottomSheetBehavior<ConstraintLayout> bottomSheetBehavior;

    // 현재 위치와 방향 표시 Label 선언
    private Label locationLabel;
    private Label headingLabel;
    private TrackingManager trackingManager;
    private FusedLocationProviderClient orientationProviderClient;
    private DeviceOrientationListener orientationListener;

    // 카메라 위치 저장
    private CameraPosition savedCameraPos;

    private Hospital hospitalModel;

    // mapView 초기화
    private void initMapView(View view) {
        mapView = view.findViewById(R.id.map_view);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                // API가 정상적으로 종료된 경우
                Log.d("API_CONNECT_END", "API 호출이 정상적으로 종료되었습니다.");
            }

            @Override
            public void onMapError(Exception e) {
                // 지도 사용 중 에러 발생 시
                Log.e("API_CONNECT_ERROR", e.getMessage());
            }

        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                // 인증 후 API가 정상적으로 호출된 경우
                Log.d("API_CONNECT_SUCCESS", "API가 정상적으로 호출됨");

                // 지도 제어
                kakaoMap = map;

                // 지도 초기 위치 복원
                if (savedCameraPos != null) {
                    kakaoMap.moveCamera(CameraUpdateFactory.newCameraPosition(savedCameraPos));
                }

                // 현재 위치 가져오기
                initCurrentLocation();

                // 마커 클릭 리스너 등록
                kakaoMap.setOnLabelClickListener((kakao, layer, label) -> {
                    handleMarker(label);
                    return true;
                });
            }
        });
    }

    private boolean isFirstLocationUpdate = true; // 최초 위치 이동 여부

    // 현재 위치 가져오기 및 라벨
    private void initCurrentLocation() {
        // TrackingManager 초기화
        trackingManager = kakaoMap.getTrackingManager();

        // 권한 체크 후 마지막 위치 가져오기 (초기 지도 위치 설정)
        if(ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        LatLng startPos;
                        if(location != null) {
                            startPos = LatLng.from(location.getLatitude(), location.getLongitude());

                            // Label 추가
                            LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();

                            // 현재 위치 label
                            locationLabel = labelLayer.addLabel(LabelOptions.from(startPos)
                                    .setRank(10)
                                    .setStyles(LabelStyles.from(
                                            LabelStyle.from(R.drawable.current_location)
                                                    .setAnchorPoint(0.5f, 0.5f))));

                            // 방향 Label
                            headingLabel = labelLayer.addLabel(LabelOptions.from(startPos)
                                    .setRank(9)
                                    .setStyles(LabelStyles.from(
                                            LabelStyle.from(R.drawable.direction_area)
                                                    .setAnchorPoint(0.5f, 1.0f))));

                            // headingLabel이 locationLabel과 함께 이동
                            locationLabel.addSharePosition(headingLabel);

                            // 최초 위치이므로 카메라 이동
                            if(isFirstLocationUpdate) {
                                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(startPos));
                                isFirstLocationUpdate = false;
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    // 마커 생성
    private Label addMarker(Hospital hospital) {
        if(kakaoMap == null || hospital == null) {
            return null;
        }

        LatLng pos = LatLng.from(hospital.getLatitude(), hospital.getLongitude());
        LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();

        // 마커 스타일 설정
        LabelStyle style = LabelStyle.from(R.drawable.red_marker).setAnchorPoint(0.5f, 1.0f);
        LabelStyles styles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(style));
        Log.d("MAP_DEBUG", "LabelStyles 추가 완료: " + styles);

        Label existing = labelLayer.getLabel("searchMarker");
        if(existing != null) {
            labelLayer.remove(existing);
            Log.d("MAP_DEBUG", "기존 마커 제거됨");
        }

        // 마커 추가
        Label newLabel = labelLayer.addLabel(LabelOptions.from(hospital.getHospital_name(), pos).setStyles(styles));

        // 마커로 카메라 이동
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 18));
        Log.d("MAP_DEBUG", "카메라 이동 완료");

        newLabel.setTag(hospital);
        Log.d("MAP_DEBUG", "새 마커 추가 완료: " + hospital.getHospital_name() + " 위치: " + pos);

        return newLabel;
    }

    private void handleMarker(Label label) {
        Hospital hospital = (Hospital) label.getTag();
        if(hospital == null)
            return;

        hospitalModel = hospital;

        LatLng pos = label.getPosition();

        // 새로운 Hospital 객체 생성 후 값 설정
        TextView hospitalNameText = getView().findViewById(R.id.hospital_name);
        TextView callText = getView().findViewById(R.id.callText);
        TextView addressText = getView().findViewById(R.id.addressText);
        TextView doctorText = getView().findViewById(R.id.doctorText);

        hospitalNameText.setText(hospital.getHospital_name());
        callText.setText(hospital.getPhone());
        addressText.setText(hospital.getAddress());
        if(hospitalModel.getDoctor_count() >= 100) {
            doctorText.setText("전문의 100+ 명");
        } else {
            doctorText.setText("전문의 " + hospitalModel.getDoctor_count() + " 명");
        }

        ConstraintLayout bottomSheet = getView().findViewById(R.id.bottom_sheet);
        bottomSheet.setVisibility(View.VISIBLE);
    }

    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        // 방향 센서 초기화
        orientationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        orientationListener = new DeviceOrientationListener() {
            @Override
            public void onDeviceOrientationChanged(@NonNull DeviceOrientation deviceOrientation) {
                if(headingLabel != null) {
                    // 방향에 따른 headingLabel 회전
                    headingLabel.rotateTo((float) Math.toRadians(deviceOrientation.getHeadingDegrees()));
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // BottomSheet 초기화
        ConstraintLayout bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // 마커 클릭 시 병원 이름 표시 TextView
        TextView hospitalNameText = view.findViewById(R.id.hospital_name);

        ImageButton callBtn = view.findViewById(R.id.callBtn);
        TextView callText = view.findViewById(R.id.callText);

        ImageButton exportBtn = view.findViewById(R.id.exportBtn);

        // 전화걸기
        callBtn.setOnClickListener(v -> {
            String phone = callText.getText().toString();
            if(!phone.isEmpty()) {
                // 현재 카메라 위치 저장
                savedCameraPos = kakaoMap.getCameraPosition();
                HospitalUtils.dialPhone(getContext(), phone);
            }
            else {
                Toast.makeText(requireContext(), "전화번호가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 외부로 해당 정보 공유하기
        exportBtn.setOnClickListener(v -> {
            String name = hospitalNameText.getText().toString();
            String phone = callText.getText().toString();

            if(!name.isEmpty()) {
                // 현재 카메라 위치 저장
                savedCameraPos = kakaoMap.getCameraPosition();
                HospitalUtils.shareHospital(getContext(), hospitalModel);
            }
        });


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                if(locationResult == null)
                    return;

                for(Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    LatLng currentLng = LatLng.from(lat, lng);

                    Log.d("CURRENT_LOCATION", "위도 : " + lat + "경도 : " + lng);

                    // 위치 Label 업데이트
                    if(locationLabel != null) {
                        locationLabel.moveTo(currentLng);
                    }
                }
            }
        };

        initMapView(view);

        // Firebase db 선언
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 문서 id 가져오기
//        db.collection("hospitals")
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    for (var document : queryDocumentSnapshots.getDocuments()) {
//                        String jsonStr = document.getString("hospital_json");
//                        if (jsonStr != null) {
//                            // JSON 파싱
//                            Gson gson = new Gson();
//                            Hospital hospital = gson.fromJson(jsonStr, Hospital.class);
//
//                            // 전문의 수 합계 계산
//                            int totalDoctors = 0;
//                            if (hospital.getSpecialties() != null) {
//                                for (Specialty s : hospital.getSpecialties()) {
//                                    totalDoctors += s.getDoctor_count();
//                                }
//                            }
//                            hospital.setDoctor_count(totalDoctors);
//
//                            // 주소 있으면 Geocoding으로 위도/경도 변환
//                            String address = hospital.getAddress();
//                            Log.d("FIREBASE_LOG", "Hospital 주소 : " + address);
//
//                            if (address != null && !address.isEmpty()) {
//                                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
//                                try {
//                                    List<Address> addressList = geocoder.getFromLocationName(address, 1);
//                                    if (address != null && !address.isEmpty()) {
//                                        Address location = addressList.get(0);
//                                        hospital.setLatitude(location.getLatitude());
//                                        hospital.setLongitude(location.getLongitude());
//
//                                        Log.d("FIREBASE_LOG", "주소 변환 완료: " + address);
//                                        Log.d("FIREBASE_LOG", "위도: " + location.getLatitude() + ", 경도: " + location.getLongitude());
//
//                                        addMarker(hospital);
//                                    } else {
//                                        Log.e("FIREBASE_LOG", "주소를 좌표로 변환할 수 없음: " + address);
//                                    }
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                    Log.e("FIREBASE_LOG", "Geocoder 예외 발생", e);
//                                }
//                            }
//                        }
//                    }
//                })
//                .addOnFailureListener(e -> Log.e("FIREBASE", "병원 데이터 로드 실패", e));
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.resume();

        // 저장된 카메라 위치가 있으면 복원
        if (savedCameraPos != null && kakaoMap != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.newCameraPosition(savedCameraPos));
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        DeviceOrientationRequest request = new DeviceOrientationRequest
                .Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build();
        orientationProviderClient.requestDeviceOrientationUpdates(
                request,
                orientationListener,
                Looper.getMainLooper() // 메인 스레드에서 콜백 받도록 지정
        );

        // 사용자 위치 업데이트 (10초마다)
        if(ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000
            ). build();

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @Override
    // 배터리 절약을 위함
    public void onPause() {
        super.onPause();
        mapView.pause();

        // 방향 센서 업데이트 중단
        orientationProviderClient.removeDeviceOrientationUpdates(orientationListener);

        // 사용자 위치 업데이트 중단
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
}