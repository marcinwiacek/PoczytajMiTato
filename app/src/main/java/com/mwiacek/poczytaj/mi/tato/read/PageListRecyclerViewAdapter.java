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

public class PageListRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static int REGULAR_VIEW_TYPE_ITEM = 1;

    private final Context context;
    private boolean allRead = false;
    private boolean showHidden = false;
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

    public void onPageUpdate(String url) {
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).url.equals(url)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update(DBHelper mydb, FragmentConfig.HiddenTexts hidden, ArrayList<Page.PageTyp> typ,
                       String authorFilter, String tagFilter) {
        mData = mydb.getAllPages(hidden, typ, authorFilter, tagFilter);
        this.search = null;
        this.allRead = true;
        for (Page.PageTyp t : typ) {
            if (mydb.getLastIndexPageRead(t)!=-1) {
                this.allRead = false;
                break;
            }
        }
        this.showHidden = hidden != FragmentConfig.HiddenTexts.NONE;
        this.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update(ArrayList<Page> mData, String[] search) {
        this.mData = mData;
        this.search = search;
        this.notifyDataSetChanged();
    }

    void setOnClick(Utils.OnItemClicked onClick) {
        this.mOnClick = onClick;
    }

    Page getItem(int position) {
        return mData.get(position);
    }

    ArrayList<Page> getAllItems() {
        return mData;
    }

    private String getStringForInfoElement() {
        if (search != null) return "Znalezione teksty: " + mData.size();
        if (showHidden) return "";
        if (mData.size() == 0) return "Użyj gestu swype, żeby załadować dane.";
        if (!allRead) return "Lista tekstów jest niepełna.";
        return "Teksty: " + mData.size(); //Ostatnie odświeżenie.
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == REGULAR_VIEW_TYPE_ITEM) {
            return new TaskListRecyclerViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.read_list_item,
                            parent, false));
        }
        return new InfoTaskListRecyclerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.info_list_item,
                        parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof InfoTaskListRecyclerViewHolder) {
            ((InfoTaskListRecyclerViewHolder) viewHolder).description.setText(getStringForInfoElement());
            return;
        }

        TaskListRecyclerViewHolder holder = (TaskListRecyclerViewHolder) viewHolder;

        Page page = mData.get(position);
        Object o = new ForegroundColorSpan(page.getCacheFile(holder.itemView.getContext()).exists() ?
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
    public int getItemViewType(int position) {
        return position == mData.size() ? (REGULAR_VIEW_TYPE_ITEM + 1) : REGULAR_VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mData.size() + (getStringForInfoElement().isEmpty() ? 0 : 1);
    }

    static class InfoTaskListRecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView description;

        InfoTaskListRecyclerViewHolder(View view) {
            super(view);

            description = view.findViewById(R.id.Description);
        }
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
