package com.emergsaver.mediquick;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class CategoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category)
        ;
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_category), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 병원 추천 버튼 찾기
        MaterialButton btnRecommend = findViewById(R.id.btnRecommendHospital);

        // 클릭 이벤트 → RecommendActivity로 이동
        btnRecommend.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, RecommendActivity.class);
            startActivity(intent);
        });
    }

    }
