package lk.mrraviya.studymate;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private MaterialCardView cvProfileImage;
    private TextView tvFullNameTop, tvFullNameList, tvEmail, tvDateJoined;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // Set status bar color to white and icons to dark
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat windowInsetsController = 
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true);
            windowInsetsController.setAppearanceLightNavigationBars(true);
        }

        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfileImage = findViewById(R.id.iv_profile_image);
        cvProfileImage = (MaterialCardView) ivProfileImage.getParent();
        tvFullNameTop = findViewById(R.id.tv_full_name_top);
        tvFullNameList = findViewById(R.id.tv_full_name_list);
        tvEmail = findViewById(R.id.tv_email);
        tvDateJoined = findViewById(R.id.tv_date_joined);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        toolbar.setNavigationOnClickListener(v -> finish());

        loadUserData();

        // Setup Edit Profile button
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });

        // Setup Sign Out button
        findViewById(R.id.btn_sign_out).setOnClickListener(v -> showSignOutConfirmationDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users").document(mAuth.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fName = documentSnapshot.getString("firstName");
                        String lName = documentSnapshot.getString("lastName");
                        String email = documentSnapshot.getString("email");
                        String dateJoined = documentSnapshot.getString("dateJoined");
                        String imageString = documentSnapshot.getString("profileImageUrl");

                        String fullName = (fName != null ? fName : "") + " " + (lName != null ? lName : "");
                        tvFullNameTop.setText(fullName);
                        tvFullNameList.setText(fullName);
                        tvEmail.setText(email);
                        
                        if (dateJoined != null) {
                            tvDateJoined.setText(dateJoined);
                        } else {
                            tvDateJoined.setText("Not available");
                        }

                        if (imageString != null && !imageString.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                
                                ivProfileImage.setImageBitmap(decodedByte);
                                ivProfileImage.setPadding(0, 0, 0, 0);
                                ivProfileImage.setImageTintList(null);
                                ivProfileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                
                                if (cvProfileImage != null) {
                                    cvProfileImage.setCardBackgroundColor(Color.WHITE);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private void showSignOutConfirmationDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.RoundedConfirmationDialog)
                .setTitle("Sign Out")
                .setMessage("Do you really want to sign out from StudyMate?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}
