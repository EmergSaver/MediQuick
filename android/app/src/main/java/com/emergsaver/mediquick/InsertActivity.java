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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InsertActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPw, etPw2; //입력
    private Spinner spyear, spMonth, spday, spAbo, spRh;// 스피너
    private MaterialButton btnOk, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert);
        bindViews();
        setupBirthSpineers();
        setupBloodSpinners();

        btnOk.setOnClickListener(v -> {

        });

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_insert);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_InPro), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}