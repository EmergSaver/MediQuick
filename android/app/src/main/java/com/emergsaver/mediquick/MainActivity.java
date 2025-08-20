package com.emergsaver.mediquick;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import nav.HospitalFragment;
import nav.MapFragment;
import nav.UserFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Fragment 초기화
        Fragment hospitalFrag = new HospitalFragment();
        Fragment mapFrag = new MapFragment();
        Fragment userFrag = new UserFragment();

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
                Fragment selectedFrag = null;

                int id = menuItem.getItemId();
                if(id == R.id.nav_hospital) {
                    selectedFrag = hospitalFrag;
                } else if(id == R.id.nav_map) {
                    selectedFrag = mapFrag;
                } else if(id == R.id.nav_user) {
                    selectedFrag = userFrag;
                }

                if(selectedFrag != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFrag)
                            .commit();
                    return true;
                }

                return false;
            }
        });
    }
}