package lk.mrraviya.studymate;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Screen to edit user profile details and profile picture.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextInputEditText etFirstName, etLastName, etEmail;
    private String base64Image = null;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // System bar styling
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat windowInsetsController = 
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true);
            windowInsetsController.setAppearanceLightNavigationBars(true);
        }

        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        ivProfileImage = findViewById(R.id.iv_edit_profile_image);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.save_progress_bar);

        loadUserData();

        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Image picker launcher
        ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivProfileImage.setImageURI(uri);
                        ivProfileImage.setPadding(0, 0, 0, 0);
                        ivProfileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivProfileImage.setImageTintList(null);
                        
                        // Convert chosen image to Base64
                        base64Image = uriToBase64(uri);
                    }
                }
        );

        // Change Picture click
        findViewById(R.id.layout_change_picture).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Save button click
        btnSave.setOnClickListener(v -> saveUserData());
    }

    /**
     * Convert an image Uri to a Base64 string for efficient Firestore storage.
     */
    private String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            // Resize bitmap to keep it under Firestore limits
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load current user data to the input fields.
     */
    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users").document(mAuth.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etFirstName.setText(documentSnapshot.getString("firstName"));
                        etLastName.setText(documentSnapshot.getString("lastName"));
                        etEmail.setText(documentSnapshot.getString("email"));
                        String imageString = documentSnapshot.getString("profileImageUrl");
                        
                        if (imageString != null && !imageString.isEmpty()) {
                            // Decode Base64 string to bitmap
                            byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivProfileImage.setImageBitmap(decodedByte);
                            ivProfileImage.setPadding(0, 0, 0, 0);
                            ivProfileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            ivProfileImage.setImageTintList(null);
                        }
                    }
                });
    }

    /**
     * Validate and save user data to Firestore.
     */
    private void saveUserData() {
        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        updateFirestore(firstName, lastName, email, base64Image);
    }

    /**
     * Show/Hide loading progress on the button.
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnSave.setEnabled(false);
            btnSave.setText("");
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnSave.setEnabled(true);
            btnSave.setText("Save Info");
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Update user details in the Firestore document.
     */
    private void updateFirestore(String fName, String lName, String email, String imageUrl) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", fName);
        userMap.put("lastName", lName);
        userMap.put("email", email);
        if (imageUrl != null) {
            userMap.put("profileImageUrl", imageUrl);
        }

        db.collection("users").document(mAuth.getUid())
                .update(userMap)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(EditProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
