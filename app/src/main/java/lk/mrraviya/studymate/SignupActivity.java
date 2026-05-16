package lk.mrraviya.studymate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout firstNameLayout, lastNameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private MaterialButton signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true);
            windowInsetsController.setAppearanceLightNavigationBars(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firstNameLayout = findViewById(R.id.first_name_layout);
        lastNameLayout = findViewById(R.id.last_name_layout);
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);

        etFirstName = findViewById(R.id.first_name_edit_text);
        etLastName = findViewById(R.id.last_name_edit_text);
        etEmail = findViewById(R.id.email_edit_text);
        etPassword = findViewById(R.id.password_edit_text);
        etConfirmPassword = findViewById(R.id.confirm_password_edit_text);
        
        signupButton = findViewById(R.id.create_account_button);
        progressBar = findViewById(R.id.signup_progress_bar);

        signupButton.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });

        findViewById(R.id.sign_in_text).setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user.getUid(), firstName, lastName, email);
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(SignupActivity.this, "Authentication failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String userId, String firstName, String lastName, String email) {
        // Automatically capture the current date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", email);
        userMap.put("dateJoined", currentDate); // Save the date joined

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(SignupActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(SignupActivity.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            signupButton.setEnabled(false);
            signupButton.setText("");
            progressBar.setVisibility(View.VISIBLE);
        } else {
            signupButton.setEnabled(true);
            signupButton.setText(getString(R.string.create_account));
            progressBar.setVisibility(View.GONE);
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        firstNameLayout.setError(null);
        lastNameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        if (firstName.isEmpty()) {
            firstNameLayout.setError("First name is required");
            isValid = false;
        }

        if (lastName.isEmpty()) {
            lastNameLayout.setError("Last name is required");
            isValid = false;
        }

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }
}
