package com.mwiacek.poczytaj.mi.tato;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ViewPagerAdapter extends FragmentStateAdapter {
    ArrayList<FragmentConfig> configs = new ArrayList<>();
    ArrayList<Integer> nums = new ArrayList<>();
    Context context;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, Context context) {
        super(fragmentActivity);
        this.context = context;
    }

    public void addTab(FragmentConfig config, String newName) throws CloneNotSupportedException {
        FragmentConfig config2 = (FragmentConfig) config.clone();
        config2.tabName = newName;
        config2.tabNum = nums.get(nums.size() - 1) + 1;
        configs.add(config2);
        nums.add(config2.tabNum);
        config2.saveToInternalStorage(context);
        this.notifyItemInserted(configs.size() - 1);
    }

    public void deleteTab(FragmentConfig config) {
        int index = -1;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).tabNum == config.tabNum) {
                index = i;
                break;
            }
        }
        configs.remove(index);
        int index2 = -1;
        for (int i = 0; i < nums.size(); i++) {
            if (nums.get(i) == config.tabNum) {
                index2 = i;
                break;
            }
        }
        nums.remove(index2);
        File f = new File(context.getFilesDir() + File.separator + "tab" + config.tabNum);
        f.delete();
        this.notifyItemRemoved(index);
    }

    private void readConfigs() {
        for (File file : context.getFilesDir().listFiles()) {
            if (file.getName().startsWith("tab")) {
                nums.add(Integer.valueOf(file.getName().substring(3)));
            }
        }
        Collections.sort(nums);

        if (nums.isEmpty()) {
            FragmentConfig c = new FragmentConfig(0, "FANTASTYKA");
            c.readInfoForReadFragment.add(Page.PagesTyp.FANTASTYKA_BIBLIOTEKA);
            configs.add(c);
            nums.add(0);
            c.saveToInternalStorage(context);
            c = new FragmentConfig(1, "OPOWI");
            c.readInfoForReadFragment.add(Page.PagesTyp.OPOWI_FANTASTYKA);
            configs.add(c);
            nums.add(1);
            c.saveToInternalStorage(context);
            c = new FragmentConfig(2, "SZUKAJ");
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.BOOKRAGE);
            //  c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.BOOKTO);
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
        if (position > configs.size() - 1) return "";
        return configs.get(position).tabName;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (configs.size() == 0) readConfigs();
        if (position > configs.size() - 1) return null;
        if (!configs.get(position).storeInfoForSearchFragment.isEmpty())
            return new SearchFragment(configs.get(position));
        return new ReadFragment(configs.get(position), this);
    }

    @Override
    public int getItemCount() {
        if (configs.size() == 0) readConfigs();
        return configs.size();
    }
}
