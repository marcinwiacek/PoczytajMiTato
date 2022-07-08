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

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStateAdapter {
    ArrayList<FragmentConfig> configs = new ArrayList<>();
    Context context;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, Context context) {
        super(fragmentActivity);
        this.context = context;
    }

    private void readConfigs() {
        FragmentConfig c = FragmentConfig.readFromInternalStorage(context, 0);
        if (c == null) {
            c = new FragmentConfig(0, "FANTASTYKA");
            c.readInfoForReadFragment.add(Page.PagesTyp.FANTASTYKA_BIBLIOTEKA);
            configs.add(c);
            c.saveToInternalStorage(context);
            c = new FragmentConfig(1, "OPOWI");
            c.readInfoForReadFragment.add(Page.PagesTyp.OPOWI_FANTASTYKA);
            configs.add(c);
            c.saveToInternalStorage(context);
            c = new FragmentConfig(2, "SZUKAJ");
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.BOOKRAGE);
            //  c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.BOOKTO);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.IBUK);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA);
            c.storeInfoForSearchFragment.add(StoreInfo.StoreInfoTyp.WOLNE_LEKTURY);
            configs.add(c);
            c.saveToInternalStorage(context);
        } else {
            int i = 0;
            while (true) {
                c = FragmentConfig.readFromInternalStorage(context, i);
                if (c == null) break;
                configs.add(c);
                i++;
            }
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
        return new ReadFragment(configs.get(position));
    }

    @Override
    public int getItemCount() {
        if (configs.size() == 0) readConfigs();
        return configs.size();
    }
}
