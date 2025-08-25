package com.emergsaver.mediquick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    // XML id와 1:1 매칭
    private TextInputLayout tilEmail, tilPw;          // insertId, insertPw
    private TextInputEditText etEmail, etPwEdit;      // etId, etPw
    private MaterialButton btLogin, btKakao, btSignup;
    private CheckBox cbAuto;

    private FirebaseFirestore db;

    // 자동로그인에 사용할 SharedPreferences 키
    private static final String PREF_NAME = "mediquick_login_pref";
    private static final String KEY_AUTO   = "auto_login";
    private static final String KEY_EMAIL  = "auto_email";
    private static final String KEY_PW     = "auto_pw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 시스템 바 패딩(당신의 루트 id: main_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_login), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        // 파이어베이스
        db = FirebaseFirestore.getInstance();

        // 뷰 바인딩 (당신의 XML id 그대로)
        tilEmail = findViewById(R.id.insertId);
        tilPw    = findViewById(R.id.insertPw);
        etEmail  = findViewById(R.id.etId);
        etPwEdit = findViewById(R.id.etPw);
        cbAuto   = findViewById(R.id.cbAuto);
        btLogin  = findViewById(R.id.btLogin);
        btKakao  = findViewById(R.id.btKakao);
        btSignup = findViewById(R.id.Signup);

        // 자동 로그인 복원(선택)
        restoreAutoFill();

        // 로그인 버튼
        btLogin.setOnClickListener(v -> {
            // 에러 초기화
            tilEmail.setError(null);
            tilPw.setError(null);

            String email = safe(etEmail);
            String pw    = safe(etPwEdit);

            // 입력 검증
            if (!isValidEmail(email)) {
                tilEmail.setError("이메일 형식을 확인해 주세요.");
                return;
            }
            if (TextUtils.isEmpty(pw)) {
                tilPw.setError("비밀번호를 입력해 주세요.");
                return;
            }

            btLogin.setEnabled(false);

            // Firestore 조회: users 컬렉션에서 email+password 일치 문서 1건
            db.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("password", pw)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs -> {
                        btLogin.setEnabled(true);
                        if (!qs.isEmpty()) {
                            // 일치 → 로그인 성공
                            DocumentSnapshot doc = qs.getDocuments().get(0);

                            // 자동로그인 체크되면 저장
                            if (cbAuto.isChecked()) {
                                saveAutoFill(email, pw, true);
                            } else {
                                saveAutoFill("", "", false);
                            }

                            // 메인으로 이동 (필요 정보 전달)
                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                            i.putExtra("uid", doc.getId());
                            i.putExtra("name", doc.getString("name"));
                            i.putExtra("email", email);
                            i.putExtra("birth", doc.getString("birth"));
                            i.putExtra("bloodType", doc.getString("bloodType"));
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(this, "이메일 또는 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        btLogin.setEnabled(true);
                        Toast.makeText(this, "로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // 회원가입 화면으로(당신의 InsertActivity)
        btSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, InsertActivity.class))
        );

        /*// 카카오 로그인은 나중에 연동(현재 버튼만 연결)
        btKakao.setOnClickListener(v ->
                Toast.makeText(this, "카카오 로그인은 추후 연동 예정입니다.", Toast.LENGTH_SHORT).show()
        );*/
    }

    private String safe(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean isValidEmail(String s) {
        return !TextUtils.isEmpty(s) && Patterns.EMAIL_ADDRESS.matcher(s).matches();
    }

    // ===== 자동로그인 저장/복원(선택) =====
    private void saveAutoFill(String email, String pw, boolean enable) {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        sp.edit()
                .putBoolean(KEY_AUTO, enable)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PW, pw)
                .apply();
    }

    private void restoreAutoFill() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean auto = sp.getBoolean(KEY_AUTO, false);
        String email = sp.getString(KEY_EMAIL, "");
        String pw    = sp.getString(KEY_PW, "");
        cbAuto.setChecked(auto);
        if (auto) {
            etEmail.setText(email);
            etPwEdit.setText(pw);
        }
    }
}
