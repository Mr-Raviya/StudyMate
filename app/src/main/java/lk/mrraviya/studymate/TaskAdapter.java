package lk.mrraviya.studymate;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onDeleteTask(int position);
        void onEditTask(int position, Task task);
        void onTaskStatusChanged(int position, boolean isCompleted);
    }

    private List<Task> taskList;
    private OnTaskActionListener actionListener;

    public TaskAdapter(List<Task> taskList, OnTaskActionListener actionListener) {
        this.taskList = taskList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvSubject.setText(task.getSubject());
        holder.tvDate.setText(task.getDate());
        
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted());
        updateTaskStyle(holder, task, task.isCompleted());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            updateTaskStyle(holder, task, isChecked);
            if (actionListener != null) {
                actionListener.onTaskStatusChanged(holder.getAdapterPosition(), isChecked);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeleteTask(holder.getAdapterPosition());
            }
        });

        holder.ivEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                int pos = holder.getAdapterPosition();
                actionListener.onEditTask(pos, taskList.get(pos));
            }
        });
    }

    private void updateTaskStyle(TaskViewHolder holder, Task task, boolean isChecked) {
        int taskColor = ContextCompat.getColor(holder.itemView.getContext(), task.getCategoryColor());
        int grayColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_grey);
        int blackColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.black);
        
        if (isChecked) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(grayColor);
            holder.ivEdit.setImageTintList(ColorStateList.valueOf(grayColor));
            holder.ivDelete.setImageTintList(ColorStateList.valueOf(grayColor));
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(blackColor);
            holder.ivEdit.setImageTintList(ColorStateList.valueOf(blackColor));
            holder.ivDelete.setImageTintList(ColorStateList.valueOf(blackColor));
        }

        // Keep these functional and colored
        holder.ivEdit.setEnabled(true);
        holder.tvSubject.setBackgroundTintList(ColorStateList.valueOf(taskColor));
        holder.checkBox.setButtonTintList(ColorStateList.valueOf(taskColor));
        
        // Calendar icon always gray
        holder.ivCalendar.setImageTintList(ColorStateList.valueOf(grayColor));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void addTask(Task task) {
        taskList.add(0, task);
        notifyItemInserted(0);
    }

    public void deleteTask(int position) {
        taskList.remove(position);
        notifyItemRemoved(position);
    }

    public void updateTask(int position, Task task) {
        taskList.set(position, task);
        notifyItemChanged(position);
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvDate;
        CheckBox checkBox;
        android.widget.ImageView ivEdit, ivDelete, ivCalendar;
        View btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvSubject = itemView.findViewById(R.id.tv_task_subject);
            tvDate = itemView.findViewById(R.id.tv_task_date);
            checkBox = itemView.findViewById(R.id.cb_task);
            ivEdit = itemView.findViewById(R.id.iv_edit);
            ivDelete = itemView.findViewById(R.id.iv_delete);
            ivCalendar = itemView.findViewById(R.id.iv_calendar);
            btnDelete = ivDelete;
        }
    }
}
