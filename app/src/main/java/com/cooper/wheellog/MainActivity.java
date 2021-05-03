package com.cooper.wheellog;

import android.Manifest;
import android.app.ActivityOptions;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
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
import android.widget.TextClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.ALARM_TYPE;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.*;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

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
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private boolean doubleBackToExitPressedOnce = false;
    private Snackbar snackbar;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
    //endregion

    protected static final int RESULT_DEVICE_SCAN_REQUEST = 20;
    protected static final int RESULT_REQUEST_ENABLE_BT = 30;
    protected static final int ResultPrivatePolicy = 666;

    private static Boolean onDestroyProcess = false;

    private BluetoothLeService getBluetoothLeService() {
        return WheelData.getInstance().getBluetoothLeService();
    }

    private final ServiceConnection mBluetoothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if (componentName.getClassName().equals(BluetoothLeService.class.getName())) {
                BluetoothLeService bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                WheelData.getInstance().setBluetoothLeService(bluetoothLeService);

                if (!bluetoothLeService.initialize()) {
                    Timber.e(getResources().getString(R.string.error_bluetooth_not_initialised));
                    Toast.makeText(MainActivity.this, R.string.error_bluetooth_not_initialised, Toast.LENGTH_SHORT).show();
                    finish();
                }
                if (bluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED &&
                        mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                    bluetoothLeService.setDeviceAddress(mDeviceAddress);
                    toggleConnectToWheel();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (componentName.getClassName().equals(BluetoothLeService.class.getName())) {
                WheelData.getInstance().setBluetoothLeService(null);
                WheelData.getInstance().setConnected(false);
                Timber.e("BluetoothLeService disconnected");
            }
        }
    };

    private void setConnectionState(int connectionState) {
        switch (connectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                pagerAdapter.configureSecondDisplay();
                if (mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                    WheelLog.AppConfig.setLastMac(mDeviceAddress);
                    if (WheelLog.AppConfig.getAutoUploadEc() && WheelLog.AppConfig.getEcToken() != null) {
                        ElectroClub.getInstance().getAndSelectGarageByMacOrShowChooseDialog(WheelLog.AppConfig.getLastMac(), this, s -> null);
                    }
                }
                hideSnackBar();
                break;
            case BluetoothLeService.STATE_CONNECTING:
                if (mConnectionState == BluetoothLeService.STATE_CONNECTING) {
                    showSnackBar(R.string.bluetooth_direct_connect_failed);
                } else if (getBluetoothLeService() != null && getBluetoothLeService().getDisconnectTime() != null) {
                    showSnackBar(
                            getString(R.string.connection_lost_at,
                                    timeFormatter.format(getBluetoothLeService().getDisconnectTime())),
                            Snackbar.LENGTH_INDEFINITE);
                }
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                break;
        }
        mConnectionState = connectionState;
        setMenuIconStates();
    }

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
                    if (alarmType < 4) {
                        showSnackBar(getResources().getString(R.string.alarm_text_speed), 3000);
                    }
                    if (alarmType == 4) {
                        showSnackBar(getResources().getString(R.string.alarm_text_current), 3000);
                    }
                    if (alarmType == 5) {
                        showSnackBar(getResources().getString(R.string.alarm_text_temperature), 3000);
                    }
                    break;
            }
        }
    };

    private final BroadcastReceiver mCoreBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    int connectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BluetoothLeService.STATE_DISCONNECTED);
                    Timber.i("Bluetooth state = %d", connectionState);
                    setConnectionState(connectionState);
                    WheelData.getInstance().setConnected(connectionState == BluetoothLeService.STATE_CONNECTED);
                    switch (connectionState) {
                        case BluetoothLeService.STATE_CONNECTED:
                            if (!LoggingService.isInstanceCreated() && WheelLog.AppConfig.getAutoLog()) {
                                toggleLoggingService();
                            }
                            if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                                KingsongAdapter.getInstance().requestNameData();
                            }
                            WheelLog.Notifications.setNotificationMessageId(R.string.connected);
                            break;
                        case BluetoothLeService.STATE_DISCONNECTED:
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
                        case BluetoothLeService.STATE_CONNECTING:
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
                    WheelLog.Notifications.update();
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
                miBand.setIcon(R.drawable.ic_mi_alarm);
                break;
            case Min:
                miBand.setIcon(R.drawable.ic_mi_min);
                break;
            case Medium:
                miBand.setIcon(R.drawable.ic_mi_med);
                break;
            case Max:
                miBand.setIcon(R.drawable.ic_mi_max);
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
            case BluetoothLeService.STATE_CONNECTED:
                miWheel.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.ic_action_wheel_orange));
                miWheel.setTitle(R.string.disconnect_from_wheel);
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                miLogging.setEnabled(true);
                miLogging.getIcon().setAlpha(255);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                miWheel.setIcon(WheelLog.ThemeManager.getDrawableId(R.drawable.anim_wheel_icon));
                miWheel.setTitle(R.string.disconnect_from_wheel);
                ((AnimationDrawable) miWheel.getIcon()).start();
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                miLogging.setEnabled(false);
                miLogging.getIcon().setAlpha(64);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
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
        mBluetoothAdapter = BluetoothLeService.getAdapter(this);
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!mBluetoothAdapter.isEnabled())
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RESULT_REQUEST_ENABLE_BT);
        } else {
            startBluetoothService();
        }

        registerReceiver(mCoreBroadcastReceiver, makeCoreIntentFilter());
        WheelLog.Notifications.update();
        if (WheelLog.AppConfig.getUseBeepOnVolumeUp()) {
            WheelLog.VolumeKeyController.setActive(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getBluetoothLeService() != null && mConnectionState != getBluetoothLeService().getConnectionState()) {
            setConnectionState(getBluetoothLeService().getConnectionState());
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
    }

    @Override
    protected void onDestroy() {
        stopPebbleService();
        stopGarminConnectIQ();
        stopLoggingService();
        WheelData.getInstance().full_reset();
        if (getBluetoothLeService() != null) {
            unbindService(mBluetoothServiceConnection);
            WheelData.getInstance().setBluetoothLeService(null);
        }
        WheelLog.ThemeManager.changeAppIcon(MainActivity.this);
        super.onDestroy();
        onDestroyProcess = true;
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
                unregisterReceiver(mCoreBroadcastReceiver);
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
                toggleLogging();
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
        if (LoggingService.isInstanceCreated())
            toggleLoggingService();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void toggleLoggingServiceLegacy() {
        toggleLoggingService();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void toggleLoggingService() {
        Intent dataLoggerServiceIntent = new Intent(getApplicationContext(), LoggingService.class);
        if (LoggingService.isInstanceCreated())
            stopService(dataLoggerServiceIntent);
        else if (mConnectionState == BluetoothLeService.STATE_CONNECTED)
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

    private void stopGarminConnectIQ() {
        if (GarminConnectIQ.isInstanceCreated())
            toggleGarminConnectIQ();
    }

    private void toggleGarminConnectIQ() {
        Intent garminConnectIQIntent = new Intent(getApplicationContext(), GarminConnectIQ.class);
        if (GarminConnectIQ.isInstanceCreated())
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

    private void startBluetoothService() {
        Intent bluetoothServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        bindService(bluetoothServiceIntent, mBluetoothServiceConnection, BIND_AUTO_CREATE);
    }
    //endregion

    private void toggleConnectToWheel() {
        if (getBluetoothLeService() != null) {
            getBluetoothLeService().toggleConnectToWheel();
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
                if (resultCode == RESULT_OK && getBluetoothLeService() != null) {
                    mDeviceAddress = data.getStringExtra("MAC");
                    Timber.i("Device selected = %s", mDeviceAddress);
                    String mDeviceName = data.getStringExtra("NAME");
                    Timber.i("Device selected = %s", mDeviceName);
                    getBluetoothLeService().setDeviceAddress(mDeviceAddress);
                    WheelData.getInstance().full_reset();
                    WheelData.getInstance().setBtName(mDeviceName);
                    pagerAdapter.updateScreen(true);
                    setMenuIconStates();
                    getBluetoothLeService().close();
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
                    startBluetoothService();
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
