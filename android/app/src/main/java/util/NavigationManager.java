package util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NavigationManager {
    // 네비게이션 실행
    public static void startNavigation(Context context, double startLat, double startLng, double destLat, double destLng, String destName) {
        List<Intent> intentList = new ArrayList<>();

        // 구글 지도
        Uri googleIntentUri = Uri.parse("http://maps.google.com/maps?saddr=" + startLat + "," + startLng +
                "&daddr=" + destLat + "," + destLng + "&dirflg=d");
        Intent googleMapIntent = new Intent(Intent.ACTION_VIEW, googleIntentUri);
        intentList.add(googleMapIntent);

        // 카카오 네비
        String kakaoUrl = "kakaonavi://navigate?ep=" + destLat + "," + destLng +
                "&by=CAR&ap=" + startLat + "," + startLng +
                "&name=" + Uri.encode(destName);
        Intent kakaoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(kakaoUrl));
        intentList.add(kakaoIntent);

        // 네이버 지도
        String naverUrl = "nmap://route/car?slat=" + startLat + "&slng=" + startLng +
                "&sname=내위치&dlat=" + destLat + "&dlng=" + destLng +
                "&dname=" + Uri.encode(destName);
        Intent naverIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(naverUrl));
        intentList.add(naverIntent);

        //실행 가능한 앱이 없을 때
        List<Intent> availableIntents = new ArrayList<>();
        for (Intent intent : intentList) {
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                availableIntents.add(intent);
            }
        }

        if (availableIntents.isEmpty()) {
            Toast.makeText(context, "네비게이션 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent chooser = Intent.createChooser(intentList.remove(0), "네비게이션 앱 선택");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[0]));

        context.startActivity(chooser);
    }
}
