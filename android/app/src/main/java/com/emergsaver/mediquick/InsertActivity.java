package com.emergsaver.mediquick;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ▼ 이미 있는 이메일 대응용 다이얼로그/예외
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // ★ 추가: 이미 존재 이메일 대응 다이얼로그
import com.google.firebase.auth.FirebaseAuthUserCollisionException;   // ★ 추가: 이미 존재 이메일 예외

public class InsertActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPw, etPw2, etPhone;
    private TextInputLayout tilName, tilEmail, tilPw, tilPw2, tilPhone;
    private Spinner spYear, spMonth, spDay, spBlood;
    private RadioGroup rgGender;
    private MaterialButton btnOk, btnCancel;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // 카카오 프리필
    private String kakaoIdFromLogin;
    private String prefillName;
    private String prefillEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_insert);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        setupBirthSpinners();
        setupBloodSpinner();

        kakaoIdFromLogin = getIntent().getStringExtra("kakao_id");
        prefillName      = getIntent().getStringExtra("prefill_name");
        prefillEmail     = getIntent().getStringExtra("prefill_email");
        if (!TextUtils.isEmpty(prefillName))  etName.setText(prefillName);
        if (!TextUtils.isEmpty(prefillEmail)) etEmail.setText(prefillEmail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        btnOk.setEnabled(true);
        setupRealtimeValidation();

        AdapterView.OnItemSelectedListener clearOnSelect = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spYear || parent == spMonth) {
                    refreshDays(getSel(spYear), getSel(spMonth));
                }
                clearInlineErrors();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spYear.setOnItemSelectedListener(clearOnSelect);
        spMonth.setOnItemSelectedListener(clearOnSelect);
        spDay.setOnItemSelectedListener(clearOnSelect);

        btnOk.setOnClickListener(v -> {
            if (!validateAndShowErrors()) return;

            String name  = textOf(etName).trim();
            String email = textOf(etEmail).trim();
            String pw    = textOf(etPw).trim();
            String phone = textOf(etPhone).trim();
            int y = getSel(spYear), m = getSel(spMonth), d = getSel(spDay);
            String birth = y + "-" + m + "-" + d;
            String blood = String.valueOf(spBlood.getSelectedItem());
            String gender = getGender();

            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("name", name);
            userProfile.put("email", email);
            userProfile.put("phone", phone);
            userProfile.put("birth", birth);
            userProfile.put("bloodType", blood);
            userProfile.put("gender", gender);

            showTermsBottomSheet(email, pw, userProfile);
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void showTermsBottomSheet(String email, String pw, Map<String, Object> profile) {
        View sheetView = getLayoutInflater().inflate(R.layout.activity_agree_term, null);

        CheckBox cbService   = sheetView.findViewById(R.id.cbTermsService);
        CheckBox cbPrivacy   = sheetView.findViewById(R.id.cbTermsPrivacy);
        CheckBox cbMarketing = sheetView.findViewById(R.id.cbTermsMarketing);
        MaterialButton sheetBtnAgree  = sheetView.findViewById(R.id.btnAgree);
        MaterialButton sheetBtnCancel = sheetView.findViewById(R.id.btnCancel);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);
        dialog.setCanceledOnTouchOutside(false);

        sheetBtnAgree.setOnClickListener(v -> {
            if (!cbService.isChecked() || !cbPrivacy.isChecked()) {
                Toast.makeText(this, "필수 약관에 동의해야 가입할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnOk.setEnabled(false);

            auth.createUserWithEmailAndPassword(email, pw)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            btnOk.setEnabled(true);
                            Toast.makeText(this, "가입 중 오류: 사용자 세션이 없습니다.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        user.sendEmailVerification()
                                .addOnSuccessListener(ignored -> {
                                    String uid = user.getUid();
                                    db.collection("users").document(uid)
                                            .set(profile)
                                            .addOnSuccessListener(x -> {
                                                if (!TextUtils.isEmpty(kakaoIdFromLogin)) {
                                                    Map<String, Object> link = new HashMap<>();
                                                    link.put("uid", uid);
                                                    FirebaseFirestore.getInstance()
                                                            .collection("user_by_kakao")
                                                            .document(kakaoIdFromLogin)
                                                            .set(link)
                                                            .addOnFailureListener(e2 ->
                                                                    Log.w("InsertActivity", "user_by_kakao set failed", e2)
                                                            );
                                                }

                                                dialog.dismiss();
                                                Toast.makeText(this, "인증 메일을 보냈습니다. 메일의 링크로 인증을 완료해 주세요.", Toast.LENGTH_LONG).show();

                                                Intent intent = new Intent(InsertActivity.this, CheckEmail.class);
                                                intent.putExtra("email", user.getEmail());
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                btnOk.setEnabled(true);
                                                Toast.makeText(this, "프로필 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    btnOk.setEnabled(true);
                                    Toast.makeText(this, "인증 메일 발송 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        btnOk.setEnabled(true);
                        // ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼
                        // ★ 변경: '이미 존재하는 이메일'일 때 전용 다이얼로그 제공
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            showEmailAlreadyInUseDialog(email, pw); // ★ 추가 호출
                            return;
                        }
                        // ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲

                        Toast.makeText(this, "가입 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        sheetBtnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupRealtimeValidation() {
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                if (name.matches("^[A-Za-z가-힣]{2,16}$")) {
                    tilName.setError(null);
                    tilName.setBoxStrokeColor(getColor(R.color.teal_700));
                } else {
                    tilName.setError("이름은 2~16자여야 합니다.");
                    tilName.setBoxStrokeColor(getColor(R.color.red));
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilEmail.setError(null);
                    tilEmail.setBoxStrokeColor(getColor(R.color.teal_700));
                } else {
                    tilEmail.setError("이메일 형식을 확인해 주세요.");
                    tilEmail.setBoxStrokeColor(getColor(R.color.red));
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etPw.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String pw = s.toString();
                if (isPasswordValid(pw)) {
                    tilPw.setError(null);
                    tilPw.setBoxStrokeColor(getColor(R.color.teal_700));
                } else {
                    tilPw.setError("8~16자, 영문+숫자 조합이어야 합니다.");
                    tilPw.setBoxStrokeColor(getColor(R.color.red));
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etPw2.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String pw = etPw.getText() != null ? etPw.getText().toString() : "";
                String pw2 = s.toString();
                if (pw.equals(pw2) && isPasswordValid(pw2)) {
                    tilPw2.setError(null);
                    tilPw2.setBoxStrokeColor(getColor(R.color.teal_700));
                } else {
                    tilPw2.setError("비밀번호가 일치하지 않습니다.");
                    tilPw2.setBoxStrokeColor(getColor(R.color.red));
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etPhone.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String d = s.toString().replaceAll("[^0-9]", "");
                if (d.length() == 11) {
                    tilPhone.setError(null);
                    tilPhone.setBoxStrokeColor(getColor(R.color.teal_700));
                } else {
                    tilPhone.setError("전화번호 11자리를 입력하세요.");
                    tilPhone.setBoxStrokeColor(getColor(R.color.red));
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void bindViews() {
        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPw    = findViewById(R.id.etPw);
        etPw2   = findViewById(R.id.etPw2);
        etPhone = findViewById(R.id.etPhone);

        tilName  = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPw    = findViewById(R.id.tilPw);
        tilPw2   = findViewById(R.id.tilPw2);
        tilPhone = findViewById(R.id.tilPhone);

        spYear  = findViewById(R.id.spYear);
        spMonth = findViewById(R.id.spMonth);
        spDay   = findViewById(R.id.spDay);
        spBlood = findViewById(R.id.spBlood);
        rgGender = findViewById(R.id.rgGender);

        btnOk     = findViewById(R.id.btnOk);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupBirthSpinners() {
        int curYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        years.add("년");
        for (int y = curYear; y >= 1950; y--) years.add(String.valueOf(y));
        List<String> months = new ArrayList<>();
        months.add("월");
        for (int m = 1; m <= 12; m++) months.add(String.valueOf(m));
        spYear.setAdapter(simpleAdapter(years));
        spMonth.setAdapter(simpleAdapter(months));
        refreshDays(0, 0);
    }

    private void refreshDays(int y, int m) {
        int max = 31;
        if (y > 0 && m > 0) {
            switch (m) {
                case 2:
                    boolean leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
                    max = leap ? 29 : 28; break;
                case 4: case 6: case 9: case 11:
                    max = 30; break;
                default: max = 31;
            }
        }
        List<String> days = new ArrayList<>();
        days.add("일");
        for (int d = 1; d <= max; d++) days.add(String.valueOf(d));
        spDay.setAdapter(simpleAdapter(days));
    }

    private void setupBloodSpinner() {
        spBlood.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.blood_types, android.R.layout.simple_spinner_dropdown_item));
    }

    private ArrayAdapter<String> simpleAdapter(List<String> items) {
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return ad;
    }

    private int getSel(Spinner sp) {
        try { return Integer.parseInt(String.valueOf(sp.getSelectedItem())); }
        catch (Exception ignore) { return 0; }
    }

    private boolean validateAndShowErrors() {
        clearInlineErrors();
        String name  = textOf(etName).trim();
        String email = textOf(etEmail).trim();
        String pw    = textOf(etPw);
        String pw2   = textOf(etPw2);

        String rawPhone = textOf(etPhone).trim().replaceAll("[^0-9]", "");
        String phone = (rawPhone.length() == 9)
                ? rawPhone.substring(0,3) + "-" + rawPhone.substring(3,7) + "-" + rawPhone.substring(7)
                : textOf(etPhone).trim();
        int y = getSel(spYear), m = getSel(spMonth), d = getSel(spDay);
        boolean ok = true;

        if (!name.matches("^[A-Za-z가-힣]{2,16}$")) {
            if (tilName != null) tilName.setError("이름은 2~16자여야 합니다.");
            ok = false;
        }
        if (!( !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches() )) {
            if (tilEmail != null) tilEmail.setError("이메일 형식을 확인해 주세요.");
            ok = false;
        }
        if (!isPasswordValid(pw)) {
            if (tilPw != null) tilPw.setError("8~16자, 영문+숫자 조합이어야 합니다.");
            ok = false;
        }
        if (!pw.equals(pw2)) {
            if (tilPw2 != null) tilPw2.setError("비밀번호가 일치하지 않습니다.");
            ok = false;
        }
        if (phone.length() != 11) { // 필요 시 rawPhone.length() == 11로 변경 권장
            if (tilPhone != null) tilPhone.setError("전화번호 11자리를 입력하세요.");
            ok = false;
        }
        if (!isValidDate(y, m, d)) {
            Toast.makeText(this, "생년월일을 확인해 주세요.", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        return ok;
    }

    private void clearInlineErrors() {
        if (tilName  != null) tilName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPw    != null) tilPw.setError(null);
        if (tilPw2   != null) tilPw2.setError(null);
        if (tilPhone != null) tilPhone.setError(null);
    }

    private boolean isPasswordValid(String pw) {
        if (TextUtils.isEmpty(pw)) return false;
        if (pw.length() < 8 || pw.length() > 16) return false;
        boolean hasAlpha = pw.matches(".*[A-Za-z].*");
        boolean hasDigit = pw.matches(".*\\d.*");
        boolean hasSpace = pw.matches(".*\\s.*");
        return hasAlpha && hasDigit && !hasSpace;
    }

    private boolean isValidDate(int y, int m, int d) {
        if (y <= 0 || m <= 0 || d <= 0) return false;
        int max;
        switch (m) {
            case 2:
                boolean leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
                max = leap ? 29 : 28; break;
            case 4: case 6: case 9: case 11:
                max = 30; break;
            default: max = 31;
        }
        return d <= max;
    }

    private String textOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }

    private String getGender() {
        int id = rgGender.getCheckedRadioButtonId();
        if (id == R.id.rbMale) return "남성";
        if (id == R.id.rbFemale) return "여성";
        return "";
    }

    // ===========================================================
    // ★ 추가: '이미 가입된 이메일'일 때 선택지 제공 다이얼로그
    //   - 로그인 화면 이동(이메일 프리필 + 힌트)
    //   - 비밀번호 재설정 메일 전송
    //   - (가능 시) 인증메일 다시 보내기
    // ===========================================================
    private void showEmailAlreadyInUseDialog(String email, String pw) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("이미 가입된 이메일")
                .setMessage("해당 이메일로 이미 계정이 존재합니다.\n로그인하여 인증을 완료하거나, 비밀번호를 재설정할 수 있습니다.")
                // ➊ 로그인 화면으로 이동 (이메일 미리 채워주기)
                .setPositiveButton("로그인 화면으로", (d, w) -> {
                    Intent i = new Intent(InsertActivity.this, LoginActivity.class);
                    i.putExtra("prefill_email", email);   // ★ 추가: Login 화면 이메일 프리필
                    i.putExtra("showVerifyHint", true);    // ★ 추가: 안내 토스트 표시 플래그
                    startActivity(i);
                    finish();
                })
                // ➋ 비밀번호 재설정
                .setNegativeButton("비밀번호 재설정", (d, w) -> {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener(v -> Toast.makeText(this, "비밀번호 재설정 메일을 보냈습니다.", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(err -> Toast.makeText(this, "재설정 메일 전송 실패: " + err.getMessage(), Toast.LENGTH_LONG).show());
                })
                // ➌ (선택) 지금 입력한 비번이 맞다면 임시 로그인 후 인증메일 재발송
                .setNeutralButton("인증메일 다시 보내기", (d, w) -> {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pw)
                            .addOnSuccessListener(res -> {
                                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                                if (u != null && !u.isEmailVerified()) {
                                    u.sendEmailVerification()
                                            .addOnSuccessListener(v2 -> {
                                                Toast.makeText(this, "인증 메일을 다시 보냈습니다.", Toast.LENGTH_LONG).show();
                                                Intent i = new Intent(InsertActivity.this, CheckEmail.class);
                                                i.putExtra("email", email);
                                                startActivity(i);
                                                finish();
                                            })
                                            .addOnFailureListener(err -> Toast.makeText(this, "재발송 실패: " + err.getMessage(), Toast.LENGTH_LONG).show());
                                } else {
                                    Toast.makeText(this, "이미 인증된 계정입니다. 로그인해 주세요.", Toast.LENGTH_LONG).show();
                                    Intent i = new Intent(InsertActivity.this, LoginActivity.class);
                                    i.putExtra("prefill_email", email);
                                    startActivity(i);
                                    finish();
                                }
                            })
                            .addOnFailureListener(err -> {
                                Toast.makeText(this, "비밀번호가 일치하지 않습니다. 로그인 화면에서 비밀번호 재설정을 진행해 주세요.", Toast.LENGTH_LONG).show();
                            });
                })
                .show();
    }
}
