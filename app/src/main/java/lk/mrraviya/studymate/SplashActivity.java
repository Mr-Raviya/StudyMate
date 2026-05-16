package lk.mrraviya.studymate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean onboardingDone = prefs.getBoolean("onboarding_done", false);

            Intent intent;
            if (mAuth.getCurrentUser() != null) {
                // User is already logged in, go to Main
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else if (onboardingDone) {
                // User has seen onboarding, go to Login
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            } else {
                // First time user, go to Onboarding
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }
            
            startActivity(intent);
            finish();
        }, 2000);
    }
}
