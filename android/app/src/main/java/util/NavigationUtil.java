package util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;

import model.Hospital;

public class NavigationUtil {
    // 길찾기 버튼 클릭 시
    public static void findRoad(FusedLocationProviderClient fusedLocationProviderClient, Activity activity,
                         Hospital hospital, int requestCode) {
        // 권한 체크
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    requestCode);
            return;
        }

        // 권한이 있는 경우에만 위치 가져오기
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if(location != null) {
                        double startLat = location.getLatitude();
                        double startLng = location.getLongitude();

                        // 병원 좌표 + 이름
                        double destLat = hospital.getLatitude();
                        double destLng = hospital.getLongitude();
                        String destName = hospital.getHospital_name();

                        // 네비게이션 호출
                        NavigationManager.startNavigation(
                                activity,
                                startLat,
                                startLng,
                                destLat,
                                destLng,
                                destName
                        );
                    } else {
                        Toast.makeText(activity, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static void handlePermissionResult(Activity activity, int requestCode,
                                              @NonNull int[] grantResults,
                                              FusedLocationProviderClient fusedLocationProviderClient,
                                              Hospital hospital) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            findRoad(fusedLocationProviderClient, activity, hospital, requestCode);
        } else {
            Toast.makeText(activity, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
