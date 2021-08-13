package com.cooper.wheellog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.cooper.wheellog.companion.WearOs;

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

    private BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;
    private BleStateEnum mConnectionState = BleStateEnum.Disconnected;
    private boolean doubleBackToExitPressedOnce = false;
    private Snackbar snackbar;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private WearOs wearOs;
    //endregion

    protected static final int RESULT_DEVICE_SCAN_REQUEST = 20;
    protected static final int RESULT_REQUEST_ENABLE_BT = 30;
    protected static final int ResultPrivatePolicy = 666;

    private static Boolean onDestroyProcess = false;

    private BleConnector getBleConnector() {
        return WheelData.getInstance().getBleConnector();
    }

    @SuppressLint("StringFormatInvalid")
    private void setConnectionState(BleStateEnum connectionState) {
        switch (connectionState) {
            case Connected:
                pagerAdapter.configureSecondDisplay();
                if (mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                    WheelLog.AppConfig.setLastMac(mDeviceAddress);
                    if (WheelLog.AppConfig.getAutoUploadEc() && WheelLog.AppConfig.getEcToken() != null) {
                        ElectroClub.getInstance().getAndSelectGarageByMacOrShowChooseDialog(WheelLog.AppConfig.getLastMac(), this, s -> null);
                    }
                    if (WheelLog.AppConfig.getUseBeepOnVolumeUp()) {
                        WheelLog.VolumeKeyController.setActive(true);
                    }
                }
                hideSnackBar();
                break;
            case Connecting:
                if (mConnectionState == BleStateEnum.Connecting) {
                    showSnackBar(R.string.bluetooth_direct_connect_failed);
                } else if (getBleConnector() != null && getBleConnector().getDisconnectTime() != null) {
                    showSnackBar(
                            getString(R.string.connection_lost_at,
                                    timeFormatter.format(getBleConnector().getDisconnectTime())),
                            Snackbar.LENGTH_INDEFINITE);
                }
                break;
            case Disconnected:
                if (WheelLog.AppConfig.getUseBeepOnVolumeUp()) {
                    WheelLog.VolumeKeyController.setActive(false);
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
                    Timber.i("Wheel type switched");
                    pagerAdapter.configureSecondDisplay();
                    pagerAdapter.updateScreen(true);
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    pagerAdapter.updateScreen(intent.hasExtra(Constants.INTENT_EXTRA_GRAPH_UPDATE_AVILABLE));
                    break;
                case Constants.ACTION_WHEEL_NEWS_AVAILABLE:
                    Timber.i("Received news");
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
                    int alarmType = ((ALARM_TYPE) intent.getSerializableExtra(Constants.INTENT_EXTRA_ALARM_TYPE)).getValue();
                    double alarmValue = intent.getDoubleExtra(Constants.INTENT_EXTRA_ALARM_VALUE, 0d);
                    if (alarmType < 4) {
                        showSnackBar(getResources().getString(R.string.alarm_text_speed)+String.format(": %.1f",alarmValue), 3000);
                    }
                    if (alarmType == 4) {
                        showSnackBar(getResources().getString(R.string.alarm_text_current)+String.format(": %.1f",alarmValue), 3000);
                    }
                    if (alarmType == 5) {
                        showSnackBar(getResources().getString(R.string.alarm_text_temperature)+String.format(": %.1f",alarmValue), 3000);
                    }
                    if (alarmType == 6) {
                        showSnackBar(getResources().getString(R.string.alarm_text_pwm)+String.format(": %.1f",alarmValue), 3000);
                    }
                    break;
            }
        }
    };

    /**
     * A broadcast receiver that always works. It shouldn't have any UI work.
     **/
    private final BroadcastReceiver mCoreBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("StringFormatInvalid")
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    BleStateEnum connectionState = BleStateEnum.values()[intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BleStateEnum.Disconnected.ordinal())];
                    Timber.i("Bluetooth state = %s", connectionState);
                    setConnectionState(connectionState);
                    WheelData.getInstance().setConnected(connectionState == BleStateEnum.Connected);
                    switch (connectionState) {
                        case Connected:
                            if (!LoggingService.isInstanceCreated() &&
                                    WheelLog.AppConfig.getAutoLog() &&
                                    !WheelLog.AppConfig.getStartAutoLoggingWhenIsMoving()) {
                                toggleLoggingService();
                            }
                            if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                                KingsongAdapter.getInstance().requestNameData();
                            }
                            if (WheelLog.AppConfig.getAutoWatch() && wearOs == null) {
                                toggleWatch();
                            }
                            WheelLog.Notifications.setNotificationMessageId(R.string.connected);
                            break;
                        case Disconnected:
                            switch (WheelData.getInstance().getWheelType()) {
                                case INMOTION:
                                    InMotionAdapter.newInstance();
                                case INMOTION_V2:
                                    InmotionAdapterV2.newInstance();
                                case NINEBOT_Z:
                                    NinebotZAdapter.newInstance();
                                case NINEBOT:
                                    NinebotAdapter.newInstance();
                            }
                            WheelLog.Notifications.setNotificationMessageId(R.string.disconnected);
                            break;
                        case Connecting:
                            if (intent.hasExtra(Constants.INTENT_EXTRA_BLE_AUTO_CONNECT)) {
                                WheelLog.Notifications.setNotificationMessageId(R.string.searching);
                            } else {
                                WheelLog.Notifications.setNotificationMessageId(R.string.connecting);
                            }
                            break;
                    }
                    WheelLog.Notifications.update();
                    break;
                case Constants.ACTION_PREFERENCE_RESET:
                    Timber.i("Reset battery lowest");
                    pagerAdapter.getWheelView().resetBatteryLowest();
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    if (wearOs != null) {
                        wearOs.updateData();
                    }
                    if (WheelLog.AppConfig.getMibandMode() != MiBandEnum.Alarm) {
                        WheelLog.Notifications.update();
                    }
                    if (!LoggingService.isInstanceCreated() &&
                            WheelLog.AppConfig.getStartAutoLoggingWhenIsMoving() &&
                            WheelLog.AppConfig.getAutoLog() &&
                            WheelData.getInstance().getSpeedDouble() > 3.5) {
                        toggleLoggingService();
                    }
                    break;
                case Constants.ACTION_PEBBLE_SERVICE_TOGGLED:
                    setMenuIconStates();
                    WheelLog.Notifications.update();
                    break;
                case Constants.ACTION_LOGGING_SERVICE_TOGGLED:
                    boolean running = intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
                    if (intent.hasExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)) {
                        String filepath = intent.getStringExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION);
                        if (running) {
                            showSnackBar(getResources().getString(R.string.started_logging, filepath), 5000);
                        }
                    }
                    setMenuIconStates();
                    WheelLog.Notifications.update();
                    break;
                case Constants.NOTIFICATION_BUTTON_CONNECTION:
                    toggleConnectToWheel();
                    WheelLog.Notifications.update();
                    break;
                case Constants.NOTIFICATION_BUTTON_LOGGING:
                    toggleLogging();
                    WheelLog.Notifications.update();
                    break;
                case Constants.NOTIFICATION_BUTTON_WATCH:
                    toggleWatch();
                    WheelLog.Notifications.update();
                    break;
                case Constants.NOTIFICATION_BUTTON_BEEP:
                    SomeUtil.playBeep(getApplicationContext());
                    break;
                case Constants.NOTIFICATION_BUTTON_LIGHT:
                    if (WheelData.getInstance().getAdapter() != null) {
                        WheelData.getInstance().getAdapter().switchFlashlight();
                    }
                    break;
                case Constants.NOTIFICATION_BUTTON_MIBAND:
                    toggleSwitchMiBand();
                    break;
            }
        }
    };

    private void toggleWatch() {
        togglePebbleService();
        if (WheelLog.AppConfig.getGarminConnectIqEnable())
            toggleGarminConnectIQ();
        else
            stopGarminConnectIQ();
        toggleWearOs();
    }

    private void toggleLogging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toggleLoggingService();
        } else {
            MainActivityPermissionsDispatcher.toggleLoggingServiceLegacyWithPermissionCheck(this);
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

        if (LoggingService.isInstanceCreated()) {
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
        AppCompatDelegate.setDefaultNightMode(WheelLog.AppConfig.getDayNightThemeMode());
        setTheme(WheelLog.AppConfig.getAppTheme());

        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main);
        WheelData.initiate();

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
            WheelData.getInstance().setBleConnector(new BleConnector(getApplicationContext()));
        }

        registerReceiver(mCoreBroadcastReceiver, makeCoreIntentFilter());
        WheelLog.Notifications.update();

        if (WheelLog.AppConfig.getDetectBatteryOptimization()) {
            SomeUtil.Companion.checkBatteryOptimizationsAndShowAlert(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getBleConnector() != null && mConnectionState != getBleConnector().getConnectionState()) {
            setConnectionState(getBleConnector().getConnectionState());
        }

        if (WheelData.getInstance().getWheelType() != WHEEL_TYPE.Unknown) {
            pagerAdapter.configureSecondDisplay();
        }

        registerReceiver(mMainViewBroadcastReceiver, makeIntentFilter());
        pagerAdapter.updateScreen(true);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setMenuIconStates();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mMainViewBroadcastReceiver);
        WheelLog.Notifications.update();
    }

    @Override
    protected void onDestroy() {
        onDestroyProcess = true;
        if (wearOs != null) {
            wearOs.stop();
        }
        stopPebbleService();
        stopGarminConnectIQ();
        stopLoggingService();
        WheelData.getInstance().full_reset();
        unregisterReceiver(mCoreBroadcastReceiver);
        if (getBleConnector() != null) {
            WheelData.getInstance().setBleConnector(null);
        }
        WheelLog.ThemeManager.changeAppIcon(MainActivity.this);
        WheelLog.Notifications.cancel();
        super.onDestroy();
        new CountDownTimer(60000 /* 1 min */, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!LoggingService.isInstanceCreated()) {
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
        switch (item.getItemId()) {
            case R.id.miSearch:
                MainActivityPermissionsDispatcher.startScanActivityWithPermissionCheck(this);
                return true;
            case R.id.miWheel:
                toggleConnectToWheel();
                return true;
            case R.id.miLogging:
                if (LoggingService.isInstanceCreated() && WheelLog.AppConfig.getContinueThisDayLog()) {
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
                toggleWatch();
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
    private void stopLoggingService() {
        if (LoggingService.isInstanceCreated()) {
            toggleLoggingService();
        }
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void toggleLoggingServiceLegacy() {
        toggleLoggingService();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void toggleLoggingService() {
        Intent dataLoggerServiceIntent = new Intent(getApplicationContext(), LoggingService.class);
        if (LoggingService.isInstanceCreated()) {
            stopService(dataLoggerServiceIntent);
            if (!onDestroyProcess) {
                new Handler().postDelayed(() -> pagerAdapter.updatePageOfTrips(), 200);
            }
        }
        else if (mConnectionState == BleStateEnum.Connected)
            startService(dataLoggerServiceIntent);
    }

    private void stopPebbleService() {
        if (PebbleService.isInstanceCreated())
            togglePebbleService();
    }

    private void togglePebbleService() {
        Intent pebbleServiceIntent = new Intent(getApplicationContext(), PebbleService.class);
        if (PebbleService.isInstanceCreated())
            stopService(pebbleServiceIntent);
        else
            ContextCompat.startForegroundService(this, pebbleServiceIntent);
    }

    private void toggleWearOs() {
        if (wearOs == null) {
            wearOs = new WearOs(this);
        } else {
            wearOs.stop();
            wearOs = null;
        }
    }

    private void stopGarminConnectIQ() {
        if (GarminConnectIQ.Companion.isInstanceCreated())
            toggleGarminConnectIQ();
    }

    private void toggleGarminConnectIQ() {
        Intent garminConnectIQIntent = new Intent(getApplicationContext(), GarminConnectIQ.class);
        if (GarminConnectIQ.Companion.isInstanceCreated())
            stopService(garminConnectIQIntent);
        else
            ContextCompat.startForegroundService(this, garminConnectIQIntent);
    }

    private void toggleSwitchMiBand() {
        MiBandEnum buttonMiBand = WheelLog.AppConfig.getMibandMode().next();
        WheelLog.AppConfig.setMibandMode(buttonMiBand);
        WheelLog.Notifications.update();

        switch (buttonMiBand) {
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
        Timber.i("onActivityResult");
        switch (requestCode) {
            case RESULT_DEVICE_SCAN_REQUEST:
                if (resultCode == RESULT_OK && getBleConnector() != null) {
                    mDeviceAddress = data.getStringExtra("MAC");
                    Timber.i("Device selected = %s", mDeviceAddress);
                    String mDeviceName = data.getStringExtra("NAME");
                    Timber.i("Device selected = %s", mDeviceName);
                    getBleConnector().setDeviceAddress(mDeviceAddress);
                    WheelData.getInstance().full_reset();
                    WheelData.getInstance().setBtName(mDeviceName);
                    pagerAdapter.updateScreen(true);
                    setMenuIconStates();
                    getBleConnector().close();
                    toggleConnectToWheel();
                    if (WheelLog.AppConfig.getAutoUploadEc() && WheelLog.AppConfig.getEcToken() != null) {
                        ElectroClub.getInstance().getAndSelectGarageByMacOrShowChooseDialog(
                                mDeviceAddress,
                                this,
                                success -> null);
                    }
                }
                break;
            case RESULT_REQUEST_ENABLE_BT:
                if (mBluetoothAdapter.isEnabled())
                    WheelData.getInstance().setBleConnector(new BleConnector(getApplicationContext()));
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
        return intentFilter;
    }

    private IntentFilter makeCoreIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PREFERENCE_RESET);
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_CONNECTION);
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_WATCH);
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_LOGGING);
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_BEEP);
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_LIGHT);
        intentFilter.addAction(Constants.NOTIFICATION_BUTTON_MIBAND);
        return intentFilter;
    }
}
