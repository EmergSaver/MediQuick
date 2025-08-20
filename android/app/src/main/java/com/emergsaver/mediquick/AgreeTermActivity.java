package com.emergsaver.mediquick;

import android.os.Bundle;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;



public class AgreeTermActivity extends AppCompatActivity {

    public interface Listner {
        void onRequiredTermsAgree();
    }

    private Listner listner;

    public static AgreeTermActivity newInstance() {
        return new AgreeTermActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_agree_term);
        

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_agree), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}