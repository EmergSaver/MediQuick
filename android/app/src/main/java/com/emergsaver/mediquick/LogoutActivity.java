package com.emergsaver.mediquick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class LogoutActivity extends AppCompatActivity {

    // LoginActivity에서 쓰는 prefs 키와 맞추면 깔끔합니다.
    private static final String PREF_LOGIN = "mediquick_login_pref";
    private static final String KEY_AUTO   = "auto_login";
    private static final String KEY_EMAIL  = "auto_email";
    private static final String KEY_PW     = "auto_pw";

    private MaterialButton bLogout, bCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_logout);

        // ★ 반드시 setContentView 이후에 바인딩
        bLogout = findViewById(R.id.btnLogout);
        bCancel = findViewById(R.id.btnCancel);

        bLogout.setOnClickListener(v -> {
            // 1) Firebase 로그아웃
            try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}

            // 2) 자동로그인/저장값 해제 (LoginActivity에서 쓰는 키 기준으로 정리)
            SharedPreferences sp = getSharedPreferences(PREF_LOGIN, MODE_PRIVATE);
            sp.edit()
                    .putBoolean(KEY_AUTO, false)
                    .remove(KEY_EMAIL)
                    .remove(KEY_PW)
                    .apply();

            // 3) 로그인 화면으로 스택 초기화 이동
            Intent i = new Intent(LogoutActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish(); // 자신 종료
        });

        bCancel.setOnClickListener(v -> finish());

        // (선택) 에지-투-에지 패딩: 루트 뷰가 있을 때만 적용
        if (findViewById(R.id.main_logout) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_logout), (view, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(sb.left, sb.top, sb.right, sb.bottom);
                return insets;
            });
        }
    }
}
