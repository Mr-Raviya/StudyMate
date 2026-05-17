package lk.mrraviya.studymate;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The main screen of the app where tasks are displayed.
 */
public class MainActivity extends AppCompatActivity {

    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference tasksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force light mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Reference to user's specific tasks collection in Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tasksRef = db.collection("users").document(user.getUid()).collection("tasks");
        }

        // Set system bar colors
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true);
            windowInsetsController.setAppearanceLightNavigationBars(true);
        }

        // Setup UI components
        setupNavigationDrawer();
        setupRecyclerView();
        loadUserProfile();

        // Click listeners
        findViewById(R.id.fab_add).setOnClickListener(v -> showAddTaskDialog());

        findViewById(R.id.cv_profile).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, UserProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user profile when returning to this screen
        loadUserProfile();
    }

    /**
     * Load user name and profile picture from Firestore.
     */
    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;
        
        android.widget.ImageView ivToolbarProfile = findViewById(R.id.iv_toolbar_profile);
        androidx.cardview.widget.CardView cvProfile = findViewById(R.id.cv_profile);

        db.collection("users").document(mAuth.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && ivToolbarProfile != null) {
                        String imageString = documentSnapshot.getString("profileImageUrl");
                        if (imageString != null && !imageString.isEmpty()) {
                            try {
                                // Convert Base64 string back to image
                                byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                ivToolbarProfile.setImageBitmap(decodedByte);
                                ivToolbarProfile.setPadding(0, 0, 0, 0);
                                ivToolbarProfile.setImageTintList(null);
                                ivToolbarProfile.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                                if (cvProfile != null) {
                                    cvProfile.setCardBackgroundColor(Color.WHITE);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    /**
     * Setup the list that displays tasks.
     */
    private void setupRecyclerView() {
        RecyclerView rvTasks = findViewById(R.id.rv_tasks);
        taskList = new ArrayList<>();

        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onDeleteTask(int position) {
                showDeleteConfirmationDialog(position);
            }

            @Override
            public void onEditTask(int position, Task task) {
                showAddTaskDialog(true, position, task);
            }

            @Override
            public void onTaskStatusChanged(int position, boolean isCompleted) {
                updateTaskStatusInFirestore(taskList.get(position));
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
        
        listenToTasks();
    }

    /**
     * Listen for changes in the tasks collection in real-time.
     */
    private void listenToTasks() {
        if (tasksRef == null) return;

        tasksRef.orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null) {
                    // Add sample tasks if the list is empty for the first time
                    if (value.isEmpty()) {
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        boolean samplesAdded = prefs.getBoolean("samples_added_" + mAuth.getUid(), false);
                        
                        if (!samplesAdded) {
                            addSampleTasksToFirestore();
                            prefs.edit().putBoolean("samples_added_" + mAuth.getUid(), true).apply();
                        }
                    }

                    taskList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            task.setId(doc.getId());
                            taskList.add(task);
                        }
                    }
                    taskAdapter.notifyDataSetChanged();
                    updateProgress();
                }
            });
    }

    /**
     * Add initial dummy tasks to Firestore.
     */
    private void addSampleTasksToFirestore() {
        List<Task> samples = new ArrayList<>();
        samples.add(new Task("Complete DB Assignment", "ICT", "Mar 25, 2026", R.color.cat_ict, false));
        samples.add(new Task("Calculus Homework", "Math", "Mar 22, 2026", R.color.cat_math, true));
        samples.add(new Task("Research Paper Outline", "English", "Mar 26, 2026", R.color.cat_english, false));
        samples.add(new Task("Physics Lab Report", "Physics", "Mar 27, 2026", R.color.cat_physics, false));
        samples.add(new Task("Study for History Quiz", "History", "Mar 24, 2026", R.color.cat_history, true));

        for (Task task : samples) {
            tasksRef.add(task);
        }
    }

    /**
     * Update the completed status of a task in Firestore.
     */
    private void updateTaskStatusInFirestore(Task task) {
        if (task.getId() != null) {
            tasksRef.document(task.getId()).update("completed", task.isCompleted());
        }
    }

    /**
     * Calculate and update the progress bar.
     */
    private void updateProgress() {
        if (taskList == null) return;
        
        int totalTasks = taskList.size();
        int completedTasks = 0;
        for (Task task : taskList) {
            if (task.isCompleted()) {
                completedTasks++;
            }
        }

        TextView tvProgressCount = findViewById(R.id.tv_progress_count);
        com.google.android.material.progressindicator.LinearProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);

        if (tvProgressCount != null) {
            tvProgressCount.setText(completedTasks + "/" + totalTasks);
        }

        if (progressIndicator != null) {
            if (totalTasks > 0) {
                int progress = (int) (((float) completedTasks / totalTasks) * 100);
                progressIndicator.setProgress(progress, true);
            } else {
                progressIndicator.setProgress(0, true);
            }
        }
    }

    /**
     * Show a popup to confirm task deletion.
     */
    private void showDeleteConfirmationDialog(int position) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.RoundedConfirmationDialog)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    String taskId = taskList.get(position).getId();
                    if (taskId != null) {
                        tasksRef.document(taskId).delete();
                    }
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Setup the side navigation menu.
     */
    private void setupNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dev_info) {
                android.content.Intent intent = new android.content.Intent(this, DevInfoActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                android.content.Intent intent = new android.content.Intent(this, SettingsPlaceholderActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_exit) {
                showExitConfirmationDialog();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    /**
     * Show a popup when user clicks the exit button.
     */
    private void showExitConfirmationDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.RoundedConfirmationDialog)
                .setTitle("Exit")
                .setMessage("Do you really want to exit from StudyMate?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                .show();
    }

    private void showAddTaskDialog() {
        showAddTaskDialog(false, -1, null);
    }

    /**
     * Show a custom dialog to add or edit a task.
     */
    private void showAddTaskDialog(boolean isEdit, int position, Task taskToEdit) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        
        TextView tvTitleLabel = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitleLabel != null) {
            tvTitleLabel.setText(isEdit ? "Edit Task" : "Add New Task");
        }

        com.google.android.material.button.MaterialButton btnAction = dialogView.findViewById(R.id.btn_add_task);
        if (btnAction != null) {
            btnAction.setText(isEdit ? "Update Task" : "Add Task");
        }

        TextInputEditText etTitle = dialogView.findViewById(R.id.et_task_title);
        TextInputEditText etDueDate = dialogView.findViewById(R.id.et_due_date);
        com.google.android.material.textfield.MaterialAutoCompleteTextView subjectSpinner = dialogView.findViewById(R.id.spinner_subject);
        
        if (isEdit && taskToEdit != null) {
            etTitle.setText(taskToEdit.getTitle());
            etDueDate.setText(taskToEdit.getDate());
            subjectSpinner.setText(taskToEdit.getSubject(), false);
        }

        // List of subjects with their colors
        List<SubjectAdapter.SubjectItem> subjects = new ArrayList<>();
        subjects.add(new SubjectAdapter.SubjectItem("ICT", R.color.cat_ict));
        subjects.add(new SubjectAdapter.SubjectItem("Math", R.color.cat_math));
        subjects.add(new SubjectAdapter.SubjectItem("English", R.color.cat_english));
        subjects.add(new SubjectAdapter.SubjectItem("Physics", R.color.cat_physics));
        subjects.add(new SubjectAdapter.SubjectItem("History", R.color.cat_history));

        SubjectAdapter adapter = new SubjectAdapter(this, subjects);
        subjectSpinner.setAdapter(adapter);

        subjectSpinner.setOnItemClickListener((parent, view, pos, id) -> {
            SubjectAdapter.SubjectItem selected = subjects.get(pos);
            com.google.android.material.textfield.TextInputLayout layout = dialogView.findViewById(R.id.spinner_subject_layout);
            if (layout != null) {
                layout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(MainActivity.this, selected.colorResId)));
            }
        });
        
        if (isEdit && taskToEdit != null) {
            com.google.android.material.textfield.TextInputLayout layout = dialogView.findViewById(R.id.spinner_subject_layout);
            if (layout != null) {
                layout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(MainActivity.this, taskToEdit.getCategoryColor())));
            }
        }

        // Date picker popup
        etDueDate.setOnClickListener(v -> {
            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now())
                    .build();

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Due Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setCalendarConstraints(constraints)
                    .setTheme(R.style.CustomDatePickerTheme)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                etDueDate.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        // Dialog creation
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(view -> dialog.dismiss());
        
        btnAction.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String subject = subjectSpinner.getText().toString();
            String date = etDueDate.getText().toString();

            if (!title.isEmpty() && !subject.isEmpty()) {
                
                int colorResId = R.color.cat_ict;
                for (SubjectAdapter.SubjectItem s : subjects) {
                    if (s.name.equals(subject)) {
                        colorResId = s.colorResId;
                        break;
                    }
                }

                // Save or update task in Firestore
                if (isEdit) {
                    taskToEdit.setTitle(title);
                    taskToEdit.setSubject(subject);
                    taskToEdit.setDate(date);
                    taskToEdit.setCategoryColor(colorResId);
                    tasksRef.document(taskToEdit.getId()).set(taskToEdit);
                } else {
                    tasksRef.add(new Task(title, subject, date, colorResId, false));
                }
                dialog.dismiss();
            }
        });

        dialog.show();
        
        // Adjust dialog size
        if (dialog.getWindow() != null) {
            int width = (int) (320 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
