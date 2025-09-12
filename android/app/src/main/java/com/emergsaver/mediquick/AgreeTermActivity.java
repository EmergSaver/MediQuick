package com.emergsaver.mediquick;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AgreeTermActivity extends AppCompatActivity {

    private CheckBox cbTermsService, cbTermsPrivacy, cbTermsMarketing;
    private MaterialButton btnAgree, btnCancel;
    private TextView btnViewService, btnViewPrivacy, btnViewMarketing;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_agree_term);

        cbTermsService   = findViewById(R.id.cbTermsService);
        cbTermsPrivacy   = findViewById(R.id.cbTermsPrivacy);
        cbTermsMarketing = findViewById(R.id.cbTermsMarketing);
        btnAgree         = findViewById(R.id.btnAgree);
        btnCancel        = findViewById(R.id.btnCancel);
        btnViewService   = findViewById(R.id.btnViewService);
        btnViewPrivacy   = findViewById(R.id.btnViewPrivacy);
        btnViewMarketing = findViewById(R.id.btnViewMarketing);

        // 보기 버튼 클릭 이벤트
        btnViewService.setOnClickListener(v -> showTermsDialog("이용약관", R.raw.terms_service));
        btnViewPrivacy.setOnClickListener(v -> showTermsDialog("개인정보 처리방침", R.raw.terms_privacy));
        btnViewMarketing.setOnClickListener(v -> showTermsDialog("마케팅 정보 수신 동의", R.raw.terms_marketing));
        Log.d("AgreeTerm", "btnViewService: " + btnViewService);
        Log.d("AgreeTerm", "btnViewPrivacy: " + btnViewPrivacy);
        Log.d("AgreeTerm", "btnViewMarketing: " + btnViewMarketing);

        // 동의 버튼
        btnAgree.setOnClickListener(v -> {
            if (!cbTermsService.isChecked() || !cbTermsPrivacy.isChecked()) {
                Toast.makeText(this, "필수 약관에 동의해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent data = new Intent();
            data.putExtra("agree_service", cbTermsService.isChecked());
            data.putExtra("agree_privacy", cbTermsPrivacy.isChecked());
            data.putExtra("agree_marketing", cbTermsMarketing.isChecked());
            setResult(RESULT_OK, data);
            finish();
        });

        // 취소 버튼
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_agree), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 약관 전문 Dialog
    private void showTermsDialog(String title, int rawResId) {
        StringBuilder text = new StringBuilder();
        try (InputStream inputStream = getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) { // ★ UTF-8 강제
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text.toString())
                .setPositiveButton("닫기", null)
                .show();
    }
}
