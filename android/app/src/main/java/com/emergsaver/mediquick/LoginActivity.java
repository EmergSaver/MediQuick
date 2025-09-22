package com.emergsaver.mediquick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.Account;
import com.kakao.sdk.user.model.Profile;

/**
 * 로그인 화면
 * - 이메일/비번 로그인 (미인증 → CheckEmail 유도)
 * - 자동로그인 복원/저장
 * - 카카오 로그인


 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputLayout tilEmail, tilPw;
    private TextInputEditText etEmail, etPwEdit;
    private Button btLogin;
    private ImageButton btKakao;
    private MaterialButton btSignup;
    private CheckBox cbAuto;
    private TextView loginError;

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
        loginError = findViewById(R.id.loginError);

        restoreAutoFill();

        // ───── 이메일/비번 로그인 ─────
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
                            //auth.signOut(); //  세션을 끊지 않음 (인증 확인 화면에서 유지)
                            Intent i = new Intent(LoginActivity.this, CheckEmail.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            Toast.makeText(this, "이메일 인증 후 로그인 가능합니다.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        //  자동로그인 저장/해제
                        if (cbAuto.isChecked()) saveAutoFill(email, pw, true);
                        else saveAutoFill("", "", false);

                        // 프로필 조회 후 메인 이동
                        String uid = user.getUid();
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    btLogin.setEnabled(true);
                                    String name = doc.getString("name");
                                    String birth = doc.getString("birth");
                                    String bloodType = doc.getString("bloodType");

                                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                    i.putExtra("uid", uid);
                                    i.putExtra("name", name);
                                    i.putExtra("email", email);
                                    i.putExtra("birth", birth);
                                    i.putExtra("bloodType", bloodType);

                                    // 로그인 성공 시 TextView가 나와있다면 안보이게 설정
                                    if(loginError.getVisibility() == VISIBLE) {
                                        loginError.setVisibility(GONE);
                                    }

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
                        // 로그인 실패 시 TextView 보이게 설정
                        loginError.setVisibility(VISIBLE);
                    });
        });

        // ───── 카카오 로그인 ─────
        btKakao.setOnClickListener(v -> {
            UserApiClient.getInstance().loginWithKakaoAccount(this, (token, error) -> {
                if (error != null) {
                    String msg = String.valueOf(error.getMessage()).toLowerCase();
                    if (msg.contains("cancel")) return null;
                    Log.e("KAKAO", "loginWithKakaoAccount FAILED", error);
                    Toast.makeText(this, "카카오 로그인 실패: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    return null;
                }
                if (token == null) return null;

                UserApiClient.getInstance().me((user, meError) -> {
                    if (meError != null || user == null) {
                        Log.e("KAKAO", "me() FAILED", meError);
                        Toast.makeText(this, "사용자 정보 조회 실패", Toast.LENGTH_LONG).show();
                        return null;
                    }

                    final String newKakaoId = String.valueOf(user.getId());
                    Account account = user.getKakaoAccount();

                    String nickname = null;
                    String email    = null;
                    String profile  = null;

                    if (account != null) {
                        Profile p = account.getProfile();
                        if (p != null) {
                            nickname = p.getNickname();
                            profile  = p.getThumbnailImageUrl();
                        }
                        email = account.getEmail();
                    }
                    if (nickname == null) nickname = "kakao_" + newKakaoId;

                    SharedPreferences sp = getSharedPreferences("kakao_user", MODE_PRIVATE);
                    String lastId = sp.getString("kakao_id", null);
                    if (lastId == null || !lastId.equals(newKakaoId)) {
                        clearKakaoCache();
                        try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignore) {}
                    }

                    sp.edit()
                            .putString("kakao_id",  newKakaoId)
                            .putString("nickname",  nickname)
                            .putString("email",     email != null ? email : "")
                            .putString("profileImg",profile != null ? profile : "")
                            .apply();

                    final String kakaoDocId = "kakao_" + newKakaoId;
                    final String nicknameF  = nickname;
                    final String emailF     = email != null ? email : "";

                    FirebaseFirestore.getInstance()
                            .collection("user_by_kakao")
                            .document(kakaoDocId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                String linkedUid = (doc.exists() ? doc.getString("uid") : null);
                                if (linkedUid != null && !linkedUid.isEmpty()) {
                                    goMainWithUid(linkedUid, nicknameF, emailF);
                                } else {
                                    startSignupForKakao(kakaoDocId, nicknameF, emailF);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "user_by_kakao 조회 실패", e);
                                startSignupForKakao(kakaoDocId, nicknameF, emailF);
                            });

                    return null;
                });

                return null;
            });
        });

        btSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );
    }

    /*
     * - 흐름:
     *   (A) 기존 세션 + 이메일 인증 완료 → 바로 MainActivity
     *   (B) 세션 없음 → 저장 자격증명으로 조용히 signIn → 인증 확인 후 MainActivity
     */
    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean auto = sp.getBoolean(KEY_AUTO, false);
        if (!auto) return; // 자동로그인 미사용

        String savedEmail = sp.getString(KEY_EMAIL, "");
        String savedPw    = sp.getString(KEY_PW, "");
        if (TextUtils.isEmpty(savedEmail) || TextUtils.isEmpty(savedPw)) return; // 저장값 없음

        // 중복 동작 방지용 (자동 로그인 시도 동안 수동 로그인 버튼 비활성화)
        if (btLogin != null) btLogin.setEnabled(false);

        FirebaseUser current = auth.getCurrentUser();

        // (A) 기존 세션 존재
        if (current != null) {
            if (current.isEmailVerified()) {
                // 프로필 조회 후 메인 이동
                String uid = current.getUid();
                db.collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            String name = doc.getString("name");
                            String birth = doc.getString("birth");
                            String bloodType = doc.getString("bloodType");

                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                            i.putExtra("uid", uid);
                            i.putExtra("name", name);
                            i.putExtra("email", savedEmail);
                            i.putExtra("birth", birth);
                            i.putExtra("bloodType", bloodType);
                            startActivity(i);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            if (btLogin != null) btLogin.setEnabled(true);
                            Toast.makeText(this, "프로필 조회 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                // 미인증 세션 → 인증 안내 화면으로
                if (btLogin != null) btLogin.setEnabled(true);
                Intent i = new Intent(LoginActivity.this, CheckEmail.class);
                i.putExtra("email", savedEmail);
                startActivity(i);
                Toast.makeText(this, "이메일 인증 후 로그인 가능합니다.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // (B) 세션 없음 → 저장된 자격증명으로 조용히 로그인
        auth.signInWithEmailAndPassword(savedEmail, savedPw)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        if (btLogin != null) btLogin.setEnabled(true);
                        return;
                    }
                    if (!user.isEmailVerified()) {
                        if (btLogin != null) btLogin.setEnabled(true);
                        Intent i = new Intent(LoginActivity.this, CheckEmail.class);
                        i.putExtra("email", savedEmail);
                        startActivity(i);
                        Toast.makeText(this, "이메일 인증 후 로그인 가능합니다.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = user.getUid();
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (btLogin != null) btLogin.setEnabled(true);
                                String name = doc.getString("name");
                                String birth = doc.getString("birth");
                                String bloodType = doc.getString("bloodType");

                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                i.putExtra("uid", uid);
                                i.putExtra("name", name);
                                i.putExtra("email", savedEmail);
                                i.putExtra("birth", birth);
                                i.putExtra("bloodType", bloodType);
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                if (btLogin != null) btLogin.setEnabled(true);
                                Toast.makeText(this, "프로필 조회 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // 자동로그인 실패 → 플래그 해제 및 저장값 삭제
                    if (btLogin != null) btLogin.setEnabled(true);
                    saveAutoFill("", "", false);
                    Toast.makeText(this, "자동 로그인 실패: 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearKakaoCache() {
        getSharedPreferences("kakao_user", MODE_PRIVATE).edit().clear().apply();
    }

    private void startSignupForKakao(@NonNull String kakaoId,
                                     @NonNull String nickname,
                                     @NonNull String email) {
        Intent i = new Intent(LoginActivity.this, SignUpActivity.class);
        i.putExtra("kakao_id", kakaoId);
        i.putExtra("prefill_name", nickname);
        i.putExtra("prefill_email", email);
        startActivity(i);
    }

    private void goMainWithUid(@NonNull String uid,
                               @NonNull String nickname,
                               @NonNull String email) {
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        i.putExtra("uid", uid);
        i.putExtra("kakao_nickname", nickname);
        i.putExtra("email", email);
        startActivity(i);
        finish();
    }

    private String safe(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private boolean isValidEmail(String s) {
        return !TextUtils.isEmpty(s) && Patterns.EMAIL_ADDRESS.matcher(s).matches();
    }

    /**
     *  현재는 SharedPreferences에 평문 저장 (캡스톤/개발 단계)
     *   → 운영 시에는 반드시 EncryptedSharedPreferences로 전환 권장
     */
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
