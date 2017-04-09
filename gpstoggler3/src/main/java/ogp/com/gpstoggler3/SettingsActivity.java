package ogp.com.gpstoggler3;


import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import java.util.List;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.settings.AppCompatPreferenceActivity;
import ogp.com.gpstoggler3.settings.Settings;


@SuppressLint("LongLogTag")
public class SettingsActivity extends AppCompatPreferenceActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GPSToggler3.SettingsActivity";

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    private static final int ACCOUNT_PICKER_CODE = 2;

    private Preference buttonStore, buttonLoad, buttonAccount;
    private long backPressedTime = 0;
    private Handler handler = new Handler();
    private GoogleApiClient googleApiClient;
    private boolean connected = false;
    private boolean in_process = false;


    private class Finished implements Runnable {
        @Override
        public void run() {
            setInProcess(false);
        }
    }


    private static Preference.OnPreferenceChangeListener bindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                if (!stringValue.isEmpty()) {
                    preference.setSummary(stringValue);
                }
            }

            return true;
        }
    };


    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

        bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
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
                LifespanPreferenceFragment.class.getName().equals(fragmentName)
                ||
                CloudPreferenceFragment.class.getName().endsWith(fragmentName);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("widget_double_click"));
            bindPreferenceSummaryToValue(findPreference("root_timeout_delay"));
            bindPreferenceSummaryToValue(findPreference("on_polling_delay"));
            bindPreferenceSummaryToValue(findPreference("off_polling_delay"));
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


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CloudPreferenceFragment extends PreferenceFragment {
        private SettingsActivity settingsActivity;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_cloud);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.button_google_drive_account)));

            settingsActivity = (SettingsActivity) getActivity();
            settingsActivity.initControls(this);
            settingsActivity.setupControls();
            settingsActivity.connectGoogleDrive();
        }

