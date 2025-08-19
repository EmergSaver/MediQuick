package com.emergsaver.mediquick;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton; // ← MaterialButton 사용

public class LoginActivity extends AppCompatActivity {

    private MaterialButton Signup; // ← 이름/타입 정리 (XML의 @id/Signup 버튼)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login); // activity_login.xml 로 연결

        // XML 매핑 (id가 'Signup' 인 MaterialButton)
        Signup = findViewById(R.id.Signup);

        // 회원가입 화면으로 이동
        Signup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, InsertActivity.class);
            startActivity(intent);
        });




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
