package com.mwiacek.poczytaj.mi.tato;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class PreferencesActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);

            setTitle("Poczytaj mi tato " + info.versionName);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        ArrayList<Header> target = new ArrayList<>();
        loadHeadersFromResource(R.xml.preference_headers, target);
        for (Header h : target) {
            if (fragmentName.equals(h.fragment)) return true;
        }
        return false;
    }

    public static class StronyFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.preference_strony, false);

            addPreferencesFromResource(R.xml.preference_strony);
        }
    }
}	
