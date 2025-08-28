package com.emergsaver.mediquick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class DeleteUsers extends AppCompatActivity {

    private TextInputEditText etPw;
    private CheckBox cbAgree;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_delete_users);

        etPw = findViewById(R.id.etPw);
        cbAgree = findViewById(R.id.cbAgree);
        MaterialButton btnDelete = findViewById(R.id.btnDelete);

        btnDelete.setOnClickListener(v -> startDeleteFlow());
    }

    private void startDeleteFlow() {
        if (!cbAgree.isChecked()) {
            toast("안내 확인을 체크하세요.");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            toast("로그인 상태가 아닙니다.");
            return;
        }

        // 🔧 여기서 pw 변수를 반드시 선언해야 합니다.
        String pw = text(etPw);
        String email = user.getEmail();

        if (email == null || email.isEmpty()) {
            toast("이메일/비밀번호 계정이 아닙니다. 운영 절차로 처리하세요.");
            return;
        }
        if (pw.isEmpty()) {
            toast("비밀번호를 입력하세요.");
            return;
        }

        // 1) 재인증
        AuthCredential cred = EmailAuthProvider.getCredential(email, pw);
        user.reauthenticate(cred).addOnSuccessListener(aVoid -> {
            // 2) Firestore 사용자 데이터 삭제 → 3) 계정 삭제 → 4) 로그인 이동
            deleteUserDataThen(user);
        }).addOnFailureListener(e -> toast("재인증 실패: " + e.getMessage()));
    }

    private void deleteUserDataThen(FirebaseUser user) {
        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference recentRef = db.collection("users").document(uid).collection("recent");
        CollectionReference favRef    = db.collection("users").document(uid).collection("favorites");

        recentRef.get().addOnSuccessListener(recentSnap -> {
            favRef.get().addOnSuccessListener(favSnap -> {
                WriteBatch batch = db.batch();

                for (DocumentSnapshot d : recentSnap.getDocuments()) batch.delete(d.getReference());
                for (DocumentSnapshot d : favSnap.getDocuments())    batch.delete(d.getReference());

                batch.commit().addOnSuccessListener(unused -> finalDeleteAccount(user))
                        .addOnFailureListener(e -> {
                            toast("데이터 삭제 중 오류: " + e.getMessage());
                            finalDeleteAccount(user); // 정책에 따라 강행
                        });
            }).addOnFailureListener(e -> toast("즐겨찾기 조회 실패: " + e.getMessage()));
        }).addOnFailureListener(e -> toast("최근 기록 조회 실패: " + e.getMessage()));
    }

    private void finalDeleteAccount(FirebaseUser user) {
        user.delete().addOnSuccessListener(v -> {

            SharedPreferences sp = getSharedPreferences("mediquick_login_pref", MODE_PRIVATE);
            sp.edit()
                    .putBoolean("auto_login", false)
                    .remove("auto_email")
                    .remove("auto_pw")
                    .apply();

            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }).addOnFailureListener(e -> toast("계정 삭제 실패: " + e.getMessage()));
    }

    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
