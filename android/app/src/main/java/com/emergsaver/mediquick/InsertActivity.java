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

    // 에러 표시용 TextInputLayout (XML에 있으면 연결됨)
    private TextInputLayout tilName, tilEmail, tilPw, tilPw2;

    // 스피너
    private Spinner spYear, spMonth, spDay, spAbo, spRh;

    // 버튼
    private MaterialButton btnOk, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_insert);

        bindViews();
        setupBirthSpinners();   // 연/월/일 (윤년/말일 반영)
        setupBloodSpinners();   // 혈액형


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        //  버튼은 항상 활성화 (누를 때만 검증/에러 표시)
        btnOk.setEnabled(true);

        //  입력이 바뀌면 '기존 에러만' 지우기 (새 에러는 표시하지 않음)
        TextWatcher clearOnly = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { clearInlineErrors(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etName.addTextChangedListener(clearOnly);
        etEmail.addTextChangedListener(clearOnly);
        etPw.addTextChangedListener(clearOnly);
        etPw2.addTextChangedListener(clearOnly);

        // 스피너: 연/월 바뀌면 일자 갱신 + 에러만 지움
        AdapterView.OnItemSelectedListener clearOnSelect = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
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

        //  확인 버튼: 이때만 검증 + 에러 표시
        btnOk.setOnClickListener(v -> {
            if (!validateAndShowErrors()) {
                // 에러가 있으면 여기서 중단 (포커스/스크롤 이동까지 수행됨)
                return;
            }
            // 통과 시 다음 화면(약관 동의) 등으로 진행
            Intent intent = new Intent(InsertActivity.this, AgreeTermActivity.class);
            startActivity(intent);
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    /** findViewById 모음 */
    private void bindViews() {
        // EditText
        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPw    = findViewById(R.id.etPw);
        etPw2   = findViewById(R.id.etPw2);

        // TextInputLayout (없으면 null)
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

        List<String> years = new ArrayList<>();
        years.add("년"); // 플레이스홀더
        for (int y = curYear; y >= 1950; y--) years.add(String.valueOf(y));

        List<String> months = new ArrayList<>();
        months.add("월");
        for (int m = 1; m <= 12; m++) months.add(String.valueOf(m));

        spYear.setAdapter(simpleAdapter(years));
        spMonth.setAdapter(simpleAdapter(months));

        // 최초 1회: 선택 전이므로 0,0
        refreshDays(0, 0);
    }

    /** 선택된 연/월을 보고 일 범위(28/29/30/31) 세팅 */
    private void refreshDays(int y, int m) {
        int max = 31;
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
        days.add("일");
        for (int d = 1; d <= max; d++) days.add(String.valueOf(d));
        spDay.setAdapter(simpleAdapter(days));
    }

    /** 혈액형 스피너 — 배열 리소스 필요 */
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


    private int getSel(Spinner sp) {
        try { return Integer.parseInt(String.valueOf(sp.getSelectedItem())); }
        catch (Exception ignore) { return 0; }
    }



    /** 확인 버튼 클릭시에만 에러를 표시한다. 통과하면 true 반환 */
    private boolean validateAndShowErrors() {
        clearInlineErrors();

        String name  = textOf(etName).trim();
        String email = textOf(etEmail).trim();
        String pw    = textOf(etPw);
        String pw2   = textOf(etPw2);
        int y = getSel(spYear), m = getSel(spMonth), d = getSel(spDay);

        boolean ok = true;
        TextInputLayout firstErr = null;

        // 이름: 2~16자, 한/영
        if (!name.matches("^[A-Za-z가-힣]{2,16}$")) {
            if (tilName != null) tilName.setError("이름은 2~16자여야 합니다.");
            if (firstErr == null) firstErr = tilName;
            ok = false;
        }

        // 이메일 형식
        if (!( !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches() )) {
            if (tilEmail != null) tilEmail.setError("이메일 형식을 확인해 주세요.");
            if (firstErr == null) firstErr = tilEmail;
            ok = false;
        }

        // 비밀번호 (8~16자, 영문+숫자, 공백X)
        if (!isPasswordValid(pw)) {
            if (tilPw != null) tilPw.setError("8~16자, 영문+숫자 조합이어야 합니다.");
            if (firstErr == null) firstErr = tilPw;
            ok = false;
        }

        // 비밀번호 확인 일치
        if (!pw.equals(pw2)) {
            if (tilPw2 != null) tilPw2.setError("비밀번호가 일치하지 않습니다.");
            if (firstErr == null) firstErr = tilPw2;
            ok = false;
        }

        // 생년월일
        if (!isValidDate(y, m, d)) {
            Toast.makeText(this, "생년월일을 확인해 주세요.", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        // 첫 에러 위치로 포커스/스크롤
        if (!ok && firstErr != null) {
            firstErr.requestFocus();
            TextInputLayout finalFirstErr = firstErr;
            findViewById(R.id.main_InPro).post(() ->
                    ((androidx.core.widget.NestedScrollView) findViewById(R.id.main_InPro))
                            .smoothScrollTo(0, finalFirstErr.getTop() - 60)
            );
        }

        return ok;
    }

    /** 기존 에러 문구 지우기 (입력/선택이 바뀔 때만 호출) */
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
