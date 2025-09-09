package com.emergsaver.mediquick;

import android.app.Application;

import com.kakao.vectormap.KakaoMapSdk;
import com.kakao.sdk.common.KakaoSdk;

import util.HospitalGeoUpdater;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 카카오맵 SDK 초기화
        KakaoMapSdk.init(this, BuildConfig.KAKAO_MAP_KEY);

        // 카카오 로그인 SDK 초기화 (네이티브 앱 키 사용)
        KakaoSdk.init(this, BuildConfig.KAKAO_LOGIN_KEY);
        // 임시 확인용 (테스트 후 삭제)
        android.util.Log.d("KAKAO", "mapKey=" + BuildConfig.KAKAO_MAP_KEY);
        android.util.Log.d("KAKAO", "loginKey=" + BuildConfig.KAKAO_LOGIN_KEY);
        android.util.Log.d("KAKAO", "keyHash=" + com.kakao.sdk.common.util.Utility.INSTANCE.getKeyHash(this));

        // 지오코딩 -> Firebase의 경도 위도 추출해서 따로 저장 (필요할 때만 사용)
//        HospitalGeoUpdater updater = new HospitalGeoUpdater(this);
//        updater.updateHospitalCoordinates();
    }
}
