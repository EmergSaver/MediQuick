package com.emergsaver.mediquick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ProfileActivity extends AppCompatActivity {
    private Button btnAllergy;
    private Button btnProfile;
    private Button btnUploadphoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 버튼 객체 생성 (xml의 id와 연결)
        btnAllergy = findViewById(R.id.btn_allergy);
        btnProfile = findViewById(R.id.btn_profile);
        btnUploadphoto = findViewById(R.id.btn_upload_photo);

        // 버튼 클릭 리스너 등록
        btnAllergy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ProfileActivity.this, AllergyActivity.class);
                startActivity(intent); // 화면 전환 실행
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent); // 화면 전환 실행
            }
        });

        btnUploadphoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ProfileActivity.this, UploadPhotoActivity.class);
                startActivity(intent); // 화면 전환 실행
            }
        });


    }
}