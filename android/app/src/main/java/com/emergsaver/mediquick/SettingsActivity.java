package com.emergsaver.mediquick;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;

import com.google.android.material.button.MaterialButton;
import com.emergsaver.mediquick.LogoutActivity;
//import com.emergsaver.mediquick.DeleteAccountActivity;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        MaterialButton btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v ->
                    startActivity(new Intent(this, LogoutActivity.class))
            );
        }
        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v ->
                    startActivity(new Intent(this, DeleteUsers.class))
            );
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}