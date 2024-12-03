package edu.uga.cs.roommateshopping;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    EditText signupEmail, signupPassword;
    TextView loginRedirectText;
    Button signupButton;
    FirebaseAuth auth;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("users");

        signupButton.setOnClickListener(view -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();

            // Validate email format
            if (!isValidEmail(email)) {
                Toast.makeText(SignupActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate password strength
            if (password.length() < 6) {
                Toast.makeText(SignupActivity.this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a user in Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            String userId = firebaseUser != null ? firebaseUser.getUid() : "";

                            // Add user data to Realtime Database using email as the key
                            HelperClass helperClass = new HelperClass(email, password);
                            reference.child(userId).setValue(helperClass)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(SignupActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(SignupActivity.this, "Failed to add user data.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(SignupActivity.this, "Email already in use. Please log in.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        loginRedirectText.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        });
    }

    // Email validation
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}";
        return email.matches(emailPattern);
    }
}




