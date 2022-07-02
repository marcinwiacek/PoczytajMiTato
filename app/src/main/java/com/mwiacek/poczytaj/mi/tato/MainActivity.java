package com.mwiacek.poczytaj.mi.tato;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {
    ViewPager viewPager;
    Activity mActitity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActitity = this;

        // getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.pager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this.getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sett:
                Intent intent = new Intent(mActitity, PreferencesActivity.class);
                startActivityForResult(intent, 0);
                //todo: reading list of engines?
                return true;
            case R.id.report:
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                String subject = "";
                try {
                    subject = "Poczytaj mi tato " +
                            getPackageManager().getPackageInfo(getPackageName(), 0).versionName +
                            " / Android " +
                            Build.VERSION.RELEASE;
                } catch (Exception ignored) {
                }
                String[] extra = new String[]{"marcin@mwiacek.com"};
                emailIntent.putExtra(Intent.EXTRA_EMAIL, extra);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.setType("message/rfc822");
                try {
                    startActivity(emailIntent);
                } catch (Exception e) {
                    AlertDialog alertDialog;
                    alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle("Informacja");
                    alertDialog.setMessage("Błąd stworzenia maila");
                    alertDialog.show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
