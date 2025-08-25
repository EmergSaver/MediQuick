package com.emergsaver.mediquick;

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPw, etPw2;
    private TextInputLayout tilName, tilEmail, tilPw, tilPw2;
    private Spinner spYear, spMonth, spDay, spBlood;   // ✅ spBlood 하나만 사용
    private MaterialButton btnOk, btnCancel;

    private FirebaseFirestore db; // Firestore 참조

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_insert);

        db = FirebaseFirestore.getInstance(); // Firestore 초기화

        bindViews();
        setupBirthSpinners();
        setupBloodSpinner();   // ✅ 수정된 메서드 호출

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        btnOk.setEnabled(true);

        // ✅ 실시간 유효성 검사 & 색상 피드백
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

        // ✅ 확인 버튼: 검증 → 약관 바텀시트 → (동의 시) Firestore 저장
        btnOk.setOnClickListener(v -> {
            if (!validateAndShowErrors()) return;

            String name  = textOf(etName).trim();
            String email = textOf(etEmail).trim();
            String pw    = textOf(etPw).trim();
            int y = getSel(spYear), m = getSel(spMonth), d = getSel(spDay);
            String birth = y + "-" + m + "-" + d;
            String blood = String.valueOf(spBlood.getSelectedItem()); // ✅ 하나로 가져오기

            Map<String, Object> user = new HashMap<>();
            user.put("name", name);
            user.put("email", email);
            user.put("password", pw);
            user.put("birth", birth);
            user.put("bloodType", blood);   // ✅ "A+" 이런 값 바로 저장

            showTermsBottomSheet(user); // 🔻 DB 저장 전에 약관 동의부터
        });

        btnCancel.setOnClickListener(v -> finish());
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

    // ------------------- 바텀시트 표시 (약관 동의) -------------------
    private void showTermsBottomSheet(Map<String, Object> userData) {
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
            db.collection("users").add(userData)
                    .addOnSuccessListener(docRef -> {
                        dialog.dismiss();
                        Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
        sheetBtnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ------------------- 이하 기존 유틸 메서드 -------------------
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

    // ✅ 혈액형 스피너: 하나로 합침
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
        int y = getSel(spYear), m = getSel(spMonth), d = getSel(spDay);

        boolean ok = true;
        TextInputLayout firstErr = null;

        if (!name.matches("^[A-Za-z가-힣]{2,16}$")) {
            if (tilName != null) tilName.setError("이름은 2~16자여야 합니다.");
            if (firstErr == null) firstErr = tilName;
            ok = false;
        }
        if (!( !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches() )) {
            if (tilEmail != null) tilEmail.setError("이메일 형식을 확인해 주세요.");
            if (firstErr == null) firstErr = tilEmail;
            ok = false;
        }
        if (!isPasswordValid(pw)) {
            if (tilPw != null) tilPw.setError("8~16자, 영문+숫자 조합이어야 합니다.");
            if (firstErr == null) firstErr = tilPw;
            ok = false;
        }
        if (!pw.equals(pw2)) {
            if (tilPw2 != null) tilPw2.setError("비밀번호가 일치하지 않습니다.");
            if (firstErr == null) firstErr = tilPw2;
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
}
