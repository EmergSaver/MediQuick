package nav;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.emergsaver.mediquick.DetailHospitalActivity;
import com.emergsaver.mediquick.R;
import com.emergsaver.mediquick.adapter.MapSearchAdapter;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraPosition;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.TrackingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.Hospital;
import repository.HospitalRepository;
import util.HospitalUtils;
import util.MapManager;


public class MapFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

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
    private MapManager mapManager;

    private boolean isFirstLocationUpdate = true; // 최초 위치 이동 여부

    private List<Hospital> allHospitals = new ArrayList<>();
    private MapSearchAdapter searchAdapter;

    private EditText searchEditText;

    private RecyclerView searchResultList;


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
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.map_view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.map_view);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

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

        initBottomSheet(view);
        initRecyclerView(view);
        initButtons(view);
        initSearchBar(view);
        currentBtn(view);

        // MapManager 생성 및 초기화
        mapManager = new MapManager(fusedLocationProviderClient);
        mapManager.initMapView(mapView, null, new MapManager.onMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap kakaoMap) {
                // 지도 준비 완료 후 병원 데이터 로드
                loadHospitalData();
            }

            @Override
            public void onMarkerClick(Hospital hospital) {
                // 마커 클릭 시 UI 업데이트
                hospitalModel = hospital;
                showHospitalInfo(view, hospital);
            }
        });
    }

    private void initBottomSheet(View view) {
        ConstraintLayout bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        Button closeBtn = view.findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(v -> bottomSheet.setVisibility(View.GONE));
    }

    private void initRecyclerView(View view) {
        RecyclerView searchResultList = view.findViewById(R.id.search_result_list);
        searchResultList.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 리스트 클릭 시
        searchAdapter = new MapSearchAdapter(new ArrayList<>(), hospital -> {
//            mapManager.moveCameraToHospital(hospital);
            if(kakaoMap != null) {
                savedCameraPos = kakaoMap.getCameraPosition();
            }
            hospitalModel = hospital;
            Log.d("SEARCH_LIST_CLICK", "hospital: " + hospital.getHospital_name()
                    + " lat=" + hospital.getLatitude()
                    + " lng=" + hospital.getLongitude());

            Intent intent = new Intent(requireContext(), DetailHospitalActivity.class);
            intent.putExtra("hospital", hospital);
            startActivity(intent);
        });
        searchResultList.setAdapter(searchAdapter);

        searchResultList.setVisibility(View.GONE);
    }

    private void initButtons(View view) {
        ImageButton callBtn = view.findViewById(R.id.callBtn);
        TextView callText = view.findViewById(R.id.callText);
        ImageButton exportBtn = view.findViewById(R.id.exportBtn);
        // 마커 클릭 시 병원 이름 표시 TextView
        TextView hospitalNameText = view.findViewById(R.id.hospital_name);

        // 전화걸기
        callBtn.setOnClickListener(v -> {
            if(hospitalModel != null && !TextUtils.isEmpty(hospitalModel.getPhone())) {
                HospitalUtils.dialPhone(getContext(), hospitalModel.getPhone());
            } else {
                Toast.makeText(requireContext(), "전화번호가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 외부로 해당 정보 공유하기
        exportBtn.setOnClickListener(v -> {
            if(hospitalModel != null) {
                HospitalUtils.shareHospital(getContext(), hospitalModel);
            } else {
                Toast.makeText(requireContext(), "공유할 병원 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView addressDetail = view.findViewById(R.id.addressDetail);
        TextView addressText = view.findViewById(R.id.addressText);
        boolean[] isExpanded = {false};

        addressDetail.setOnClickListener(v -> {
            toggleAddress(addressText, isExpanded);
        });
    }

    private void toggleAddress(TextView addressText, boolean[] isExpanded) {
        if(isExpanded[0]) {
            addressText.setMaxLines(1);
            addressText.setEllipsize(TextUtils.TruncateAt.END);
            isExpanded[0] = false;
        } else {
            addressText.setMaxLines(Integer.MAX_VALUE);
            addressText.setEllipsize(null);
            isExpanded[0] = true;
        }
    }

    private void loadHospitalData() {
        HospitalRepository repository = new HospitalRepository();
        repository.fetchHospitals(new HospitalRepository.OnHospitalsLoaded() {
            @Override
            public void onLoaded(List<Hospital> hospitals) {
                allHospitals.clear();
                allHospitals.addAll(hospitals);

                for(Hospital h : hospitals) {
                    mapManager.addHospitalMarker(h);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("FIREBASE", "병원 데이터 로드 실패", e);
            }
        });
    }

    private void initSearchBar(View view) {
        searchEditText = view.findViewById(R.id.search_text);
        ImageButton searchBtn = view.findViewById(R.id.search_btn);

        searchBtn.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            performSearch(query);
        });

        // 키보드 엔터키 처리
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString().trim();
            performSearch(query);
            return true;
        });
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            searchAdapter.setItems(new ArrayList<>());
            return;
        }

        HospitalRepository repository = new HospitalRepository();
        repository.searchHospitalByName(query, new HospitalRepository.OnHospitalsLoaded() {
            @Override
            public void onLoaded(List<Hospital> hospitals) {
                searchAdapter.setItems(hospitals);

                searchResultList = getView().findViewById(R.id.search_result_list);
                searchResultList.setVisibility(hospitals.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(Exception e) {
                Log.e("SEARCH_ERROR", "검색 실패 ", e);
            }
        });
        View view = getView();
        if(view != null) {
            InputMethodManager input = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            input.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showHospitalInfo(View view, Hospital hospital) {
        // 새로운 Hospital 객체 생성 후 값 설정
        TextView hospitalNameText = view.findViewById(R.id.hospital_name);
        TextView callText = view.findViewById(R.id.callText);
        TextView addressText = view.findViewById(R.id.addressText);
        TextView doctorText = view.findViewById(R.id.doctorText);

        hospitalNameText.setText(hospital.getHospital_name());
        callText.setText(hospital.getPhone());
        addressText.setText(hospital.getAddress());
        if(hospitalModel.getDoctor_count() >= 100) {
            doctorText.setText("전문의 100+ 명");
        } else {
            doctorText.setText("전문의 " + hospitalModel.getDoctor_count() + " 명");
        }

        ConstraintLayout bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheet.setVisibility(View.VISIBLE);
    }


    // 현재 위치로 카메라 이동
    private void currentBtn(View view) {
        FloatingActionButton floatBtn = view.findViewById(R.id.btn_current);

        floatBtn.setOnClickListener(v -> {
            mapManager.moveCameraToCurrent(view.getContext());
        });
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
                    Priority.PRIORITY_HIGH_ACCURACY, 0 // 1초 간격
            ).setMinUpdateDistanceMeters(0f)
                    .build();

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

        // 검생창 초기화
        if (searchAdapter != null) {
            searchEditText.setText("");
            searchEditText.clearFocus();
        }

        if(searchResultList != null) {
            searchResultList.setVisibility(View.GONE);
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