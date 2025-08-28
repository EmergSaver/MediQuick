package com.emergsaver.mediquick;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import nav.CategoryFragment;
import nav.MapFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Fragment 초기화
        Fragment mapFrag = new MapFragment();
        Fragment userFrag = new ProfileFragment();

        // 첫 번째 화면일 경우 (지도 보기)
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mapFrag).commit();

            // 지도 탭을 선택 상태로 변경
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
                        // SearchActivity로 이동
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new CategoryFragment())
                                .commit();
                    });

                    dialog.show();
                    return true; // Fragment 교체는 하지 않음
                }
                else if (id == R.id.nav_map) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, mapFrag)
                            .commit();
                    return true;
                }
                else if (id == R.id.nav_user) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, userFrag)
                            .commit();
                    return true;
                }

                return false;
            }
        });
    }
}
