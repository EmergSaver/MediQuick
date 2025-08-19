package com.emergsaver.mediquick;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecommendActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recommend);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recommend_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btn = findViewById(R.id.btnGoDetail);
        btn.setOnClickListener(v -> {
            Intent intent = new Intent(RecommendActivity.this, DetailHospitalActivity.class);
            // 필요하면 샘플 데이터 전달
            intent.putExtra("hospitalId", "sample-001");
            startActivity(intent);
        });
    }
}