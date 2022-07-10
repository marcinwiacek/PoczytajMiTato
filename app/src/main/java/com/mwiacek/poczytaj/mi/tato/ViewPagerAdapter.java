package com.mwiacek.poczytaj.mi.tato;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.ImageCache;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final ArrayList<FragmentConfig> configs = new ArrayList<>();
    private final ArrayList<Integer> nums = new ArrayList<>();
    private final Context context;
    private final ImageCache imageCache;
    private final DBHelper mydb;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, Context context,
                            ImageCache imageCache, DBHelper mydb) {
        super(fragmentActivity);
        this.context = context;
        this.imageCache = imageCache;
        this.mydb = mydb;
    }

    public void addTab(FragmentConfig config, String newName) throws CloneNotSupportedException {
        FragmentConfig config2 = (FragmentConfig) config.clone();
        config2.tabName = newName;
        config2.tabNumForFileForSerialization = nums.get(nums.size() - 1) + 1;
        configs.add(config2);
        nums.add(config2.tabNumForFileForSerialization);
        config2.saveToInternalStorage(context);
        this.notifyItemInserted(configs.size() - 1);
    }

    public void deleteTab(FragmentConfig config) {
        /* Remove tab from configs */
        int index = -1;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).tabNumForFileForSerialization == config.tabNumForFileForSerialization) {
                index = i;
                break;
            }
        }
        configs.remove(index);
        /* Remove tab from nums. Numbers are sorted and we need second loop */
        int index2 = -1;
        for (int i = 0; i < nums.size(); i++) {
            if (nums.get(i) == config.tabNumForFileForSerialization) {
                index2 = i;
                break;
            }
        }
        nums.remove(index2);
        /* delete file */
        new File(context.getFilesDir() + File.separator +
                "tab" + config.tabNumForFileForSerialization).delete();
        this.notifyItemRemoved(index);
    }

    private void readConfigs() {
        File[] f = context.getFilesDir().listFiles();
        if (f != null) {
            for (File file : f) {
                if (file.getName().startsWith("tab")) {
                    nums.add(Integer.valueOf(file.getName().substring(3)));
                }
            }
            Collections.sort(nums);
        }
        if (f == null || nums.isEmpty()) {
            FragmentConfig c = new FragmentConfig(0, "FANTASTYKA");
            c.readInfoForReadFragment.add(Page.PageTyp.FANTASTYKA_BIBLIOTEKA);
            configs.add(c);
            nums.add(0);
            c.saveToInternalStorage(context);
            c = new FragmentConfig(1, "OPOWI");
            c.readInfoForReadFragment.add(Page.PageTyp.OPOWI_FANTASTYKA);
            configs.add(c);
            nums.add(1);
            c.saveToInternalStorage(context);
            c = new FragmentConfig(2, "SZUKAJ");
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.BOOKRAGE);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.IBUK);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.WOLNE_LEKTURY);
            configs.add(c);
            nums.add(2);
            c.saveToInternalStorage(context);
            return;
        }
        for (int i : nums) {
            FragmentConfig c = FragmentConfig.readFromInternalStorage(context, i);
            configs.add(c);
        }
    }

    public CharSequence getPageTitle(int position) {
        if (configs.size() == 0) readConfigs();
        //if (position > configs.size() - 1) return "";
        return configs.get(position).tabName;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (configs.size() == 0) readConfigs();
        //if (position > configs.size() - 1) return null;
        return configs.get(position).storeInfoForSearchFragment.isEmpty() ?
                new ReadFragment(configs.get(position), this, mydb) :
                new SearchFragment(configs.get(position), imageCache);
    }

    @Override
    public int getItemCount() {
        if (configs.size() == 0) readConfigs();
        return configs.size();
    }
}
