package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.FragmentConfig;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

public class PageListRecyclerViewAdapter extends
        RecyclerView.Adapter<PageListRecyclerViewAdapter.TaskListRecyclerViewHolder> {

    private final Context context;
    private ArrayList<Page> mData = new ArrayList<>();
    private Utils.OnItemClicked mOnClick;
    private String[] search;

    public PageListRecyclerViewAdapter(Context context) {
        this.context = context;
    }

    private static Spannable getSpannable(Object o, String text, String[] search) {
        Spannable WordtoSpan = new SpannableString(text);
        WordtoSpan.setSpan(o, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (search != null) {
            for (String s : search) {
                String x = Utils.findText(text, s.trim());
                while (x.length() != text.length()) {
                    int i = x.indexOf(Utils.BEFORE_HIGHLIGHT);
                    x = x.replace(Utils.BEFORE_HIGHLIGHT, "");
                    int j = x.indexOf(Utils.AFTER_HIGHLIGHT);
                    x = x.replace(Utils.AFTER_HIGHLIGHT, "");
                    WordtoSpan.setSpan(new BackgroundColorSpan(Color.YELLOW),
                            i, j, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    WordtoSpan.setSpan(new UnderlineSpan(),
                            i, j, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return WordtoSpan;
    }

    public void notifyAboutUpdates(String url) {
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).url.equals(url)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update(DBHelper mydb, FragmentConfig.HiddenTexts hidden, Iterator<Page.PageTyp> typ,
                       String authorFilter, String tagFilter) {
        mData = mydb.getAllPages(hidden, typ, authorFilter, tagFilter);
        this.search = null;
        this.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update(ArrayList<Page> mData, String[] search) {
        this.mData = mData;
        this.search = search;
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
        Object o = new ForegroundColorSpan(page.getCacheFileName(holder.itemView.getContext()).exists() ?
                (((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES) ? Color.WHITE : Color.BLUE) : Color.GRAY);

        holder.titleText.setText(getSpannable(o, page.name, search));
        holder.titleAuthor.setText(getSpannable(o, page.author, search));
        holder.desc.setText(getSpannable(o, page.tags, search));

        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy hh:mm");
        String time = " | " + df.format(page.dt);
        Spannable WordtoSpan = new SpannableString(time);
        WordtoSpan.setSpan(o, 0, time.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.when.setText(WordtoSpan);
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