/*
        @Override
        public void onResume() {
            Log.v(TAG, "SettingsActivity::CloudPreferenceFragment::onResume. Entry...");

            super.onResume();

            Log.v(TAG, "SettingsActivity::CloudPreferenceFragment::onResume. Exit.");
        }


        @Override
        public void onPause() {
            Log.v(TAG, "SettingsActivity::CloudPreferenceFragment::onPause. Entry...");

            super.onPause();

            Log.v(TAG, "SettingsActivity::CloudPreferenceFragment::onPause. Exit.");
        }
*/

        @Override
        public void onDestroy() {
            Log.v(TAG, "SettingsActivity::CloudPreferenceFragment::onDestroy. Entry...");

            settingsActivity.disconnectGoogleDrive();
            super.onDestroy();

            Log.v(TAG, "SettingsActivity::CloudPreferenceFragment::onDestroy. Exit.");
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
    public void onConnected(@Nullable Bundle bundle) {
        Log.v(TAG, "SettingsActivity::onConnected. Entry...");

        setConnected(true);

        Log.v(TAG, "SettingsActivity::onConnected. Exit.");
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "SettingsActivity::onConnectionSuspended. Entry...");

        Log.v(TAG, "SettingsActivity::onConnectionSuspended. Exit.");
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v(TAG, "SettingsActivity::onConnectionFailed. Entry...");

        setConnected(connected);

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                reportError(R.string.error_cannot_resolve_account);
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }

        Log.v(TAG, "SettingsActivity::onConnectionFailed. Exit.");
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.v(TAG, "SettingsActivity::onActivityResult. Entry...");

        switch (requestCode) {
            case ACCOUNT_PICKER_CODE:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "SettingsActivity::onActivityResult. Picker OK.");

                    String accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    setGoogleDriveAccount(accName);
                    connectGoogleDrive();
                } else {
                    Log.i(TAG, "SettingsActivity::onActivityResult. Picker cancelled.");
                }
                break;


            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "SettingsActivity::onActivityResult. Connection OK.");
                    connectGoogleDrive();
                } else {
                    Log.i(TAG, "SettingsActivity::onActivityResult. Connection cancelled.");
                }
                break;


            default:
                Log.w(TAG, String.format("SettingsActivity::onActivityResult. Unknown code [%d].", requestCode));
                break;
        }

        Log.v(TAG, "SettingsActivity::onActivityResult. Exit.");
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


    private void disconnectGoogleDrive() {
        if (null != googleApiClient) {
            googleApiClient.disconnect();
        }

        setConnected(false);
    }


    private void connectGoogleDrive() {
        Log.v(TAG, "SettingsActivity::connectGoogleDrive. Entry...");

        String accName = Settings.retriveAccountName(getString(R.string.button_google_drive_account));
        if (accName.isEmpty()) {
            Log.i(TAG, "SettingsActivity::connectGoogleDrive. No default Google Drive account set. Select one...");

            selectGoogleDriveAccount();
        } else {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .setAccountName(accName)
                    .addApi(Drive.API)
                    .addApi(Auth.CREDENTIALS_API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            Log.i(TAG, String.format("SettingsActivity::connectGoogleDrive. 'googleApiClient' for [%s] created. Connecting...", accName));
            googleApiClient.connect();
        }

        Log.v(TAG, "SettingsActivity::connectGoogleDrive. Exit.");
    }

/*
    private void connectGoogleDrive(boolean reconnect) {
        Log.v(TAG, "SettingsActivity::connectGoogleDrive. Entry...");

        if (null != googleApiClient) {
            if (reconnect) {
                if (connected) {
                    setConnected(false);
                    googleApiClient.clearDefaultAccountAndReconnect();
                }
            } else {
                googleApiClient.connect();
            }
        }

        Log.v(TAG, "SettingsActivity::connectGoogleDrive. Exit.");
    }
*/

    private void setConnected(boolean connected) {
        this.connected = connected;
        setupControls();
    }


    private void setInProcess(boolean in_process) {
        Log.v(TAG, String.format("SettingsActivity::setInProcess. Entry... [%s]", in_process ? "IN" : "OUT"));

        this.in_process = in_process;
        setupControls();

        Log.v(TAG, "SettingsActivity::setInProcess. Exit.");
    }


    private void reportError(int error_id) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.error);
        dialog.setMessage(error_id);
        dialog.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        dialog.show();
    }


    private void initControls(PreferenceFragment fragment) {
        buttonStore = fragment.findPreference(getString(R.string.button_store));
        buttonStore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return storeData();
            }
        });

        buttonLoad = fragment.findPreference(getString(R.string.button_load));
        buttonLoad.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return loadData();
            }
        });

        buttonAccount = fragment.findPreference(getString(R.string.button_google_drive_account));
        buttonAccount.setDefaultValue(getString(R.string.google_drive_account_not_selected));

        buttonAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return selectGoogleDriveAccount();
            }
        });
    }


    private void setupControls() {
        buttonLoad.setEnabled(connected && !in_process);
        buttonStore.setEnabled(connected && !in_process);
        buttonAccount.setEnabled(!in_process);
    }


    private boolean storeData() {
        Log.v(TAG, "SettingsActivity::storeData. Entry...");

        setInProcess(true);

        handler.postDelayed(new Finished(), 1000);

        Log.v(TAG, "SettingsActivity::storeData. Exit.");
        return true;
    }


    private boolean loadData() {
        Log.v(TAG, "SettingsActivity::loadData. Entry...");

        setInProcess(true);
        handler.postDelayed(new Finished(), 1000);

        Log.v(TAG, "SettingsActivity::loadData. Exit.");
        return true;
    }


    private boolean selectGoogleDriveAccount() {
        Log.v(TAG, "SettingsActivity::selectGoogleDriveAccount. Entry...");

        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"}, true, null, null, null, null);
        startActivityForResult(intent, ACCOUNT_PICKER_CODE);

        Log.v(TAG, "SettingsActivity::selectGoogleDriveAccount. Exit.");
        return true;
    }


    private void setGoogleDriveAccount(String accName) {
        Log.v(TAG, String.format("SettingsActivity::setGoogleDriveAccount. Entry... for [%s]", accName));

        Settings.preserveAccountName(getString(R.string.button_google_drive_account), accName);
        buttonAccount.setSummary(accName);

        Log.v(TAG, "SettingsActivity::setGoogleDriveAccount. Exit.");
    }
}
