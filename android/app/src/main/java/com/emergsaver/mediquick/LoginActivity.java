package com.emergsaver.mediquick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPw;
    private TextInputEditText etEmail, etPwEdit;
    private MaterialButton btLogin, btKakao, btSignup;
    private CheckBox cbAuto;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private static final String PREF_NAME = "mediquick_login_pref";
    private static final String KEY_AUTO   = "auto_login";
    private static final String KEY_EMAIL  = "auto_email";
    private static final String KEY_PW     = "auto_pw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_login), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.insertId);
        tilPw    = findViewById(R.id.insertPw);
        etEmail  = findViewById(R.id.etId);
        etPwEdit = findViewById(R.id.etPw);
        cbAuto   = findViewById(R.id.cbAuto);
        btLogin  = findViewById(R.id.btLogin);
        btKakao  = findViewById(R.id.btKakao);
        btSignup = findViewById(R.id.Signup);

        restoreAutoFill();

        // ───────────────── 이메일/비번 로그인 ─────────────────
        btLogin.setOnClickListener(v -> {
            tilEmail.setError(null);
            tilPw.setError(null);

            String email = safe(etEmail);
            String pw    = safe(etPwEdit);

            if (!isValidEmail(email)) {
                tilEmail.setError("이메일 형식을 확인해 주세요.");
                return;
            }
            if (TextUtils.isEmpty(pw)) {
                tilPw.setError("비밀번호를 입력해 주세요.");
                return;
            }

            btLogin.setEnabled(false);

            auth.signInWithEmailAndPassword(email, pw)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            btLogin.setEnabled(true);
                            Toast.makeText(this, "로그인 오류: 사용자 세션이 없습니다.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!user.isEmailVerified()) {
                            btLogin.setEnabled(true);
                            auth.signOut();
                            Intent i = new Intent(LoginActivity.this, CheckEmail.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            Toast.makeText(this, "이메일 인증 후 로그인 가능합니다.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String uid = user.getUid();
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    btLogin.setEnabled(true);
                                    if (cbAuto.isChecked()) saveAutoFill(email, pw, true);
                                    else saveAutoFill("", "", false);

                                    String name = doc.getString("name");
                                    String birth = doc.getString("birth");
                                    String bloodType = doc.getString("bloodType");

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

        // ───────────────── 카카오 로그인(B안) ─────────────────
        btKakao.setOnClickListener(v -> {
            if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
                UserApiClient.getInstance().loginWithKakaoTalk(this, (token, error) -> {
                    if (error != null) {
                        Log.e("KAKAO", "loginWithKakaoTalk error: " + error.getMessage(), error);
                        UserApiClient.getInstance().loginWithKakaoAccount(this, (t2, e2) -> {
                            handleKakaoResult(t2, e2);
                            return null;
                        });
                    } else {
                        handleKakaoResult(token, null);
                    }
                    return null;
                });
            } else {
                UserApiClient.getInstance().loginWithKakaoAccount(this, (token, error) -> {
                    handleKakaoResult(token, error);
                    return null;
                });
            }
        });

        btSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, InsertActivity.class))
        );
    }

    // ───────────────── 카카오 콜백 공통 처리 ─────────────────
    private void handleKakaoResult(OAuthToken token, Throwable error) {
        if (error != null) {
            Toast.makeText(this, "카카오 로그인 실패: " + error.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("KAKAO", "login error", error);
            return;
        }
        if (token == null) {
            Toast.makeText(this, "카카오 토큰이 비어 있습니다.", Toast.LENGTH_LONG).show();
            Log.e("KAKAO", "OAuthToken is null");
            return;
        }
        String kakaoAccessToken = token.getAccessToken();
        sendKakaoTokenToFirebase(kakaoAccessToken);
    }

    // ───────────────── 카카오 토큰 → Functions → 커스텀 토큰 ─────────────────

    private void sendKakaoTokenToFirebase(String kakaoAccessToken) {
        // ➊ 디버그: 앱이 바라보는 Firebase 프로젝트/스위치/리전 확인
        String projectId = com.google.firebase.FirebaseApp.getInstance().getOptions().getProjectId();
        Log.d("FUNC", "projectId=" + projectId
                + " USE_EMU=" + BuildConfig.USE_FUNCTIONS_EMULATOR
                + " region=asia-northeast3");

        // ➋ 리전 고정 (에뮬/운영 공통)
        FirebaseFunctions functions = FirebaseFunctions.getInstance("asia-northeast3");

        // ➌ 에뮬레이터일 때만 로컬로 연결 (AVD면 10.0.2.2, 실기기면 PC IP)
        if (BuildConfig.USE_FUNCTIONS_EMULATOR) {
            functions.useEmulator("10.0.2.2", BuildConfig.FUNCTIONS_EMULATOR_PORT);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("token", kakaoAccessToken);

        functions
                .getHttpsCallable("kakaoLogin")
                .call(data)
                .addOnSuccessListener((HttpsCallableResult result) -> {
                    Map map = (Map) result.getData();
                    String customToken = (String) map.get("token");
                    FirebaseAuth.getInstance()
                            .signInWithCustomToken(customToken)
                            .addOnSuccessListener(authResult -> {
                                FirebaseUser user = authResult.getUser();
                                if (user == null) {
                                    Toast.makeText(this, "로그인 오류: 사용자 세션 없음", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                String uid = user.getUid();
                                db.collection("users").document(uid).get()
                                        .addOnSuccessListener(doc -> {
                                            String name = doc.getString("name");
                                            String birth = doc.getString("birth");
                                            String bloodType = doc.getString("bloodType");

                                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                            i.putExtra("uid", uid);
                                            i.putExtra("name", name);
                                            i.putExtra("email", user.getEmail()); // 카카오는 null일 수 있음
                                            i.putExtra("birth", birth);
                                            i.putExtra("bloodType", bloodType);
                                            startActivity(i);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "프로필 조회 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e("Firestore", "get user profile failed", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirebaseAuth", "signInWithCustomToken 실패", e);
                                Toast.makeText(this, "Firebase 로그인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Functions", "kakaoLogin 호출 실패", e);
                    Toast.makeText(this, "Function 호출 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    // ───────────────── 유틸 ─────────────────
    private String safe(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean isValidEmail(String s) {
        return !TextUtils.isEmpty(s) && Patterns.EMAIL_ADDRESS.matcher(s).matches();
    }

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
