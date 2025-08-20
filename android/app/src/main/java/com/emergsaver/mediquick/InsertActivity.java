package com.emergsaver.mediquick;


import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InsertActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPw, etPw2; //입력
    private Spinner spYear, spMonth, spDay, spAbo, spRh;// 스피너
    private MaterialButton btnOk, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);                  // ← 시스템 바와 겹치지 않게
        setContentView(R.layout.activity_insert); // ← setContentView는 '한 번만'

        bindViews();             // ← findViewById 모음
        setupBirthSpinners();    // ← 연/월/일 스피너 채우기
        setupBloodSpinners();    // ← 혈액형 스피너 채우기

        // 확인 버튼
        btnOk.setOnClickListener(v -> {
            if (!isValidDate(getSel(spYear), getSel(spMonth), getSel(spDay))) {
                Toast.makeText(this, "생년월일을 확인해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "확인 눌림", Toast.LENGTH_SHORT).show();
            // TODO: 서버 전송 / 저장 로직
            finish();
        });

        // 취소 버튼
        btnCancel.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        // 시스템 바 패딩 적용 (루트 id: main_InPro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
    }

    /*
     * findViewById 모음 (onCreate 밖 '메서드 스코프'로 둬야 함)
     */
    private void bindViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPw = findViewById(R.id.etPw);
        etPw2 = findViewById(R.id.etPw2);

        spYear = findViewById(R.id.spYear);
        spMonth = findViewById(R.id.spMonth);
        spDay = findViewById(R.id.spDay);
        spAbo = findViewById(R.id.spAbo);
        spRh = findViewById(R.id.spRh);

        btnOk = findViewById(R.id.btnOk);
        btnCancel = findViewById(R.id.btnCancel);
    }

    /*
     * 연: 1950~현재, 월: 1~12, 일: 월/윤년 자동 제한
     */
    private void setupBirthSpinners() {
        int curYear = Calendar.getInstance().get(Calendar.YEAR);

        // 연도 (현재~1950)
        List<String> years = new ArrayList<>();
        years.add("년"); // 플레이스홀더
        for (int y = curYear; y >= 1950; y--) years.add(String.valueOf(y));

        // 월 (1~12)
        List<String> months = new ArrayList<>();
        months.add("월");
        for (int m = 1; m <= 12; m++) months.add(String.valueOf(m));

        spYear.setAdapter(simpleAdapter(years));
        spMonth.setAdapter(simpleAdapter(months));

        // 최초 1회: 아직 값 선택 전이므로 0,0으로 호출
        refreshDays(0, 0);

        // 연/월 변경 시 '일' 갱신
        spYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                refreshDays(getSel(spYear), getSel(spMonth));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                refreshDays(getSel(spYear), getSel(spMonth));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /* 선택된 연/월을 보고 일 범위(28/29/30/31) 세팅 */
    private void refreshDays(int y, int m) {
        int max = 31; // 디폴트 31일
        if (y > 0 && m > 0) {
            switch (m) {
                case 2:
                    boolean leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
                    max = leap ? 29 : 28;
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    max = 30;
                    break;
                default:
                    max = 31;
            }
        }
        //  리스트/어댑터는 if문 '밖'에서 항상 세팅해야 초기 호출(0,0)도 표시가 됨
        List<String> days = new ArrayList<>();
        days.add("일");
        for (int d = 1; d <= max; d++) days.add(String.valueOf(d));
        spDay.setAdapter(simpleAdapter(days));
    }

    /*
      혈액형 스피너 채우기 (values/arrays.xml 필요)
     */
    private void setupBloodSpinners() {
        spAbo.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.blood_abo, android.R.layout.simple_spinner_dropdown_item));
        spRh.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.blood_rh, android.R.layout.simple_spinner_dropdown_item));
    }

    /*
      공통 어댑터
     */
    private ArrayAdapter<String> simpleAdapter(List<String> items) {
        ArrayAdapter<String> ad = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return ad;
    }

    /*
     * 현재 스피너 선택값 정수 변환 (플레이스홀더면 0 반환)
     */
    private int getSel(Spinner sp) {
        try {
            return Integer.parseInt(String.valueOf(sp.getSelectedItem()));
        } catch (Exception ignore) {
            return 0;
        }
    }

    /*
     * 날짜 유효성 체크
     */
    private boolean isValidDate(int y, int m, int d) {
        if (y <= 0 || m <= 0 || d <= 0) return false;
        int max;
        switch (m) {
            case 2:
                boolean leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
                max = leap ? 29 : 28;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                max = 30;
                break;
            default:
                max = 31;
        }
        return d <= max;

    }
}









