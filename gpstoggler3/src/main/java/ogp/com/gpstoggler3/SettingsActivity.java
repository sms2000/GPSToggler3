package ogp.com.gpstoggler3;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

import java.util.List;

import ogp.com.gpstoggler3.settings.AppCompatPreferenceActivity;
import ogp.com.gpstoggler3.settings.Settings;
import ogp.com.gpstoggler3.global.Constants;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private long backPressedTime = 0;
    private Handler handler = new Handler();


    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };


    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(),
                        ""));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(Constants.TAG, "SettingsActivity::onCreate. Entry...");

        setupActionBar();

        Log.v(Constants.TAG, "SettingsActivity::onCreate. Exit.");
    }


    @Override
    protected void onPause() {
        Log.v(Constants.TAG, "SettingsActivity::onPause. Entry...");

        Settings.reloadSettings();

        super.onPause();

        Log.v(Constants.TAG, "SettingsActivity::onPause. Exit.");
    }


    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }


    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }


    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
               ||
               GeneralPreferenceFragment.class.getName().equals(fragmentName)
               ||
               LifespanPreferenceFragment.class.getName().equals(fragmentName);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("widget_double_click"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (android.R.id.home == id) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LifespanPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_lifespan);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("back_delay"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (android.R.id.home == id) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
        }

        return true;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                final int delay = Settings.getLongBackKeyPressDelay();
                if (0 < delay) {
                    backPressedTime = System.currentTimeMillis();
                    Log.d(Constants.TAG, "SettingsActivity::onKeyDown. Recognized 'back key' pressed. Escape procedure initiated...");

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (0 < backPressedTime) {
                                Log.w(Constants.TAG, "SettingsActivity::onKeyDown. Escaping the termination by 'long back press'. Close the activity before Android killed the whole process.");
                                setResult(Constants.SETTINGS_KILLED);
                                finish();
                            }
                        }
                    }, delay);
                }
        }

        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                backPressedTime = 0;
                Log.d(Constants.TAG, "SettingsActivity::onKeyUp. Recognized 'back key' unpressed. Escape procedure cancelled.");
                break;
        }

        return super.onKeyUp(keyCode, event);
    }
}
