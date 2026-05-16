package lk.mrraviya.studymate;

import com.google.firebase.firestore.Exclude;

public class Task {
    private String id;
    private String title;
    private String subject;
    private String date;
    private int categoryColor;
    private boolean isCompleted;

    public Task() {
        // Required for Firestore
    }

    public Task(String title, String subject, String date, int categoryColor, boolean isCompleted) {
        this.title = title;
        this.subject = subject;
        this.date = date;
        this.categoryColor = categoryColor;
        this.isCompleted = isCompleted;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int getCategoryColor() { return categoryColor; }
    public void setCategoryColor(int categoryColor) { this.categoryColor = categoryColor; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
