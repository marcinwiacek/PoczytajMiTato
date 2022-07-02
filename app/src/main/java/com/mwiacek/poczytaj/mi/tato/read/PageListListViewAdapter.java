package com.mwiacek.poczytaj.mi.tato.read;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.R;

import java.util.ArrayList;

public class PageListListViewAdapter extends
        RecyclerView.Adapter<PageListListViewAdapter.TaskListRecyclerViewHolder> {

    private ArrayList<Page> mData = new ArrayList<>();
    private OnItemClicked mOnClick; // Callback used after clicking on entry

    public void update(DBHelper mydb) {
        mData = mydb.getAllPages();
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskListRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskListRecyclerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.read_page_list_item, parent, false));
    }

    Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskListRecyclerViewHolder holder, int position) {
        Page b = ((Page) getItem(position));

        Object o = b.getCacheFileName(holder.itemView.getContext()).exists() ?
                new ForegroundColorSpan(Color.BLUE) :
                new ForegroundColorSpan(Color.GRAY);

        Spannable WordtoSpan = new SpannableString(b.name);
        WordtoSpan.setSpan(o, 0, b.name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.titleText.setText(WordtoSpan);

        WordtoSpan = new SpannableString(b.author);
        WordtoSpan.setSpan(o, 0, b.author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.titleAuthor.setText(WordtoSpan);

        WordtoSpan = new SpannableString(b.dt.toString());
        WordtoSpan.setSpan(o, 0, b.dt.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.when.setText(WordtoSpan);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * Setting callback for handling click on single task list entry
     */
    void setOnClick(OnItemClicked onClick) {
        this.mOnClick = onClick;
    }

    public interface OnItemClicked {
        void onItemClick(int position);
    }

    class TaskListRecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView titleText;
        public TextView titleAuthor;
        public TextView when;

        TaskListRecyclerViewHolder(View view) {
            super(view);

            titleText = view.findViewById(R.id.taskTitle);
            titleAuthor = view.findViewById(R.id.taskAuthor);
            when = view.findViewById(R.id.taskDate);

            view.setOnClickListener(v -> mOnClick.onItemClick(getAbsoluteAdapterPosition()));
        }
    }
}
