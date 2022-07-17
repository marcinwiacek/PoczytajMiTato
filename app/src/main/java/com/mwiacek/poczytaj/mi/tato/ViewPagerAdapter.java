package com.mwiacek.poczytaj.mi.tato;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.ImageCache;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final static ArrayList<FragmentConfig> configs = new ArrayList<>();
    private final Context context;
    private final ImageCache imageCache;
    private final DBHelper mydb;
    private final TabLayout tabLayout;
    private final AppCompatActivity activity;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, Context context,
                            ImageCache imageCache, DBHelper mydb, TabLayout tabLayout,
                            AppCompatActivity activity) {
        super(fragmentActivity);
        this.context = context;
        this.imageCache = imageCache;
        this.mydb = mydb;
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

    private void checkTabMode() {
        tabLayout.setVisibility(tabLayout.getTabCount() == 1 ? View.GONE : View.VISIBLE);
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
            if (f instanceof ReadFragment) {
                ((ReadFragment) f).updateLayout(!(tabLayout.getTabCount() == 1));
            }
        }
    }

    public void deleteSearchTab() {
        FragmentConfig c = null;
        for (FragmentConfig f : configs) {
            if (f.searchFragmentConfig) {
                c = f;
                break;
            }
        }
        if (c != null) {
            deleteTab(c);
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
        checkTabMode();
    }

    public void addTab(FragmentConfig config, String newName) throws CloneNotSupportedException {
        FragmentConfig config2 = (FragmentConfig) config.clone();
        config2.tabName = newName;
        config2.fileNameTabNum = configs.get(configs.size() - 1).fileNameTabNum + 1;
        configs.add(config2);
        config2.saveToInternalStorage(context);
        this.notifyItemInserted(configs.size() - 1);
        checkTabMode();
    }

    public void deleteTab(FragmentConfig config) {
        /* Remove tab from configs */
        int index = -1;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).fileNameTabNum == config.fileNameTabNum) {
                index = i;
                break;
            }
        }
        configs.remove(index);
        /* delete file */
        boolean delete = new File(context.getFilesDir() + File.separator +
                "tab" + config.fileNameTabNum).delete();
        if (!delete) System.out.println("file not deleted");
        this.notifyItemRemoved(index);
        checkTabMode();
    }

    private void readConfigs() {
        File[] f = context.getFilesDir().listFiles();
        int correct = 0;
        if (f != null) {
            for (File file : f) {
                if (file.getName().startsWith("tab")) {
                    FragmentConfig c = FragmentConfig.readFromInternalStorage(context,
                            Integer.parseInt(file.getName().substring(3)));
                    System.out.println("x" + Integer.parseInt(file.getName().substring(3)));
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
            FragmentConfig c = new FragmentConfig(0, "FANTASTYKA");
            c.readInfoForReadFragment.add(Page.PageTyp.FANTASTYKA_BIBLIOTEKA);
            c.searchFragmentConfig = false;
            configs.add(c);
            c.saveToInternalStorage(context);

            c = new FragmentConfig(1, "OPOWI");
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
        checkTabMode();
        return configs.get(position).tabName;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (configs.size() == 0) readConfigs();
        checkTabMode();
        return configs.get(position).searchFragmentConfig ?
                new SearchFragment(configs.get(position), imageCache, this) :
                new ReadFragment(configs.get(position), this, mydb);
    }

    @Override
    public int getItemCount() {
        if (configs.size() == 0) readConfigs();
        checkTabMode();
        return configs.size();
    }
}
