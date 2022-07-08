package com.mwiacek.poczytaj.mi.tato;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {
    ViewPager2 viewPager;
    ViewPagerAdapter adapter;

    @Override
    public void onBackPressed() {
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof ReadFragment && viewPager.getCurrentItem() == ((ReadFragment) f).config.tabNum) {
                ((ReadFragment) f).onBackPressed();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.pager);
        adapter = new ViewPagerAdapter(this, getApplicationContext());
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(findViewById(R.id.tab_layout), viewPager,
                (tab, position) -> tab.setText(adapter.getPageTitle(position))
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
            touchSlopField.set(recyclerView, touchSlop * 6);//6 is empirical value
        } catch (Exception ignore) {
        }
    }
}
