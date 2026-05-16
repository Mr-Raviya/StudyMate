package lk.mrraviya.studymate;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputEditText etEmail;
    private MaterialButton btnReset;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true);
            windowInsetsController.setAppearanceLightNavigationBars(true);
        }

        emailLayout = findViewById(R.id.email_layout);
        etEmail = findViewById(R.id.email_edit_text);
        btnReset = findViewById(R.id.btn_reset_password);
        progressBar = findViewById(R.id.reset_progress_bar);

        btnReset.setOnClickListener(v -> {
            if (validateInput()) {
                resetPassword();
            }
        });
    }

    private void resetPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        setLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnReset.setEnabled(false);
            btnReset.setText("");
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnReset.setEnabled(true);
            btnReset.setText("Send Reset Link");
            progressBar.setVisibility(View.GONE);
        }
    }

    private boolean validateInput() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        emailLayout.setError(null);

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            return false;
        }
        return true;
    }
}
