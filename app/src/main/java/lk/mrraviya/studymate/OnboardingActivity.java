package lk.mrraviya.studymate;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

        findViewById(R.id.get_started_button).setOnClickListener(v -> {
            // Save that onboarding is done
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_done", true)
                .apply();

            Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}