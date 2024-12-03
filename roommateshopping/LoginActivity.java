package edu.uga.cs.roommateshopping;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    Button loginButton;
    TextView signupRedirectText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        // Login button visibility
        loginButton.setEnabled(true);

        // Login button functionality
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("LoginActivity", "Login button clicked");
                // Validate email and password before proceeding
                if (!validateEmail() || !validatePassword()) {
                    Log.d("LoginActivity", "Validation failed");
                    return;
                }
                // Check user info with Firebase authentication
                checkUser();
            }
        });

        // Signup button
        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("LoginActivity", "Redirecting to Signup activity");
                // Redirect to Signup Activity
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    // Validate email field
    public Boolean validateEmail() {
        String val = loginEmail.getText().toString();
        if (val.isEmpty()) {
            loginEmail.setError("Email cannot be empty");
            return false;
        } else {
            loginEmail.setError(null);
            return true;
        }
    }

    // Validate password field
    public Boolean validatePassword() {
        String val = loginPassword.getText().toString();
        if (val.isEmpty()) {
            loginPassword.setError("Password cannot be empty");
            return false;
        } else {
            loginPassword.setError(null);
            return true;
        }
    }

    // Check user info with Firebase Authentication
    public void checkUser() {
        String userEmail = loginEmail.getText().toString().trim();
        String userPassword = loginPassword.getText().toString().trim();

        // Sign in with Firebase Authentication
        mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign-in success -> retrieve current user
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("LoginActivity", "Authentication successful, user: " + user.getEmail());

                        // Start NavMainActivity
                        Intent intent = new Intent(LoginActivity.this, NavMainActivity.class);
                        intent.putExtra("userID", user.getUid());
                        intent.putExtra("email", user.getEmail());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the stack
                        startActivity(intent);
                        finish();
                    } else {
                        // Authentication failure
                        Log.d("LoginActivity", "Authentication failed: " + task.getException().getMessage());
                        loginPassword.setError("Invalid Credentials");
                        loginPassword.requestFocus();
                    }
                });
    }

}

