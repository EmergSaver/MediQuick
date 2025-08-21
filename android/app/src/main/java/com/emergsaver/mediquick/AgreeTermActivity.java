package com.emergsaver.mediquick;

import android.os.Bundle;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.widget.CheckBox;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

public class AgreeTermActivity extends AppCompatActivity {

    private CheckBox cbTermsService, cbTermsPrivacy, cbTermsMarketing;
    private MaterialButton btnAgree, btnCancel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_agree_term);

        cbTermsService   = findViewById(R.id.cbTermsService);
        cbTermsPrivacy   = findViewById(R.id.cbTermsPrivacy);
        cbTermsMarketing = findViewById(R.id.cbTermsMarketing);
        btnAgree  = findViewById(R.id.btnAgree);
        btnCancel = findViewById(R.id.btnCancel);

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
}