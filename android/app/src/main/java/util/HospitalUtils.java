package util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import model.Hospital;

public class HospitalUtils {
    public static void dialPhone(Context context, String phone) {
        if(phone != null && !phone.isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            context.startActivity(callIntent);
        } else {
            Toast.makeText(context, "전화번호가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareHospital(Context context, Hospital hospital) {
        // 공유 텍스트 구성
        String shareText = "병원 정보\n" + "병원명 : " + hospital.getHospital_name() + "\n" + (hospital.getPhone().isEmpty() ? "" : "전화번호: " + hospital.getPhone());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "병원 위치 공유");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        context.startActivity(Intent.createChooser(shareIntent, "공유하기"));
    }
}
