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
import com.google.firebase.auth.FirebaseAuth;   // ★ Firebase Auth 로그인/인증 기능
import com.google.firebase.auth.FirebaseUser;  // ★ 현재 로그인 사용자 객체
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    //  로그인 화면: 입력 → FirebaseAuth 로그인 → 이메일 인증여부 확인 → Firestore 프로필 불러오기 → 메인화면 이동

    private TextInputLayout tilEmail, tilPw;
    private TextInputEditText etEmail, etPwEdit;
    private MaterialButton btLogin, btKakao, btSignup;
    private CheckBox cbAuto;

    private FirebaseFirestore db; // ★ 사용자 프로필 문서 저장소
    private FirebaseAuth auth;    // ★ Firebase 인증 진입점

    // 자동로그인 저장 키
    private static final String PREF_NAME = "mediquick_login_pref";
    private static final String KEY_AUTO   = "auto_login";
    private static final String KEY_EMAIL  = "auto_email";
    private static final String KEY_PW     = "auto_pw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        //  상태바/네비게이션바 영역 패딩 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_login), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance(); // Firestore 연결
        auth = FirebaseAuth.getInstance();    // ★ Auth 초기화

        //  XML 뷰 연결
        tilEmail = findViewById(R.id.insertId);
        tilPw    = findViewById(R.id.insertPw);
        etEmail  = findViewById(R.id.etId);
        etPwEdit = findViewById(R.id.etPw);
        cbAuto   = findViewById(R.id.cbAuto);
        btLogin  = findViewById(R.id.btLogin);
        btKakao  = findViewById(R.id.btKakao);
        btSignup = findViewById(R.id.Signup);

        restoreAutoFill(); //  저장된 자동로그인 정보 복원

        //  [로그인] 버튼 클릭 시: FirebaseAuth 로그인 → 이메일 인증여부 체크 → Firestore 프로필 불러오기
        btLogin.setOnClickListener(v -> {
            tilEmail.setError(null);
            tilPw.setError(null);

            String email = safe(etEmail);
            String pw    = safe(etPwEdit);

            //  입력값 검증
            if (!isValidEmail(email)) {
                tilEmail.setError("이메일 형식을 확인해 주세요.");
                return;
            }
            if (TextUtils.isEmpty(pw)) {
                tilPw.setError("비밀번호를 입력해 주세요.");
                return;
            }

            btLogin.setEnabled(false); // ★ 중복 클릭 방지

            //  FirebaseAuth 이메일/비번 로그인
            auth.signInWithEmailAndPassword(email, pw)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            btLogin.setEnabled(true);
                            Toast.makeText(this, "로그인 오류: 사용자 세션이 없습니다.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        //  이메일 인증 여부 체크
                        if (!user.isEmailVerified()) {
                            btLogin.setEnabled(true);
                            auth.signOut(); // 인증 안 된 계정은 즉시 로그아웃
                            Intent i = new Intent(LoginActivity.this, CheckEmail.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            Toast.makeText(this, "이메일 인증 후 로그인 가능합니다.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        //  인증 통과 → Firestore 프로필 조회 (users/{uid})
                        String uid = user.getUid();
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    btLogin.setEnabled(true);

                                    // 자동로그인 체크되면 SharedPreferences에 저장
                                    if (cbAuto.isChecked()) {
                                        saveAutoFill(email, pw, true);
                                    } else {
                                        saveAutoFill("", "", false);
                                    }

                                    //  Firestore 프로필 필드 불러오기
                                    String name = doc.getString("name");
                                    String birth = doc.getString("birth");
                                    String bloodType = doc.getString("bloodType");

                                    //  메인 화면으로 이동 + 사용자 정보 전달
                                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                    i.putExtra("uid", uid);
                                    i.putExtra("name", name);
                                    i.putExtra("email", email);
                                    i.putExtra("birth", birth);
                                    i.putExtra("bloodType", bloodType);
                                    startActivity(i);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btLogin.setEnabled(true);
                                    Toast.makeText(this, "프로필 조회 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        btLogin.setEnabled(true);
                        Toast.makeText(this, "로그인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        //  [회원가입] 버튼 → InsertActivity 이동
        btSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, InsertActivity.class))
        );
    }

    // ------------------- 유틸 메서드 -------------------

    //  TextInputEditText에서 안전하게 문자열 꺼내기
    private String safe(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    //  이메일 유효성 검사
    private boolean isValidEmail(String s) {
        return !TextUtils.isEmpty(s) && Patterns.EMAIL_ADDRESS.matcher(s).matches();
    }

    //  자동로그인 정보 저장
    private void saveAutoFill(String email, String pw, boolean enable) {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        sp.edit()
                .putBoolean(KEY_AUTO, enable)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PW, pw)
                .apply();
    }

    //  자동로그인 정보 복원
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
