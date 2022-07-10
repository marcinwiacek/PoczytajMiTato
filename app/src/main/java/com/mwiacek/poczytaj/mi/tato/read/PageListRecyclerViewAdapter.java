package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
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
import com.mwiacek.poczytaj.mi.tato.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class PageListRecyclerViewAdapter extends
        RecyclerView.Adapter<PageListRecyclerViewAdapter.TaskListRecyclerViewHolder> {

    private ArrayList<Page> mData = new ArrayList<>();
    private Utils.OnItemClicked mOnClick;

    public void update(DBHelper mydb, boolean hidden, Page.PageTyp[] typ) {
        mData = mydb.getAllPages(hidden, typ);
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskListRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskListRecyclerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.read_page_list_item, parent, false));
    }

    Page getItem(int position) {
        return mData.get(position);
    }

    ArrayList<Page> getAllItems() {
        return mData;
    }

    @Override
    public void onBindViewHolder(@NonNull TaskListRecyclerViewHolder holder, int position) {
        Page page = mData.get(position);

        Object o = page.getCacheFileName(holder.itemView.getContext()).exists() ?
                new ForegroundColorSpan(Color.BLUE) :
                new ForegroundColorSpan(Color.GRAY);

        Spannable WordtoSpan = new SpannableString(page.name);
        WordtoSpan.setSpan(o, 0, page.name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.titleText.setText(WordtoSpan);

        WordtoSpan = new SpannableString(page.author);
        WordtoSpan.setSpan(o, 0, page.author.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.titleAuthor.setText(WordtoSpan);

        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy hh:mm");
        String time = " | " + df.format(page.dt);
        WordtoSpan = new SpannableString(time);
        WordtoSpan.setSpan(o, 0, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.when.setText(WordtoSpan);

        WordtoSpan = new SpannableString(page.tags);
        WordtoSpan.setSpan(o, 0, page.tags.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.desc.setText(WordtoSpan);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    void setOnClick(Utils.OnItemClicked onClick) {
        this.mOnClick = onClick;
    }

    class TaskListRecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView titleText;
        public TextView titleAuthor;
        public TextView when;
        public TextView desc;

        TaskListRecyclerViewHolder(View view) {
            super(view);

            titleText = view.findViewById(R.id.taskTitle);
            titleAuthor = view.findViewById(R.id.taskAuthor);
            when = view.findViewById(R.id.taskDate);
            desc = view.findViewById(R.id.taskDesc);

            view.setOnClickListener(v -> mOnClick.onItemClick(getAbsoluteAdapterPosition()));
        }
    }
}
