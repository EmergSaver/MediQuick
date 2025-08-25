package nav;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.emergsaver.mediquick.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

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
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

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

        EditText search = view.findViewById(R.id.search_text);
        mapView = view.findViewById(R.id.map_view);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // BottomSheet 초기화
        LinearLayout bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        // bottomSheet 기본 상태 -> 숨김 설정
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // editText 누르면 추천하는 병원 페이지 보여줌
//        search.setOnClickListener(v -> {
//            Intent intent = new Intent(requireContext(), CategoryActivity.class);
//            startActivity(intent);
//        });
        // editText 누르면 대전 성모 병원으로 이동
        search.setOnClickListener(v -> {
            if (kakaoMap == null) {
                Log.d("MAP_DEBUG", "kakaoMap 아직 null");
                return;
            }

            LatLng targetPos = LatLng.from(36.322848, 127.420258);
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
            Label newLabel = labelLayer.addLabel(LabelOptions.from("searchMarker", targetPos).setStyles(styles));
            Log.d("MAP_DEBUG", "새 마커 추가 완료: " + newLabel + " 위치: " + targetPos);

            // 마커로 카메라 이동
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(targetPos, 18));
            Log.d("MAP_DEBUG", "카메라 이동 완료");

            // 마커 클릭 시 위치 자동 이동 잠시 중단
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
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

                    // kakaoMap이 준비되어 있으면 카메라 이동
                    if(kakaoMap != null) {
                        // 카메라 이동
                        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentLng));
                    }
                }

            }
        };

        // 위치 권한 체크 & 요청
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }

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

                // 권한 체크 후 마지막 위치 가져오기 (초기 지도 위치 설정)
                if(ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(location -> {
                                if(location != null) {
                                    double lat = location.getLatitude();
                                    double lng = location.getLongitude();
                                    LatLng currentLng = LatLng.from(lat, lng);

                                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentLng));
                                } else {
                                    // null 이면 기본 위치 (서울 시청으로)
                                    Log.d("LOCATION", "현재 위치 가져올 수 없음");
                                }
                            });
                } else {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
                }
            }
        });

    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.resume();

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
    public void onPause() {
        super.onPause();
        mapView.pause();

        // 사용자 위치 업데이트 중단 (배터리 절약)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

//    private void addLabel(LatLng lng) {
//        if(kakaoMap == null) return;
//
//        // LabelLayer 가져오기
//        LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();
//
//        // 기존 라벨 제거
//        Label existing = labelLayer.getLabel("current");
//        if (existing != null) labelLayer.remove(existing);
//
//        // 원(circle) 스타일 설정
//        LabelStyle circleStyle = LabelStyle.from(R.drawable.ic_marker) // 원 아이콘
//                .setAnchorPoint(0.5f, 0.5f); // 원 중심점 지정
//
//        // 라벨 추가
//        labelLayer.addLabel(LabelOptions.from("currentLng", lng)
//                .setStyles(LabelStyles.from(circleStyle)));
//
//        // 카메라 이동
//        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(lng, 16));
//    }
}