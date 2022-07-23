package com.mwiacek.poczytaj.mi.tato;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.ImageCache;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;

import java.io.File;
import java.lang.reflect.Field;

/*
TODO czerwone teksty (niewidoczne na serwerze)
TODO up down przy szukaniu - FloatingActionButton
TODO TOR
TODO Google Books ?
TODO sortowanie szukania ?
TODO sync z szukaniem systemowym ?
 */
public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                (getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this,
                    com.google.android.material.R.color.design_dark_default_color_background
            ));
        }

        setContentView(R.layout.activity_main);

        ImageCache mImageCache = new ImageCache(getApplicationContext());
        DBHelper mydb = new DBHelper(getApplicationContext());

        viewPagerAdapter = new ViewPagerAdapter(this, getApplicationContext(),
                mImageCache, mydb, findViewById(R.id.tab_layout), this);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(viewPagerAdapter);
        new TabLayoutMediator(findViewById(R.id.tab_layout), viewPager,
                (tab, position) -> tab.setText(viewPagerAdapter.getPageTitle(position))
        ).attach();

        Notifications.setupNotifications(getApplicationContext());

        //https://stackoverflow.com/questions/61772241/how-to-make-viewpager2-less-sensitive-to-swipe
        //https://issuetracker.google.com/issues/123006042
        try {
            final Field recyclerViewField = ViewPager2.class.getDeclaredField("pagesRecyclerView");
            recyclerViewField.setAccessible(true);
            final RecyclerView recyclerView = (RecyclerView) recyclerViewField.get(viewPager);
            final Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
            touchSlopField.setAccessible(true);
            final int touchSlop = (int) touchSlopField.get(recyclerView);
            touchSlopField.set(recyclerView, touchSlop * 0.5);
        } catch (Exception ignore) {
        }

        new File(Utils.getDiskCacheFolder(getApplicationContext()), Page.CACHE_SUB_DIRECTORY).mkdirs();
    }
}
