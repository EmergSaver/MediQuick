package com.emergsaver.mediquick;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        MaterialButton btnChangePassword = findViewById(R.id.btnChangePw);
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
        if (btnChangePassword != null) {
            boolean hasPasswordProvider = false;
            FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
            if (cur != null) {
                for (com.google.firebase.auth.UserInfo p : cur.getProviderData()) {
                    if ("password".equals(p.getProviderId())) { hasPasswordProvider = true; break; }
                }
            }
            btnChangePassword.setEnabled(hasPasswordProvider);

            btnChangePassword.setOnClickListener(v ->
                    startActivity(new Intent(this, ChangePassActivity.class))
            );
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}