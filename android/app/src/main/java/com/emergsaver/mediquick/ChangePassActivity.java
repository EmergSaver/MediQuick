package com.emergsaver.mediquick;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePassActivity extends AppCompatActivity {

    private TextInputLayout tilCurrent, tilNew, tilConfirm;
    private TextInputEditText etCurrent, etNew, etConfirm;
    private AppCompatButton btnChange, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_pass);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_changepw), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tilCurrent = findViewById(R.id.tilCurrent);
        tilNew = findViewById(R.id.tilNew);
        tilConfirm = findViewById(R.id.tilConfirm);
        etCurrent = findViewById(R.id.etCurrent);
        etNew = findViewById(R.id.etNew);
        etConfirm = findViewById(R.id.etConfirm);
        btnChange = findViewById(R.id.btnChange);
        btnCancel = findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> finish());

        btnChange.setOnClickListener(v -> {
            String currentPw = getText(etCurrent);
            String newPw = getText(etNew);
            String confirmPw = getText(etConfirm);

            clearErrors();
            if (!validate(currentPw, newPw, confirmPw)) return;

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || user.getEmail() == null) {
                Toast.makeText(this, "로그인 정보를 확인하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), currentPw))
                    .addOnSuccessListener(aVoid ->
                            user.updatePassword(newPw)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "변경 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    })
                    )
                    .addOnFailureListener(e -> {
                        tilCurrent.setError("현재 비밀번호가 올바르지 않습니다.");
                    });
        });
    }

    // --- 유틸 ---
    private String getText(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void clearErrors() {
        tilCurrent.setError(null);
        tilNew.setError(null);
        tilConfirm.setError(null);
    }

    private boolean validate(@NonNull String cur, @NonNull String nw, @NonNull String cf) {
        if (TextUtils.isEmpty(cur)) {
            tilCurrent.setError("현재 비밀번호를 입력하세요."); return false;
        }
        if (TextUtils.isEmpty(nw)) {
            tilNew.setError("새 비밀번호를 입력하세요."); return false;
        }
        if (!nw.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]{8,20}$")) {
            tilNew.setError("영문+숫자 포함 8~20자"); return false;
        }
        if (!nw.equals(cf)) {
            tilConfirm.setError("새 비밀번호가 일치하지 않습니다."); return false;
        }
        if (nw.equals(cur)) {
            tilNew.setError("현재 비밀번호와 다르게 설정하세요."); return false;
        }
        return true;
    }
}
