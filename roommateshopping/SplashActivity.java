package edu.uga.cs.roommateshopping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private Button loginButton;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("userEmail", null);

        if (userEmail != null) {
            startActivity(new Intent(SplashActivity.this, NavMainActivity.class));
            finish();
        } else {
            loginButton = findViewById(R.id.login_button);
            signupButton = findViewById(R.id.signup_button);

            loginButton.setOnClickListener(v -> {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            });

            signupButton.setOnClickListener(v -> {
                startActivity(new Intent(SplashActivity.this, SignupActivity.class));
                finish();
            });
        }
    }
}


