package com.emergsaver.mediquick;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CheckEmail extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView tvGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_check_email);

        auth = FirebaseAuth.getInstance();

        tvGuide = findViewById(R.id.tvGuide);
        String email = getIntent().getStringExtra("email");
        if (email != null && tvGuide != null) {
            tvGuide.setText(email + " 로 인증 메일을 보냈습니다.\n메일의 확인(Verify) 링크를 누른 뒤 아래 버튼으로 인증 완료를 확인하세요.");
        }

        // 메일 앱 열기
        findViewById(R.id.btnOpenEmail).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            startActivity(Intent.createChooser(intent, "메일 앱 선택"));
        });

        // 인증 완료 확인 (reload 후 isEmailVerified 재확인)
        findViewById(R.id.btnVerifyDone).setOnClickListener(v -> {
            FirebaseUser u = auth.getCurrentUser();
            if (u == null) {
                // InsertActivity에서 이제는 signOut하지 않지만, 앱 재시작 등으로 세션이 사라졌을 수 있음
                Toast.makeText(this, "세션이 만료되었거나 앱이 재시작되었습니다. 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            u.reload().addOnCompleteListener(t -> {
                if (u.isEmailVerified()) {
                    Toast.makeText(this, "가입완료! 로그인 해주세요", Toast.LENGTH_LONG).show();
                    // 로그인 화면으로 보내기 전에 세션 정리
                    auth.signOut();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(this, "아직 인증되지 않았습니다. 메일의 링크를 눌러주세요.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 인증 메일 재전송
        findViewById(R.id.btnResend).setOnClickListener(v -> {
            FirebaseUser u = auth.getCurrentUser();
            if (u == null) {
                Toast.makeText(this, "세션이 만료되었습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            u.sendEmailVerification().addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    Toast.makeText(this, "인증 메일을 다시 보냈습니다.", Toast.LENGTH_LONG).show();
                } else {
                    String msg = (t.getException() != null) ? t.getException().getMessage() : "재전송 실패";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            });
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_email), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
