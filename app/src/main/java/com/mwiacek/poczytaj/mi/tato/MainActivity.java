package com.mwiacek.poczytaj.mi.tato;


import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.ImageCache;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;

import java.lang.reflect.Field;

/*
TODO inform other pages about updates
TODO symbioza obrazki
TODO EPUB import eksport
TODO sync w background
TODO czerwone teksty (niewidoczne na serwerze)
TODO tor
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
            if (f instanceof ReadFragment && (
                    viewPager.getCurrentItem() == 0 ||
                            viewPager.getCurrentItem() == ((ReadFragment) f).getTabNum())) {
                ((ReadFragment) f).onBackPressed();
                return;
            } else if (f instanceof SearchFragment && (
                    viewPager.getCurrentItem() == 0 ||
                            viewPager.getCurrentItem() == ((SearchFragment) f).getTabNum())) {
                ((SearchFragment) f).onBackPressed();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageCache mImageCache = new ImageCache(getApplicationContext());
        DBHelper mydb = new DBHelper(getApplicationContext());

        // getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        viewPagerAdapter = new ViewPagerAdapter(this, getApplicationContext(),
                mImageCache, mydb, findViewById(R.id.tab_layout), displayMetrics.widthPixels, this);
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
            touchSlopField.set(recyclerView, touchSlop);//6 is empirical value
        } catch (Exception ignore) {
        }
    }
}
