package util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.emergsaver.mediquick.R;
import com.google.android.gms.location.FusedLocationProviderClient;
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

import model.Hospital;

public class MapManager {
    private KakaoMap kakaoMap;

    // 센서 데이터 제공
    private FusedLocationProviderClient fusedLocationProviderClient;

    private Label locationLabel, headingLabel;
    private TrackingManager trackingManager;

    // 카메라 위치 저장
    private CameraPosition savedCameraPos;
    private Hospital hospitalModel;

    // 최초 위치 이동 여부
    private boolean isFirstLocationUpdate = true;

    public interface onMapReadyCallback {
        void onMapReady(KakaoMap kakaoMap);
        void onMarkerClick(Hospital hospital);
    }

    private onMapReadyCallback callback;

    public MapManager(FusedLocationProviderClient client) {
        this.fusedLocationProviderClient = client;
    }

    // getter 추가
    public KakaoMap getKakaoMap() {
        return kakaoMap;
    }

    public void initCurrentLocation(Context context) {
        // TrackingManager 초기화
        trackingManager = kakaoMap.getTrackingManager();

        // 권한 체크 후 마지막 위치 가져오기 (초기 지도 위치 설정)
        if(ActivityCompat.checkSelfPermission(context,
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
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    public void initMapView(MapView mapView, onMapReadyCallback callback) {
        this.callback = callback;

        // initMapView 로직
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
                initCurrentLocation(mapView.getContext());

                // 마커 클릭 리스너 등록
                kakaoMap.setOnLabelClickListener((kakao, layer, label) -> {
                    Hospital hospital = (Hospital) label.getTag();
                    if(hospital != null) {
                        callback.onMarkerClick(hospital);
                    }
                    return true;
                });

                if(callback != null) {
                    callback.onMapReady(kakaoMap);
                }
            }
        });
    }

    public Label addHospitalMarker(Hospital hospital) {
        // 마커 추가
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

        newLabel.setTag(hospital);
        Log.d("MAP_DEBUG", "새 마커 추가 완료: " + hospital.getHospital_name() + " 위치: " + pos);

        return newLabel;
    }

    // 카메라 이동
    public void moveCameraToHospital(Hospital hospital) {
        LatLng pos = LatLng.from(hospital.getLatitude(), hospital.getLongitude());
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(pos, 18));
        Log.d("MAP_DEBUG", "카메라 이동 완료");
    }
}
