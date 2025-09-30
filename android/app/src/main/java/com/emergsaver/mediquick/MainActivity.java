package com.emergsaver.mediquick;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import nav.CategoryFragment;
import nav.MapFragment;

public class MainActivity extends AppCompatActivity {

    //사용자 UID를 저장할 변수
    private String userUid;

    // 재사용 프래그먼트
    private Fragment mapFrag;
    private Fragment userFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // LoginActivity에서 전달된 UID 우선 획득
        Intent intent = getIntent();
        if (intent != null) {
            userUid = intent.getStringExtra("uid");
        }

        // 보조: 현재 FirebaseAuth 세션에서 uid (A안의 익명로그인/이메일로그인 모두 지원)
        if (userUid == null) {
            FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
            if (cur != null) userUid = cur.getUid();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Fragment 초기화
        mapFrag = new MapFragment();
        userFrag = new ProfileFragment();

        // ProfileFragment에 uid 전달 (존재할 때만)
        if (userUid != null) {
            Bundle bundle = new Bundle();
            bundle.putString("userUid", userUid);
            userFrag.setArguments(bundle);
        }

        // 첫 화면: 지도
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mapFrag).commit();
            bottomNav.setSelectedItemId(R.id.nav_map);
        }

        // 네비게이션 클릭 시
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == R.id.nav_hospital) {
                    // 병원 추천 버튼 → 팝업 띄우기
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_searchpopup, null);

                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setView(dialogView)
                            .create();

                    Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
                    Button btnOk = dialogView.findViewById(R.id.btn_ok);

                    btnCancel.setOnClickListener(v -> dialog.dismiss());

                    btnOk.setOnClickListener(v -> {
                        dialog.dismiss();
                        // 카테고리 화면으로 이동
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new nav.CategoryFragment())
                                .commit();
                    });

                    dialog.show();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                        // 좌우 여백 주고 싶으면 width 조정
                        dialog.getWindow().setLayout(
                                (int) (getResources().getDisplayMetrics().widthPixels * 0.9), // 화면 너비의 90%
                                WindowManager.LayoutParams.WRAP_CONTENT
                        );
                    }

                    return true; // Fragment 교체는 하지 않음
                }
                else if (id == R.id.nav_map) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, mapFrag)
                            .commit();
                    return true;
                }
                else if (id == R.id.nav_user) {
                    // 런타임에 uid가 생겼다면 최신 uid 다시 전달
                    ensureUserUidOnProfile();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, userFrag)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }

    private void ensureUserUidOnProfile() {
        if (userFrag == null) return;

        if (userUid == null) {
            FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
            if (cur != null) userUid = cur.getUid();
        }

        if (userUid != null) {
            Bundle args = userFrag.getArguments();
            if (args == null) args = new Bundle();
            args.putString("userUid", userUid);
            userFrag.setArguments(args);
        }
    }
}
