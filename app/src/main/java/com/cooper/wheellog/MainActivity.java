package com.cooper.wheellog;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.cooper.wheellog.services.CoreService;
import com.cooper.wheellog.services.LoggingService;
import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.ALARM_TYPE;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.*;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import me.relex.circleindicator.CircleIndicator3;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    public static AudioManager audioManager = null;

    private EventsLoggingTree eventsLoggingTree;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }

    //region private variables
    ViewPager2 pager;
    MainPageAdapter pagerAdapter;

    Menu mMenu;
    MenuItem miSearch;
    MenuItem miWheel;
    MenuItem miWatch;
    MenuItem miBand;
    MenuItem miLogging;

    private CoreService mCoreService;
    private Boolean coreServiceIsBinded = false;
    private BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;
    private BleStateEnum mConnectionState = BleStateEnum.Disconnected;
    private boolean doubleBackToExitPressedOnce = false;
    private Snackbar snackbar;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss ", Locale.US);
    //endregion

    protected static final int RESULT_DEVICE_SCAN_REQUEST = 20;
    protected static final int RESULT_REQUEST_ENABLE_BT = 30;
    protected static final int ResultPrivatePolicy = 666;

    private static Boolean onDestroyProcess = false;

    private BleConnector getBleConnector() {
        return WheelData.getInstance().getBleConnector();
    }

    private void setConnectionState(BleStateEnum connectionState) {
        switch (connectionState) {
            case Connected:
                pagerAdapter.configureSecondDisplay();
                hideSnackBar();
                break;
            case Connecting:
                if (mConnectionState == BleStateEnum.Connecting) {
                    showSnackBar(R.string.bluetooth_direct_connect_failed);
                } else if (getBleConnector() != null && getBleConnector().getDisconnectTime() != null) {
                    var text = timeFormatter.format(getBleConnector().getDisconnectTime()) +
                            getString(R.string.connection_lost_at);
                    showSnackBar(text, Snackbar.LENGTH_INDEFINITE);
                }
                break;
        }
        mConnectionState = connectionState;
        setMenuIconStates();
    }

    /**
     * Broadcast receiver for MainView UI. It should only work with UI elements.
     * Intents are accepted only if MainView is active.
     **/
    private final BroadcastReceiver mMainViewBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.ACTION_WHEEL_TYPE_CHANGED:
                    Timber.i("[ma] Wheel type switched");
                    pagerAdapter.configureSecondDisplay();
                    pagerAdapter.updateScreen(true);
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    pagerAdapter.updateScreen(intent.hasExtra(Constants.INTENT_EXTRA_GRAPH_UPDATE_AVILABLE));
                    break;
                case Constants.ACTION_WHEEL_NEWS_AVAILABLE:
                    Timber.i("[ma] Received news");
                    showSnackBar(intent.getStringExtra(Constants.INTENT_EXTRA_NEWS), 1500);
                    break;
                case Constants.ACTION_WHEEL_TYPE_RECOGNIZED:
                    if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.NINEBOT_Z
                            && WheelData.getInstance().getProtoVer().equals("")) { // Hide bms for ninebot S2
                        pagerAdapter.addPage(R.layout.main_view_smart_bms);
                    } else {
                        pagerAdapter.removePage(R.layout.main_view_smart_bms);
                    }
                    break;
                case Constants.ACTION_ALARM_TRIGGERED:
                    ALARM_TYPE alarmType = (ALARM_TYPE) intent.getSerializableExtra(Constants.INTENT_EXTRA_ALARM_TYPE);
                    String alarmValue = "undefined alarm";
                    switch (alarmType) {
                        case SPEED1:
                        case SPEED2:
                        case SPEED3:
                            alarmValue = getString(R.string.alarm_text_speed);
                            break;
                        case CURRENT:
                            alarmValue = getString(R.string.alarm_text_current);
                            break;
                        case TEMPERATURE:
                            alarmValue = getString(R.string.alarm_text_temperature);
                            break;
                        case PWM:
                            alarmValue = getString(R.string.alarm_text_pwm);
                            break;
                    }
                    alarmValue += String.format(Locale.US, ": %.1f", intent.getDoubleExtra(Constants.INTENT_EXTRA_ALARM_VALUE, 0d));
                    showSnackBar(alarmValue, 3000);
                    break;
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    BleStateEnum connectionState = BleStateEnum.values()[intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BleStateEnum.Disconnected.ordinal())];
                    setConnectionState(connectionState);
                    break;
                case Constants.ACTION_PREFERENCE_RESET:
                    Timber.i("[ma] Reset battery lowest");
                    pagerAdapter.getWheelView().resetBatteryLowest();
                    break;
                case Constants.ACTION_PEBBLE_SERVICE_TOGGLED:
                    setMenuIconStates();
                    break;
                case Constants.ACTION_LOGGING_SERVICE_TOGGLED:
                    boolean running = intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
                    if (intent.hasExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)) {
                        var filepath = " " + intent.getStringExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION);
                        if (running) {
                            var text = getString(R.string.started_logging) + filepath;
                            showSnackBar(text, 5000);
                        }
                    }
                    setMenuIconStates();
                    break;
            }
        }
    };

    private final ServiceConnection mCoreServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mCoreService = ((CoreService.LocalBinder) service).getService();
            WheelData.getInstance().setCoreService(mCoreService);
            Timber.i("[ma] CoreService connected.");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCoreService = null;
            WheelData.getInstance().setCoreService(null);
            Timber.e("[ma] CoreService disconnected");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Timber.i("[ma] onBindingDied - coreService Died -=|o_0]=- how the hell did this happen?");
            if (!onDestroyProcess) {
                Timber.i("[ma] onBindingDied - restarting coreService");
                stopCoreService();
                startCoreService();
            }
        }
    };

    private void startCoreService() {
        Timber.i("[ma] startCoreService called.");
        coreServiceIsBinded = bindService(new Intent(this, CoreService.class),
                mCoreServiceConnection,
                BIND_AUTO_CREATE | BIND_IMPORTANT);
    }

    private void stopCoreService() {
        Timber.i("[ma] stopCoreService called.");
        if (coreServiceIsBinded) {
            coreServiceIsBinded = false;
            unbindService(mCoreServiceConnection);
        }
    }

    private void toggleLogging() {
        Timber.i("[ma] toggleLogging called.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toggleLogger();
        } else {
            MainActivityPermissionsDispatcher.toggleLoggerLegacyWithPermissionCheck(this);
        }
    }

    private void setMenuIconStates() {
        if (mMenu == null)
            return;

        if (mDeviceAddress == null || mDeviceAddress.isEmpty()) {
            miWheel.setEnabled(false);
            miWheel.getIcon().setAlpha(64);
        } else {
            miWheel.setEnabled(true);
            miWheel.getIcon().setAlpha(255);
        }

        switch (WheelLog.AppConfig.getMibandMode()) {
            case Alarm:
                miBand.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_mi_alarm));
                break;
            case Min:
                miBand.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_mi_min));
                break;
            case Medium:
                miBand.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_mi_med));
                break;
            case Max:
                miBand.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_mi_max));
                break;
        }

        if (WheelLog.AppConfig.getMibandOnMainscreen()) {
            miBand.setVisible(true);
            miWatch.setVisible(false);
        } else {
            miBand.setVisible(false);
            miWatch.setVisible(true);
        }

        if (PebbleService.isInstanceCreated()) {
            miWatch.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_watch_orange));
        } else {
            miWatch.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_watch_white));
        }

        if (LoggingService.isStarted()) {
            miLogging.setTitle(R.string.stop_data_service);
            miLogging.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_logging_orange));
        } else {
            miLogging.setTitle(R.string.start_data_service);
            miLogging.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_logging_white));
        }

        switch (mConnectionState) {
            case Connected:
                miWheel.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_wheel_orange));
                miWheel.setTitle(R.string.disconnect_from_wheel);
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                miLogging.setEnabled(true);
                miLogging.getIcon().setAlpha(255);
                break;
            case Connecting:
                miWheel.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.anim_wheel_icon));
                miWheel.setTitle(R.string.disconnect_from_wheel);
                ((AnimationDrawable) miWheel.getIcon()).start();
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                miLogging.setEnabled(false);
                miLogging.getIcon().setAlpha(64);
                break;
            case Disconnected:
                miWheel.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_wheel_white));
                miWheel.setTitle(R.string.connect_to_wheel);
                miSearch.setEnabled(true);
                miSearch.getIcon().setAlpha(255);
                miLogging.setEnabled(false);
                miLogging.getIcon().setAlpha(64);
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        pagerAdapter.updateScreen(true);
    }

    private void createPager() {
        // add pages into main view
        pager = findViewById(R.id.pager);
        pager.setOffscreenPageLimit(10);

        ArrayList<Integer> pages = new ArrayList<>();
        pages.add(R.layout.main_view_main);
        pages.add(R.layout.main_view_params_list);
        pages.add(R.layout.main_view_graph);
        if (WheelLog.AppConfig.getPageTrips()) {
            pages.add(R.layout.main_view_trips);
        }
        if (WheelLog.AppConfig.getPageEvents()) {
            pages.add(R.layout.main_view_events);
        }

        pagerAdapter = new MainPageAdapter(pages, this);
        pager.setAdapter(pagerAdapter);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                pagerAdapter.setPosition(position);
                pagerAdapter.updateScreen(true);
            }
        });

        eventsLoggingTree = new EventsLoggingTree(getApplicationContext(), pagerAdapter);
        Timber.plant(eventsLoggingTree);

        CircleIndicator3 indicator = findViewById(R.id.indicator);
        indicator.setViewPager(pager);
        pagerAdapter.registerAdapterDataObserver(indicator.getAdapterDataObserver());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (onDestroyProcess) {
            android.os.Process.killProcess(android.os.Process.myPid());
            return;
        }
        Timber.i("[ma] onCreate called");

        AppCompatDelegate.setDefaultNightMode(WheelLog.AppConfig.getDayNightThemeMode());
        setTheme(WheelLog.AppConfig.getAppTheme());

        WheelData.initiate();
        WheelLog.Notifications.update();

        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main);

        ElectroClub.getInstance().setErrorListener((method, error) -> {
            String message = "[ec] " + method + " error: " + error;
            Timber.i(message);
            MainActivity.this.runOnUiThread(() -> showSnackBar(message, 4000));
            return null;
        });
        ElectroClub.getInstance().setSuccessListener((method, success) -> {
            if (method.equals(ElectroClub.GET_GARAGE_METHOD)) {
                return null;
            }
            String message = "[ec] " + method + " ok: " + success;
            Timber.i(message);
            MainActivity.this.runOnUiThread(() -> showSnackBar(message, 4000));
            return null;
        });

        createPager();

        // clock font
        TextClock textClock = findViewById(R.id.textClock);
        textClock.setTypeface(WheelLog.ThemeManager.getTypeface(getApplicationContext()));

        mDeviceAddress = WheelLog.AppConfig.getLastMac();
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (!WheelLog.AppConfig.getPrivatePolicyAccepted()) {
            startActivityForResult(new Intent(MainActivity.this, PrivacyPolicyActivity.class), ResultPrivatePolicy);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        // Checks if Bluetooth is supported on the device.
        mBluetoothAdapter = BleConnector.getAdapter(this);
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!mBluetoothAdapter.isEnabled())
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RESULT_REQUEST_ENABLE_BT);
        } else {
            startCoreService();
        }

        if (WheelLog.AppConfig.getDetectBatteryOptimization()) {
            SomeUtil.Companion.checkBatteryOptimizationsAndShowAlert(this);
        }
        Timber.i("[ma] onCreate ended");
    }

    @Override
    public void onResume() {
        Timber.i("[ma] onResume called");
        super.onResume();
        if (getBleConnector() != null && mConnectionState != getBleConnector().getConnectionState()) {
            setConnectionState(getBleConnector().getConnectionState());
        }

        if (WheelData.getInstance().getWheelType() != WHEEL_TYPE.Unknown) {
            pagerAdapter.configureSecondDisplay();
        }

        registerReceiver(mMainViewBroadcastReceiver, makeIntentFilter());
        pagerAdapter.updateScreen(true);
        if (!LoggingService.isStarted()) {
            pagerAdapter.updatePageOfTrips();
        }
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setMenuIconStates();
    }

    @Override
    public void onPause() {
        Timber.i("[ma] onPause called");
        super.onPause();
        unregisterReceiver(mMainViewBroadcastReceiver);
        WheelLog.Notifications.update();
    }

    @Override
    protected void onDestroy() {
        Timber.i("[ma] onDestroy called");
        onDestroyProcess = true;
        if (LoggingService.isStarted()) {
            toggleLogger();
        }
        stopCoreService();
        WheelData.getInstance().full_reset();
        WheelLog.ThemeManager.changeAppIcon(MainActivity.this);
        WheelLog.Notifications.cancel();
        super.onDestroy();
        new CountDownTimer(60000 /* 1 min */, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!LoggingService.isStarted()) {
                    onFinish();
                }
            }

            @Override
            public void onFinish() {
                Timber.uproot(eventsLoggingTree);
                eventsLoggingTree.close();
                eventsLoggingTree = null;
                android.os.Process.killProcess(android.os.Process.myPid());
            }

        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;
        miSearch = mMenu.findItem(R.id.miSearch);
        miWheel = mMenu.findItem(R.id.miWheel);
        miWatch = mMenu.findItem(R.id.miWatch);
        miBand = mMenu.findItem(R.id.miBand);
        miLogging = mMenu.findItem(R.id.miLogging);

        // Themes
        if (WheelLog.AppConfig.getAppTheme() == R.style.AJDMTheme) {
            MenuItem miSettings = mMenu.findItem(R.id.miSettings);
            miSettings.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_baseline_settings_24));
            miSearch.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_bluetooth_searching_white));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.i("[ma] onOptionsItemSelected called.");
        switch (item.getItemId()) {
            case R.id.miSearch:
                MainActivityPermissionsDispatcher.startScanActivityWithPermissionCheck(this);
                return true;
            case R.id.miWheel:
                toggleConnectToWheel();
                return true;
            case R.id.miLogging:
                Timber.i("[ma] logging menu toggled.");
                if (LoggingService.isStarted() && WheelLog.AppConfig.getContinueThisDayLog()) {
                    Timber.i("[ma] finish trip alert show.");
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.continue_this_day_log_alert_title)
                            .setMessage(R.string.continue_this_day_log_alert_description)
                            .setPositiveButton(android.R.string.yes, (dialog1, which) -> {
                                WheelLog.AppConfig.setContinueThisDayLogMacException(WheelLog.AppConfig.getLastMac());
                                toggleLogging();
                            })
                            .setNegativeButton(android.R.string.no, (dialog1, which) -> toggleLogging())
                            .create();
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        private static final int AUTO_DISMISS_MILLIS = 5000;
                        @Override
                        public void onShow(final DialogInterface dialog) {
                            final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                            final CharSequence negativeButtonText = defaultButton.getText();
                            new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    defaultButton.setText(String.format(
                                            Locale.getDefault(), "%s (%d)",
                                            negativeButtonText,
                                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1
                                    ));
                                }
                                @Override
                                public void onFinish() {
                                    if (((AlertDialog) dialog).isShowing()) {
                                        WheelLog.AppConfig.setContinueThisDayLogMacException(WheelLog.AppConfig.getLastMac());
                                        toggleLogging();
                                        dialog.dismiss();
                                    }
                                }
                            }.start();
                        }
                    });
                    dialog.show();
                } else {
                    toggleLogging();
                }
                return true;
            case R.id.miWatch:
                mCoreService.toggleWatch();
                return true;
            case R.id.miBand:
                toggleSwitchMiBand();
                return true;
            case R.id.miSettings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                ? ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                                : null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (doubleBackToExitPressedOnce) {
                    finish();
                    return true;
                }

                doubleBackToExitPressedOnce = true;
                showSnackBar(R.string.back_to_exit);

                new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void showSnackBar(int msg) {
        showSnackBar(getString(msg));
    }

    private void showSnackBar(String msg) {
        showSnackBar(msg, 2000);
    }

    private void showSnackBar(String msg, int timeout) {
        if (snackbar == null) {
            View mainView = findViewById(R.id.main_view);
            snackbar = Snackbar
                    .make(mainView, "", Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundResource(R.color.primary_dark);
            snackbar.setAction(android.R.string.ok, view -> {
            });
        }
        snackbar.setDuration(timeout);
        snackbar.setText(msg);
        snackbar.show();
        Timber.wtf(msg);
    }

    private void hideSnackBar() {
        if (snackbar == null)
            return;

        snackbar.dismiss();
    }

    //region services
    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void toggleLoggerLegacy() {
        toggleLogger();
    }

    void toggleLogger() {
        if (!mCoreService.toggleLogger() && !onDestroyProcess) {
            new Handler().postDelayed(() -> pagerAdapter.updatePageOfTrips(), 200);
        }
    }

    private void toggleSwitchMiBand() {
        switch (mCoreService.toggleSwitchMiBand()) {
            case Alarm:
                showSnackBar(R.string.alarmmiband);
                break;
            case Min:
                showSnackBar(R.string.minmiband);
                break;
            case Medium:
                showSnackBar(R.string.medmiband);
                break;
            case Max:
                showSnackBar(R.string.maxmiband);
                break;
        }
        setMenuIconStates();
    }
    //endregion

    private void toggleConnectToWheel() {
        if (getBleConnector() != null) {
            getBleConnector().toggleConnectToWheel();
        }
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void startScanActivity() {
        startActivityForResult(new Intent(MainActivity.this, ScanActivity.class), RESULT_DEVICE_SCAN_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.i("[ma] onActivityResult");
        switch (requestCode) {
            case RESULT_DEVICE_SCAN_REQUEST:
                if (resultCode == RESULT_OK && getBleConnector() != null) {
                    mDeviceAddress = data.getStringExtra("MAC");
                    Timber.i("[ma] Device selected = %s", mDeviceAddress);
                    String mDeviceName = data.getStringExtra("NAME");
                    Timber.i("[ma] Device selected = %s", mDeviceName);
                    getBleConnector().setDeviceAddress(mDeviceAddress);
                    WheelData.getInstance().full_reset();
                    WheelData.getInstance().setBtName(mDeviceName);
                    pagerAdapter.updateScreen(true);
                    setMenuIconStates();
                    getBleConnector().close();
                    toggleConnectToWheel();

                }
                break;
            case RESULT_REQUEST_ENABLE_BT:
                if (mBluetoothAdapter.isEnabled())
                    startCoreService();
                else {
                    Toast.makeText(this, R.string.bluetooth_required, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case ResultPrivatePolicy:
                if (resultCode == RESULT_OK) {
                    WheelLog.AppConfig.setPrivatePolicyAccepted(true);
                } else {
                    finish();
                }
                break;
        }
    }

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_RECOGNIZED);
        intentFilter.addAction(Constants.ACTION_ALARM_TRIGGERED);
        intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_CHANGED);
        intentFilter.addAction(Constants.ACTION_WHEEL_NEWS_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_PREFERENCE_RESET);
        return intentFilter;
    }
}
