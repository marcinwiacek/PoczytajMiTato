package com.mwiacek.poczytaj.mi.tato;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.mwiacek.poczytaj.mi.tato.read.Page;
import com.mwiacek.poczytaj.mi.tato.read.ReadFragment;
import com.mwiacek.poczytaj.mi.tato.search.SearchFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public final static String PREF_HIDE_NAVIGATION = "HideNavigation";
    public final static String PREF_DONT_BLOCK_SCREEN = "DontBlockScreen";
    public final static String PREF_TEXT_SIZE = "TextSize";

    private static ViewPagerAdapter viewPagerAdapter;
    private static MainActivity mContext;
    private static android.content.SharedPreferences sharedPref;
    private ViewPager2 viewPager;

    public static android.content.SharedPreferences getSharedPref() {
        return sharedPref;
    }

    public static Context getContext() {
        return mContext.getApplicationContext();
    }

    public static ViewPagerAdapter getViewPagerAdapter() {
        return viewPagerAdapter;
    }

    public static void setHiding() {
        if (getSharedPref().getBoolean(PREF_HIDE_NAVIGATION, false)) {
            mContext.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility ->
                    mContext.getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            );
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        if (getSharedPref().getBoolean(PREF_HIDE_NAVIGATION, false)) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      /*  StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());
*/

        setContentView(R.layout.activity_main);

        MainActivity.mContext = this;
        MainActivity.sharedPref = getPreferences(MODE_PRIVATE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.top_layout), (v, windowInsets) -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });
        setHiding();
        if (getSharedPref().getBoolean(PREF_DONT_BLOCK_SCREEN, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this,
                    com.google.android.material.R.color.design_dark_default_color_background
            ));
        }

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
