package com.mwiacek.poczytaj.mi.tato;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static ViewPagerAdapter viewPagerAdapter;
    private static MainActivity mContext;
    private ViewPager2 viewPager;

    public static Context getContext() {
        return mContext.getApplicationContext();
    }

    public static ViewPagerAdapter getViewPagerAdapter() {
        return viewPagerAdapter;
    }

    @Override
    public void onBackPressed() {
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof ReadFragment &&
                    viewPagerAdapter.getTabNum(viewPager.getCurrentItem())
                            == ((ReadFragment) f).getTabNum()) {
                ((ReadFragment) f).onBackPressed();
                return;
            } else if (f instanceof SearchFragment &&
                    viewPagerAdapter.getTabNum(viewPager.getCurrentItem())
                            == ((SearchFragment) f).getTabNum()) {
                ((SearchFragment) f).onBackPressed();
                return;
            }
        }
        super.onBackPressed();
    }

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      /*  StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());
*/

        MainActivity.mContext = this;

        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this,
                    com.google.android.material.R.color.design_dark_default_color_background
            ));
        }

        setContentView(R.layout.activity_main);

        MainActivity.viewPagerAdapter = new ViewPagerAdapter(this,
                findViewById(R.id.tab_layout), this);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(findViewById(R.id.tab_layout), viewPager,
                (tab, position) -> tab.setText(viewPagerAdapter.getPageTitle(position))
        ).attach();
        viewPager.setUserInputEnabled(false);

        Notifications.setupNotifications(getApplicationContext());

        //https://stackoverflow.com/questions/61772241/how-to-make-viewpager2-less-sensitive-to-swipe
        //https://issuetracker.google.com/issues/123006042
      /*  try {
            final Field recyclerViewField = ViewPager2.class.getDeclaredField("pagesRecyclerView");
            recyclerViewField.setAccessible(true);
            final RecyclerView recyclerView = (RecyclerView) recyclerViewField.get(viewPager);
            final Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
            touchSlopField.setAccessible(true);
            final int touchSlop = (int) touchSlopField.get(recyclerView);
            touchSlopField.set(recyclerView, touchSlop * 10);
        } catch (Exception ignore) {
        }*/

        new File(Utils.getDiskCacheFolder(getApplicationContext()), Page.CACHE_SUB_DIRECTORY).mkdirs();

        if (getIntent().getExtras() != null && getIntent().getIntExtra("tabNum", -1) != -1) {
            viewPager.setCurrentItem(getIntent().getIntExtra("tabNum", -1), true);
        }
    }
}
