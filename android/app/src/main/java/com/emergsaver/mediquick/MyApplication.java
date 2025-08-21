package com.emergsaver.mediquick;

import android.app.Application;

import com.kakao.vectormap.KakaoMapSdk;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 앱 실행 시 SDK 초기화
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_KEY);
    }
}
