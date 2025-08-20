package com.emergsaver.mediquick;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InsertActivity extends AppCompatActivity {

    // 입력 필드
    private TextInputEditText etName, etEmail, etPw, etPw2;

    // (선택) 에러를 예쁘게 보여줄 TextInputLayout (XML에 있다면 자동으로 연결됨)
    private TextInputLayout tilName, tilEmail, tilPw, tilPw2;

    // 스피너
    private Spinner spYear, spMonth, spDay, spAbo, spRh;

    // 버튼
    private MaterialButton btnOk, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_insert); //

        bindViews();            // findViewById 모음
        setupBirthSpinners();   // 연/월/일 스피너 세팅 (윤년/말일 반영)
        setupBloodSpinners();   // 혈액형 스피너 세팅

        // 시스템바(노치) 패딩 적용 — setContentView 이후, 한 번만 등록
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        //  처음엔 비활성화: 모든 요구조건 만족 전까지 버튼 잠금
        btnOk.setEnabled(false);

        // 텍스트 변경 시마다 활성화 조건 갱신
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSubmitEnabled(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etName.addTextChangedListener(watcher);
        etEmail.addTextChangedListener(watcher);
        etPw.addTextChangedListener(watcher);
        etPw2.addTextChangedListener(watcher);

        // 스피너 변경 시에도 갱신
        AdapterView.OnItemSelectedListener spinListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                // 연/월 스피너는 '일자' 목록 갱신 + 제출 가능 여부 갱신
                if (parent == spYear || parent == spMonth) {
                    refreshDays(getSel(spYear), getSel(spMonth));
                }
                updateSubmitEnabled();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spYear.setOnItemSelectedListener(spinListener);
        spMonth.setOnItemSelectedListener(spinListener);
        spDay.setOnItemSelectedListener(spinListener);

        // 확인 버튼: 최종 방어로 한번 더 날짜 체크
        btnOk.setOnClickListener(v -> {
            // (유지) 최종 방어: 생년월일 유효성
            if (!isValidDate(getSel(spYear), getSel(spMonth), getSel(spDay))) {
                showInlineError(null, "생년월일을 확인해 주세요.");
                Toast.makeText(this, "생년월일을 확인해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // (선택) 필요하면 입력값을 약관 화면에 넘길 수 있습니다. 지금은 화면전환만.
            Intent intent = new Intent(InsertActivity.this, AgreeTermActivity.class);
            startActivity(intent);     // ← 로그인→회원가입 때와 동일한 전환 방식
            // finish();  // ← 지금 단계에선 종료하지 않습니다.
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void showInlineError(Object o, String s) {
    }

    /** findViewById 모음 */
    private void bindViews() {
        // EditText
        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPw    = findViewById(R.id.etPw);
        etPw2   = findViewById(R.id.etPw2);

        // (옵션) TextInputLayout — XML에 없으면 null로 남음
        tilName  = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPw    = findViewById(R.id.tilPw);
        tilPw2   = findViewById(R.id.tilPw2);

        // Spinner
        spYear  = findViewById(R.id.spYear);
        spMonth = findViewById(R.id.spMonth);
        spDay   = findViewById(R.id.spDay);
        spAbo   = findViewById(R.id.spAbo);
        spRh    = findViewById(R.id.spRh);

        // Buttons
        btnOk     = findViewById(R.id.btnOk);
        btnCancel = findViewById(R.id.btnCancel);
    }

    /** 연: 1950~현재, 월: 1~12, 일: 월/윤년에 따라 자동 제한 */
    private void setupBirthSpinners() {
        int curYear = Calendar.getInstance().get(Calendar.YEAR);

        // 연도 (현재 → 1950)
        List<String> years = new ArrayList<>();
        years.add("년"); // 플레이스홀더
        for (int y = curYear; y >= 1950; y--) years.add(String.valueOf(y));

        // 월 (1 ~ 12)
        List<String> months = new ArrayList<>();
        months.add("월");
        for (int m = 1; m <= 12; m++) months.add(String.valueOf(m));

        spYear.setAdapter(simpleAdapter(years));
        spMonth.setAdapter(simpleAdapter(months));

        // 최초 1회: 일자 목록 생성(선택 전이므로 0,0)
        refreshDays(0, 0);
    }

    /** 선택된 연/월을 보고 일 범위(28/29/30/31) 세팅 */
    private void refreshDays(int y, int m) {
        int max = 31; // 디폴트
        if (y > 0 && m > 0) {
            switch (m) {
                case 2:
                    boolean leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
                    max = leap ? 29 : 28; break;
                case 4: case 6: case 9: case 11:
                    max = 30; break;
                default:
                    max = 31;
            }
        }
        List<String> days = new ArrayList<>();
        days.add("일"); // 플레이스홀더
        for (int d = 1; d <= max; d++) days.add(String.valueOf(d));
        spDay.setAdapter(simpleAdapter(days));
    }

    /** 혈액형 스피너 — values/strings.xml 또는 values/arrays.xml 에 배열 필요 */
    private void setupBloodSpinners() {
        spAbo.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.blood_abo, android.R.layout.simple_spinner_dropdown_item));
        spRh.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.blood_rh, android.R.layout.simple_spinner_dropdown_item));
    }

    /** 공통 어댑터 */
    private ArrayAdapter<String> simpleAdapter(List<String> items) {
        ArrayAdapter<String> ad = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return ad;
    }

    /** 현재 스피너 선택값 정수 변환 (플레이스홀더면 0) */
    private int getSel(Spinner sp) {
        try { return Integer.parseInt(String.valueOf(sp.getSelectedItem())); }
        catch (Exception ignore) { return 0; }
    }

    // ========= 검증/활성화 로직 (요구사항 반영) =========

    /** 모든 입력이 조건을 만족할 때만 확인 버튼 활성화 */
    private void updateSubmitEnabled() {
        clearInlineErrors(); // 실시간 갱신 전에 기존 에러 지움(선택)

        String name = textOf(etName).trim();
        String email = textOf(etEmail).trim();
        String pw = textOf(etPw);
        String pw2 = textOf(etPw2);
        int y = getSel(spYear), m = getSel(spMonth), d = getSel(spDay);

        boolean nameOk  = name.matches("^[A-Za-z가-힣]{2,16}$");                 // 2~16자, 한/영
        boolean emailOk = isEmailValid(email);                                   // 이메일 형식
        boolean pwOk    = isPasswordValid(pw);                                   // 8~16자, 영문+숫자, 공백X
        boolean pw2Ok   = pw.equals(pw2);                                        // 일치
        boolean birthOk = isValidDate(y, m, d);                                  // 윤년/말일

        // (선택) 인라인 에러 표시 — TextInputLayout이 존재할 때만
        if (tilName  != null && !nameOk)  tilName.setError("이름은 2~16자여야 합니다.");
        if (tilEmail != null && !emailOk) tilEmail.setError("이메일 형식을 확인해 주세요.");
        if (tilPw    != null && !pwOk)    tilPw.setError("8~16자, 영문+숫자 조합이어야 합니다.");
        if (tilPw2   != null && !pw2Ok)   tilPw2.setError("비밀번호가 일치하지 않습니다.");

        boolean allOk = nameOk && emailOk && pwOk && pw2Ok && birthOk;
        btnOk.setEnabled(allOk);
    }

    private void clearInlineErrors() {
        if (tilName  != null) tilName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPw    != null) tilPw.setError(null);
        if (tilPw2   != null) tilPw2.setError(null);
    }

    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String pw) {
        if (TextUtils.isEmpty(pw)) return false;
        if (pw.length() < 8 || pw.length() > 16) return false;
        boolean hasAlpha = pw.matches(".*[A-Za-z].*");
        boolean hasDigit = pw.matches(".*\\d.*");
        boolean hasSpace = pw.matches(".*\\s.*");
        return hasAlpha && hasDigit && !hasSpace;
    }

    /** 날짜 유효성 체크 (윤년/말일 포함) */
    private boolean isValidDate(int y, int m, int d) {
        if (y <= 0 || m <= 0 || d <= 0) return false;
        int max;
        switch (m) {
            case 2:
                boolean leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
                max = leap ? 29 : 28; break;
            case 4: case 6: case 9: case 11:
                max = 30; break;
            default:
                max = 31;
        }
        return d <= max;
    }

    private String textOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }
}
