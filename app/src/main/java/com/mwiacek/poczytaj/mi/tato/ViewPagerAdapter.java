package com.mwiacek.poczytaj.mi.tato;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public final static ArrayList<FragmentConfig> configs = new ArrayList<>();
    public final ViewPagerAdapter topPageAdapter = this;
    private final Context context;
    //  private final ImageCache imageCache;
    //  private final DBHelper mydb;
    private final TabLayout tabLayout;
    private final AppCompatActivity activity;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, Context context,
                            //ImageCache imageCache, DBHelper mydb,
                            TabLayout tabLayout,
                            AppCompatActivity activity) {
        super(fragmentActivity);
        this.context = context;
        //    this.imageCache = imageCache;
        //  this.mydb = mydb;
        this.tabLayout = tabLayout;
        this.activity = activity;
    }

    public static boolean isSearchTabAvailable() {
        int i = 0;
        for (FragmentConfig f : configs) {
            if (f.searchFragmentConfig) i++;
        }
        return i > 0;
    }

    public static boolean areMultipleReadTabsAvailable() {
        int i = 0;
        for (FragmentConfig f : configs) {
            if (!f.searchFragmentConfig) i++;
        }
        return i > 1;
    }

    private void updateTabMode() {
        tabLayout.setVisibility(tabLayout.getTabCount() == 1 ? View.GONE : View.VISIBLE);
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
            if (f instanceof ReadFragment) {
                ((ReadFragment) f).onUpdateLayout(!(tabLayout.getTabCount() == 1));
            }
        }
    }

    public void deleteSearchTab() {
        for (FragmentConfig c : configs) {
            if (c.searchFragmentConfig) {
                deleteTab(c);
                /* We need to refresh menu items */
                for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
                    if (f instanceof ReadFragment) {
                        ((ReadFragment) f).onInvalidateMenu();
                    }
                }
                return;
            }
        }
    }

    public void addSearchTab() {
        FragmentConfig c = new FragmentConfig(2, "SZUKAJ");
        c.searchFragmentConfig = true;
        c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.BOOKRAGE);
        c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW);
        c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.IBUK);
        c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA);
        c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.WOLNE_LEKTURY);
        c.fileNameTabNum = configs.get(configs.size() - 1).fileNameTabNum + 1;
        configs.add(c);
        c.saveToInternalStorage(context);
        this.notifyItemInserted(configs.size() - 1);
        updateTabMode();
        /* We need to refresh menu items */
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
            if (f instanceof ReadFragment) {
                ((ReadFragment) f).onInvalidateMenu();
            }
        }
    }

    public void addTab(FragmentConfig config, String newName) throws CloneNotSupportedException {
        FragmentConfig config2 = (FragmentConfig) config.clone();
        config2.tabName = newName;
        config2.fileNameTabNum = configs.get(configs.size() - 1).fileNameTabNum + 1;
        configs.add(config2);
        config2.saveToInternalStorage(context);
        this.notifyItemInserted(configs.size() - 1);
        updateTabMode();
    }

    public void deleteTab(FragmentConfig config) {
        int i = 0;
        ListIterator<FragmentConfig> listIterator = configs.listIterator();
        while (listIterator.hasNext()) {
            if (listIterator.next().fileNameTabNum == config.fileNameTabNum) {
                listIterator.remove();
                /* delete file */
                new File(context.getFilesDir() + File.separator +
                        "tab" + config.fileNameTabNum).delete();
                this.notifyItemRemoved(i);
                updateTabMode();
                return;
            }
            i++;
        }
    }

    private void readConfigs() {
        File[] f = context.getFilesDir().listFiles();
        int correct = 0;
        if (f != null) {
            for (File file : f) {
                if (file.getName().startsWith("tab")) {
                    FragmentConfig c = FragmentConfig.readFromInternalStorage(context,
                            Integer.parseInt(file.getName().substring(3)));
                    if (c == null) {
                        file.delete();
                        correct = -1;
                    } else if (correct != -1) {
                        correct++;
                        configs.add(c);
                    }
                }
            }
            Comparator<FragmentConfig> comparator =
                    (lhs, rhs) -> Integer.compare(lhs.fileNameTabNum, rhs.fileNameTabNum);
            Collections.sort(configs, comparator);
        }
        if (f == null || correct < 1) {
            FragmentConfig c = new FragmentConfig(0, "BIBLIOTEKA");
            c.readInfoForReadFragment.add(Page.PageTyp.FANTASTYKA_BIBLIOTEKA);
            c.searchFragmentConfig = false;
            configs.add(c);
            c.saveToInternalStorage(context);

            c = new FragmentConfig(1, "POCZEKALNIA");
            c.readInfoForReadFragment.add(Page.PageTyp.FANTASTYKA_POCZEKALNIA);
            c.searchFragmentConfig = false;
            configs.add(c);
            c.saveToInternalStorage(context);

            c = new FragmentConfig(2, "OPOWI");
            c.readInfoForReadFragment.add(Page.PageTyp.OPOWI_FANTASTYKA);
            c.searchFragmentConfig = false;
            configs.add(c);
            c.saveToInternalStorage(context);

            addSearchTab();
        }
    }

    public int getTabNum(int position) {
        return configs.get(position).fileNameTabNum;
    }

    public CharSequence getPageTitle(int position) {
        if (configs.size() == 0) readConfigs();
        updateTabMode();
        return configs.get(position).tabName;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (configs.size() == 0) readConfigs();
        updateTabMode();
        Bundle args = new Bundle();
        args.putInt("configNum", position);
        Fragment f = configs.get(position).searchFragmentConfig ? new SearchFragment() : new ReadFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public int getItemCount() {
        if (configs.size() == 0) readConfigs();
        updateTabMode();
        return configs.size();
    }
}
