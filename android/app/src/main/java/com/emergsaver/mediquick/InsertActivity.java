package com.emergsaver.mediquick;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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

public class InsertActivity extends AppCompatActivity {

    //  회원가입 화면: 폼 입력 → 약관 동의 → FirebaseAuth 가입/인증메일 → Firestore 프로필 저장 → 인증 대기 화면 이동

    private TextInputEditText etName, etEmail, etPw, etPw2;
    private TextInputLayout tilName, tilEmail, tilPw, tilPw2;
    private Spinner spYear, spMonth, spDay, spBlood;
    private MaterialButton btnOk, btnCancel;

    private FirebaseFirestore db;
    private FirebaseAuth auth; // ★ 이메일/비번 인증(Auth) 진입점

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_insert);

        db = FirebaseFirestore.getInstance(); // Firestore 초기화
        auth = FirebaseAuth.getInstance(); //  Auth 초기화 (이메일/비번 가입/로그인/인증메일에 사용)

        bindViews();
        setupBirthSpinners();
        setupBloodSpinner();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        btnOk.setEnabled(true);
        setupRealtimeValidation(); // ✅ 실시간 유효성 검사 & 색상 피드백

        AdapterView.OnItemSelectedListener clearOnSelect = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spYear || parent == spMonth) {
                    refreshDays(getSel(spYear), getSel(spMonth)); // ★ 연/월 바뀌면 일수 갱신
                }
                clearInlineErrors(); //  선택 시 인라인 에러 제거
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spYear.setOnItemSelectedListener(clearOnSelect);
        spMonth.setOnItemSelectedListener(clearOnSelect);
        spDay.setOnItemSelectedListener(clearOnSelect);

        //  [확인] 클릭 흐름: 입력 검증 → 약관 BottomSheet → (동의) Auth 가입 → 인증메일 발송 → Firestore 저장 → 인증 대기 화면 이동
        btnOk.setOnClickListener(v -> {
            if (!validateAndShowErrors()) return; // ★ 폼 검증 실패 시 중단

            String name  = textOf(etName).trim();
            String email = textOf(etEmail).trim();
            String pw    = textOf(etPw).trim();
            int y = getSel(spYear), m = getSel(spMonth), d = getSel(spDay);
            String birth = y + "-" + m + "-" + d; // ★ YYYY-M-D 형식
            String blood = String.valueOf(spBlood.getSelectedItem());

            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("name", name);
            userProfile.put("email", email);
            userProfile.put("birth", birth);
            userProfile.put("bloodType", blood);

            //  약관 동의가 완료되면 실제 가입/인증/저장 진행
            showTermsBottomSheet(email, pw, userProfile);
        });

        btnCancel.setOnClickListener(v -> finish()); //  취소 시 화면 종료
    }

    // ------------------- 약관 동의 바텀시트 -------------------
    private void showTermsBottomSheet(String email, String pw, Map<String, Object> profile) {
        View sheetView = getLayoutInflater().inflate(R.layout.activity_agree_term, null);

        CheckBox cbService   = sheetView.findViewById(R.id.cbTermsService);
        CheckBox cbPrivacy   = sheetView.findViewById(R.id.cbTermsPrivacy);
        CheckBox cbMarketing = sheetView.findViewById(R.id.cbTermsMarketing);
        MaterialButton sheetBtnAgree  = sheetView.findViewById(R.id.btnAgree);
        MaterialButton sheetBtnCancel = sheetView.findViewById(R.id.btnCancel);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);
        dialog.setCanceledOnTouchOutside(false); // ★ 바깥 터치로 닫히지 않게

        sheetBtnAgree.setOnClickListener(v -> {
            //  필수 약관 동의 확인
            if (!cbService.isChecked() || !cbPrivacy.isChecked()) {
                Toast.makeText(this, "필수 약관에 동의해야 가입할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnOk.setEnabled(false); //  중복 클릭 방지

            //  1) FirebaseAuth에 이메일/비밀번호로 계정 생성
            auth.createUserWithEmailAndPassword(email, pw)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            btnOk.setEnabled(true);
                            Toast.makeText(this, "가입 중 오류: 사용자 세션이 없습니다.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        //  2) 인증 메일 보내기 (사용자 메일함으로 발송)
                        user.sendEmailVerification()
                                .addOnSuccessListener(ignored -> {
                                    String uid = user.getUid();

                                    //  3) Firestore에 프로필 문서 저장 (비밀번호는 저장하지 않음)
                                    db.collection("users").document(uid)
                                            .set(profile)
                                            .addOnSuccessListener(x -> {
                                                dialog.dismiss();
                                                Toast.makeText(this, "가입 완료! 인증 메일을 확인하세요.", Toast.LENGTH_LONG).show();

                                                //  4) 인증 대기 화면으로 이동 (사용자가 메일의 링크를 누른 뒤, 앱에서 확인하도록 유도)
                                                Intent intent = new Intent(InsertActivity.this, CheckEmail.class);
                                                intent.putExtra("email", user.getEmail()); // ★ 안내용 이메일 전달
                                                startActivity(intent);
                                                finish(); //  가입 화면 종료 (뒤로가기 방지)
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
                        Toast.makeText(this, "가입 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        sheetBtnCancel.setOnClickListener(v -> dialog.dismiss()); //  약관 시트 닫기
        dialog.show();
    }

    // ------------------- 실시간 유효성 검사 -------------------
    private void setupRealtimeValidation() {
        // 이름 검사
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                if (name.matches("^[A-Za-z가-힣]{2,16}$")) {
                    tilName.setError(null);
                    tilName.setBoxStrokeColor(getColor(R.color.teal_700)); // ✅ 초록
                } else {
                    tilName.setError("이름은 2~16자여야 합니다.");
                    tilName.setBoxStrokeColor(getColor(R.color.red)); // ❌ 빨강
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // 이메일 검사
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

        // 비밀번호 검사
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

        // 비밀번호 확인 검사
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
    }

    // ------------------- 이하 유틸 -------------------
    private void bindViews() {
        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPw    = findViewById(R.id.etPw);
        etPw2   = findViewById(R.id.etPw2);

        tilName  = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPw    = findViewById(R.id.tilPw);
        tilPw2   = findViewById(R.id.tilPw2);

        spYear  = findViewById(R.id.spYear);
        spMonth = findViewById(R.id.spMonth);
        spDay   = findViewById(R.id.spDay);
        spBlood = findViewById(R.id.spBlood);

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
        refreshDays(0, 0); // ★ 기본값: '월/년' 선택 전이라 0으로 일수 초기화
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
        catch (Exception ignore) { return 0; } // ★ "년/월/일" 같은 플레이스홀더 선택 시 NumberFormat 예외 방지
    }

    private boolean validateAndShowErrors() {
        clearInlineErrors();
        String name  = textOf(etName).trim();
        String email = textOf(etEmail).trim();
        String pw    = textOf(etPw);
        String pw2   = textOf(etPw2);
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
        if (!isValidDate(y, m, d)) {
            Toast.makeText(this, "생년월일을 확인해 주세요.", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        return ok; //  하나라도 실패하면 false → 상단 onClick에서 리턴
    }

    private void clearInlineErrors() {
        if (tilName  != null) tilName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPw    != null) tilPw.setError(null);
        if (tilPw2   != null) tilPw2.setError(null);
    }

    private boolean isPasswordValid(String pw) {
        if (TextUtils.isEmpty(pw)) return false;
        if (pw.length() < 8 || pw.length() > 16) return false;
        boolean hasAlpha = pw.matches(".*[A-Za-z].*");
        boolean hasDigit = pw.matches(".*\\d.*");
        boolean hasSpace = pw.matches(".*\\s.*");
        return hasAlpha && hasDigit && !hasSpace; // ★ 공백 금지
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
}
