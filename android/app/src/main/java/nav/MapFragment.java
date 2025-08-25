package nav;

import android.Manifest;
import android.content.Intent;
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

import com.emergsaver.mediquick.CategoryActivity;
import com.emergsaver.mediquick.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;

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

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
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
        // editText 누르면 추천하는 병원 페이지 보여줌
        search.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CategoryActivity.class);
            startActivity(intent);
        });

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

                    // kakaoMap이 준비되어 있으면 카메라 이동
                    if(kakaoMap != null) {
                        // 카메라 이동
                        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                                LatLng.from(currentLng)
                        ));
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

                                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                                            LatLng.from(currentLng)
                                    ));
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
}