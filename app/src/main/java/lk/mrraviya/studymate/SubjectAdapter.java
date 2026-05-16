package lk.mrraviya.studymate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

public class SubjectAdapter extends ArrayAdapter<SubjectAdapter.SubjectItem> {

    public static class SubjectItem {
        String name;
        int colorResId;

        public SubjectItem(String name, int colorResId) {
            this.name = name;
            this.colorResId = colorResId;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    public SubjectAdapter(Context context, List<SubjectItem> items) {
        super(context, 0, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_subject_dropdown, parent, false);
        }

        SubjectItem item = getItem(position);
        if (item != null) {
            ImageView colorView = convertView.findViewById(R.id.iv_subject_color);
            TextView nameView = convertView.findViewById(R.id.tv_subject_name);

            nameView.setText(item.name);
            colorView.setColorFilter(ContextCompat.getColor(getContext(), item.colorResId));
        }

        return convertView;
    }
}