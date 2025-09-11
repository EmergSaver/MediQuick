package util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.emergsaver.mediquick.R;
import com.google.android.gms.location.DeviceOrientationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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

    public void initCurrentLocation(Context context) {
        if(kakaoMap == null) {
            return;
        }

        // 권한 체크 후 마지막 위치 가져오기 (초기 지도 위치 설정)
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청 후 종료
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

        // TrackingManager 초기화
        trackingManager = kakaoMap.getTrackingManager();
        // 위치 기록 없을 경우 실시간 위치 요청
        LocationRequest locationRequest = new LocationRequest.Builder(
                LocationRequest.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateDistanceMeters(0f)
                .build();


        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    LatLng startPos;
                    if(location != null) {
                        // 위치 기록 있으면 지도 및 라벨 초기화
                        updateMapWithLocation(location, context);

                    } else {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(@NonNull LocationResult result) {
                                if(result.getLastLocation() != null) {
                                    updateMapWithLocation(result.getLastLocation(), context);
                                    fusedLocationProviderClient.removeLocationUpdates(this); // 한 번만 사용
                                }
                            }
                        }, Looper.getMainLooper());
                    }
                });
    }

    private void updateMapWithLocation(Location location, Context context) {
        LatLng startPos = LatLng.from(location.getLatitude(), location.getLongitude());

        // Label 추가
        LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();

        // 현재 위치 label
        if(locationLabel == null) {
            locationLabel = labelLayer.addLabel(LabelOptions.from(startPos)
                    .setRank(10)
                    .setStyles(LabelStyles.from(
                            LabelStyle.from(R.drawable.current_location)
                                    .setAnchorPoint(0.5f, 0.5f))));

        } else {
            locationLabel.moveTo(startPos);
        }
        if(headingLabel == null) {
            // 방향 Label
            headingLabel = labelLayer.addLabel(LabelOptions.from(startPos)
                    .setRank(9)
                    .setStyles(LabelStyles.from(
                            LabelStyle.from(R.drawable.direction_area)
                                    .setAnchorPoint(0.5f, 1.0f))));

        } else {
            headingLabel.moveTo(startPos);
        }

        // headingLabel이 locationLabel과 함께 이동
        locationLabel.addSharePosition(headingLabel);

        // 최초 위치이므로 카메라 이동
        if(isFirstLocationUpdate) {
            moveCameraToCurrent(context);
            isFirstLocationUpdate = false;
        }
    }

    public void initMapView(MapView mapView, LatLng initialPosition, onMapReadyCallback callback) {
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

                // 마커 클릭 리스너 등록
                kakaoMap.setOnLabelClickListener((kakao, layer, label) -> {
                    Hospital hospital = (Hospital) label.getTag();
                    if(hospital != null) {
                        callback.onMarkerClick(hospital);
                    }
                    return true;
                });

                if(initialPosition != null) {
                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(initialPosition, 18));
                } else {
                    initCurrentLocation(mapView.getContext());
                }

                if(callback != null) {
                    callback.onMapReady(kakaoMap);
                }

                DeviceOrientationRequest request = new DeviceOrientationRequest
                        .Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build();
                fusedLocationProviderClient.requestDeviceOrientationUpdates(
                        request,
                        deviceOrientation -> {
                            if (headingLabel != null) {
                                headingLabel.rotateTo((float) Math.toRadians(deviceOrientation.getHeadingDegrees()));
                            }
                        },
                        Looper.getMainLooper()
                );
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

        // 혼잡도 기반 색상 결정
        int drawableRes;
        int peopleCount = hospital.getCurrentPeople();

        if(peopleCount <= 20) {
            drawableRes = R.drawable.green_marker;
        } else if(peopleCount <= 40) {
            drawableRes = R.drawable.orange_marker;
        } else {
            drawableRes = R.drawable.red_marker;
        }

        // 마커 스타일 설정
        LabelStyle style = LabelStyle.from(drawableRes).setAnchorPoint(0.5f, 1.0f);
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

    public void moveCameraToCurrent(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && kakaoMap != null) {
                            LatLng currentPos = LatLng.from(location.getLatitude(), location.getLongitude());
                            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentPos, 16));
                        }
                    });
        } else {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }
}
