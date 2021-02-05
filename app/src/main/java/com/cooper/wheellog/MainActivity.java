package com.cooper.wheellog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.viewpager.widget.ViewPager;

import com.cooper.wheellog.presentation.preferences.MultiSelectPreference;
import com.cooper.wheellog.utils.BaseAdapter;
import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.ALARM_TYPE;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.StringUtil;
import com.cooper.wheellog.views.WheelView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.android.material.snackbar.Snackbar;
import com.viewpagerindicator.LinePageIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

import static com.cooper.wheellog.utils.MathsUtil.kmToMiles;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    public static AudioManager audioManager = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }

    //region private variables
    ViewPageAdapter pagerAdapter;
    TextView eventsTextView;
    int eventsCurrentCount = 0;
    int eventsMaxCount = 500;

    Menu mMenu;
    MenuItem miSearch;
    MenuItem miWheel;
    MenuItem miWatch;
    MenuItem miLogging;

    LineChart chart1;

    WheelView wheelView;

    TextView tvBms1Sn;
    TextView tvBms2Sn;
    TextView tvBms1Fw;
    TextView tvBms2Fw;
    TextView tvBms1FactoryCap;
    TextView tvBms2FactoryCap;
    TextView tvBms1ActualCap;
    TextView tvBms2ActualCap;
    TextView tvBms1Cycles;
    TextView tvBms2Cycles;
    TextView tvBms1ChrgCount;
    TextView tvBms2ChrgCount;
    TextView tvBms1MfgDate;
    TextView tvBms2MfgDate;
    TextView tvBms1Status;
    TextView tvBms2Status;
    TextView tvBms1RemCap;
    TextView tvBms2RemCap;
    TextView tvBms1RemPerc;
    TextView tvBms2RemPerc;
    TextView tvBms1Current;
    TextView tvBms2Current;
    TextView tvBms1Voltage;
    TextView tvBms2Voltage;
    TextView tvBms1Temp1;
    TextView tvBms2Temp1;
    TextView tvBms1Temp2;
    TextView tvBms2Temp2;
    TextView tvBms1Health;
    TextView tvBms2Health;
    TextView tvBms1Cell1;
    TextView tvBms2Cell1;
    TextView tvBms1Cell2;
    TextView tvBms2Cell2;
    TextView tvBms1Cell3;
    TextView tvBms2Cell3;
    TextView tvBms1Cell4;
    TextView tvBms2Cell4;
    TextView tvBms1Cell5;
    TextView tvBms2Cell5;
    TextView tvBms1Cell6;
    TextView tvBms2Cell6;
    TextView tvBms1Cell7;
    TextView tvBms2Cell7;
    TextView tvBms1Cell8;
    TextView tvBms2Cell8;
    TextView tvBms1Cell9;
    TextView tvBms2Cell9;
    TextView tvBms1Cell10;
    TextView tvBms2Cell10;
    TextView tvBms1Cell11;
    TextView tvBms2Cell11;
    TextView tvBms1Cell12;
    TextView tvBms2Cell12;
    TextView tvBms1Cell13;
    TextView tvBms2Cell13;
    TextView tvBms1Cell14;
    TextView tvBms2Cell14;
    TextView tvTitleBms1Cell15;
    TextView tvBms1Cell15;
    TextView tvTitleBms2Cell15;
    TextView tvBms2Cell15;
    TextView tvTitleBms1Cell16;
    TextView tvBms1Cell16;
    TextView tvTitleBms2Cell16;
    TextView tvBms2Cell16;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private boolean doubleBackToExitPressedOnce = false;
    private Snackbar snackbar;
    int viewPagerPage = R.id.page_main;
    private ArrayList<String> xAxis_labels = new ArrayList<>();
    private boolean use_mph = false;
    private DrawerLayout mDrawer;
    //endregion

    protected static final int RESULT_DEVICE_SCAN_REQUEST = 20;
    protected static final int RESULT_REQUEST_ENABLE_BT = 30;
    protected static final int RESULT_AUTH_REQUEST = 50;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Timber.e(getResources().getString(R.string.error_bluetooth_not_initialised));
                Toast.makeText(MainActivity.this, R.string.error_bluetooth_not_initialised, Toast.LENGTH_SHORT).show();
                finish();
            }

            loadPreferences();
            if (mBluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED &&
                    mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                mBluetoothLeService.setDeviceAddress(mDeviceAddress);
                toggleConnectToWheel();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            finish();
        }
    };

    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                    int connectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BluetoothLeService.STATE_DISCONNECTED);
                    Timber.i("Bluetooth state = %d", connectionState);
                    setConnectionState(connectionState);
                    break;
                case Constants.ACTION_WHEEL_TYPE_CHANGED:
                    Timber.i("Wheel type switched");
                    getPreferencesFragment().changeWheelType();
                    configureDisplay(WheelData.getInstance().getWheelType());
                    updateScreen(true);
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                        if (WheelData.getInstance().getName().isEmpty())
                            sendBroadcast(new Intent(Constants.ACTION_REQUEST_KINGSONG_NAME_DATA));
                        else if (WheelData.getInstance().getSerial().isEmpty())
                            sendBroadcast(new Intent(Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA));
                    }
                    if (intent.hasExtra(Constants.INTENT_EXTRA_WHEEL_SETTINGS)) {
                        setWheelPreferences();
                    }
                    updateScreen(intent.hasExtra(Constants.INTENT_EXTRA_GRAPH_UPDATE_AVILABLE));
                    break;
                case Constants.ACTION_PEBBLE_SERVICE_TOGGLED:
                    setMenuIconStates();
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
                    break;
                case Constants.ACTION_PREFERENCE_CHANGED:
                    int settingsKey = intent.getIntExtra(Constants.INTENT_EXTRA_SETTINGS_KEY, -1);
                    loadPreferences(settingsKey);
                    break;
                case Constants.ACTION_PREFERENCE_RESET:
                    Timber.i("Reset battery lowest");
                    wheelView.resetBatteryLowest();
                    break;

                case Constants.ACTION_WHEEL_TYPE_RECOGNIZED:
                    if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.NINEBOT_Z
                            && WheelData.getInstance().getProtoVer().equals("")) { // Hide bms for ninebot S2
                        pagerAdapter.showPage(R.id.page_smart_bms);
                    } else {
                        pagerAdapter.hidePage(R.id.page_smart_bms);
                    }
                    findViewById(R.id.indicator).invalidate();
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


    private void setConnectionState(int connectionState) {
        switch (connectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                configureDisplay(WheelData.getInstance().getWheelType());
                if (mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                    WheelLog.AppConfig.setLastMac(mDeviceAddress);
                    if (WheelLog.AppConfig.getAutoUploadEc() && WheelLog.AppConfig.getEcToken() != null) {
                        ElectroClub.getInstance().getAndSelectGarageByMacOrPrimary(WheelLog.AppConfig.getLastMac(), s -> null);
                    }
                }
                hideSnackBar();
                break;
            case BluetoothLeService.STATE_CONNECTING:
                if (mConnectionState == BluetoothLeService.STATE_CONNECTING) {
                    showSnackBar(R.string.bluetooth_direct_connect_failed);

                } else {
                    if (mBluetoothLeService.getDisconnectTime() != null) {
                        showSnackBar(
                                getString(R.string.connection_lost_at,
                                        new SimpleDateFormat("HH:mm:ss", Locale.US).format(mBluetoothLeService.getDisconnectTime())),
                                Snackbar.LENGTH_INDEFINITE);
                    }
                }
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                break;
        }
        mConnectionState = connectionState;
        setMenuIconStates();
    }

    private void setWheelPreferences() {
        Timber.i("SetWheelPreferences");
        getPreferencesFragment().refreshWheelSettings();
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

        if (PebbleService.isInstanceCreated()) {
            miWatch.setIcon(R.drawable.ic_action_watch_orange);
        } else {
            miWatch.setIcon(R.drawable.ic_action_watch_white);
        }

        if (LoggingService.isInstanceCreated()) {
            miLogging.setTitle(R.string.stop_data_service);
            miLogging.setIcon(R.drawable.ic_action_logging_orange);
        } else {
            miLogging.setTitle(R.string.start_data_service);
            miLogging.setIcon(R.drawable.ic_action_logging_white);
        }

        switch (mConnectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_orange);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                miLogging.setEnabled(true);
                miLogging.getIcon().setAlpha(255);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                miWheel.setIcon(R.drawable.anim_wheel_icon);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                ((AnimationDrawable) miWheel.getIcon()).start();
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                miLogging.setEnabled(false);
                miLogging.getIcon().setAlpha(64);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_white);
                miWheel.setTitle(R.string.connect_to_wheel);
                miSearch.setEnabled(true);
                miSearch.getIcon().setAlpha(255);
                miLogging.setEnabled(false);
                miLogging.getIcon().setAlpha(64);
                break;
        }
    }

    //region SecondPage
    private LinkedHashMap<Integer, String> secondPageValues = new LinkedHashMap<>();

    public void setupFieldForSecondPage(int resId)
    {
        secondPageValues.put(resId, "");
    }

    public void updateFieldForSecondPage(int resId, String value)
    {
        if (secondPageValues.containsKey(resId)) {
            secondPageValues.put(resId, value);
        }
    }

    public void createSecondPage() {
        GridLayout layout = findViewById(R.id.page_two_grid);
        layout.removeAllViews();
        for (Map.Entry<Integer, String> entry : secondPageValues.entrySet()) {
            TextView headerText = (TextView) getLayoutInflater().inflate(
                    R.layout.textview_title_template, layout, false);
            TextView valueText = (TextView) getLayoutInflater().inflate(
                    R.layout.textview_value_template, layout, false);
            headerText.setText(getApplicationContext().getString(entry.getKey()));
            valueText.setText(entry.getValue());
            layout.addView(headerText);
            layout.addView(valueText);
        }
    }

    public Boolean updateSecondPage() {
        GridLayout layout = findViewById(R.id.page_two_grid);
        int count = layout.getChildCount();
        if (secondPageValues.size() * 2 != count)
        {
            return false;
        }
        int index = 1;
        for (String value : secondPageValues.values()) {
            TextView valueText = (TextView) layout.getChildAt(index);
            valueText.setText(value);
            index += 2;
        }
        return true;
    }
    //endregion

    private void configureDisplay(WHEEL_TYPE wheelType) {
        tvBms1Cell15.setVisibility(View.GONE);
        tvBms1Cell16.setVisibility(View.GONE);
        tvBms2Cell15.setVisibility(View.GONE);
        tvBms2Cell16.setVisibility(View.GONE);
        tvTitleBms1Cell15.setVisibility(View.GONE);
        tvTitleBms1Cell16.setVisibility(View.GONE);
        tvTitleBms2Cell15.setVisibility(View.GONE);
        tvTitleBms2Cell16.setVisibility(View.GONE);

        secondPageValues.clear();

        switch (wheelType) {
            case KINGSONG:
                setupFieldForSecondPage(R.string.speed);
                setupFieldForSecondPage(R.string.dynamic_speed_limit);
                setupFieldForSecondPage(R.string.top_speed);
                setupFieldForSecondPage(R.string.average_speed);
                setupFieldForSecondPage(R.string.average_riding_speed);
                setupFieldForSecondPage(R.string.battery);
                setupFieldForSecondPage(R.string.output);
                setupFieldForSecondPage(R.string.cpuload);
                setupFieldForSecondPage(R.string.temperature);
                setupFieldForSecondPage(R.string.temperature2);
                setupFieldForSecondPage(R.string.ride_time);
                setupFieldForSecondPage(R.string.riding_time);
                setupFieldForSecondPage(R.string.distance);
                setupFieldForSecondPage(R.string.wheel_distance);
                setupFieldForSecondPage(R.string.user_distance);
                setupFieldForSecondPage(R.string.total_distance);
                setupFieldForSecondPage(R.string.voltage);
                setupFieldForSecondPage(R.string.voltage_sag);
                setupFieldForSecondPage(R.string.current);
                setupFieldForSecondPage(R.string.power);
                setupFieldForSecondPage(R.string.fan_status);
                setupFieldForSecondPage(R.string.charging);
                setupFieldForSecondPage(R.string.mode);
                setupFieldForSecondPage(R.string.name);
                setupFieldForSecondPage(R.string.model);
                setupFieldForSecondPage(R.string.version);
                setupFieldForSecondPage(R.string.serial_number);
                break;

            case VETERAN:
                setupFieldForSecondPage(R.string.speed);
                setupFieldForSecondPage(R.string.top_speed);
                setupFieldForSecondPage(R.string.average_speed);
                setupFieldForSecondPage(R.string.average_riding_speed);
                setupFieldForSecondPage(R.string.battery);
                setupFieldForSecondPage(R.string.temperature);
                setupFieldForSecondPage(R.string.ride_time);
                setupFieldForSecondPage(R.string.riding_time);
                setupFieldForSecondPage(R.string.distance);
                setupFieldForSecondPage(R.string.wheel_distance);
                setupFieldForSecondPage(R.string.user_distance);
                setupFieldForSecondPage(R.string.total_distance);
                setupFieldForSecondPage(R.string.voltage);
                setupFieldForSecondPage(R.string.voltage_sag);
                setupFieldForSecondPage(R.string.current);
                setupFieldForSecondPage(R.string.power);
                setupFieldForSecondPage(R.string.charging);
                setupFieldForSecondPage(R.string.model);
                setupFieldForSecondPage(R.string.version);
                setupFieldForSecondPage(R.string.charging);
                break;

            case GOTWAY:
                setupFieldForSecondPage(R.string.speed);
                setupFieldForSecondPage(R.string.top_speed);
                setupFieldForSecondPage(R.string.average_speed);
                setupFieldForSecondPage(R.string.average_riding_speed);
                setupFieldForSecondPage(R.string.battery);
                setupFieldForSecondPage(R.string.temperature);
                setupFieldForSecondPage(R.string.ride_time);
                setupFieldForSecondPage(R.string.riding_time);
                setupFieldForSecondPage(R.string.distance);
                setupFieldForSecondPage(R.string.wheel_distance);
                setupFieldForSecondPage(R.string.user_distance);
                setupFieldForSecondPage(R.string.total_distance);
                setupFieldForSecondPage(R.string.voltage);
                setupFieldForSecondPage(R.string.voltage_sag);
                setupFieldForSecondPage(R.string.current);
                setupFieldForSecondPage(R.string.power);
                break;

            case INMOTION_V2:
                setupFieldForSecondPage(R.string.speed);
                setupFieldForSecondPage(R.string.dynamic_speed_limit);
                setupFieldForSecondPage(R.string.torque);
                setupFieldForSecondPage(R.string.top_speed);
                setupFieldForSecondPage(R.string.average_speed);
                setupFieldForSecondPage(R.string.average_riding_speed);
                setupFieldForSecondPage(R.string.battery);
                setupFieldForSecondPage(R.string.temperature);
                setupFieldForSecondPage(R.string.temperature2);
                setupFieldForSecondPage(R.string.cpu_temp);
                setupFieldForSecondPage(R.string.imu_temp);
                setupFieldForSecondPage(R.string.angle);
                setupFieldForSecondPage(R.string.roll);
                setupFieldForSecondPage(R.string.ride_time);
                setupFieldForSecondPage(R.string.riding_time);
                setupFieldForSecondPage(R.string.distance);
                setupFieldForSecondPage(R.string.wheel_distance);
                setupFieldForSecondPage(R.string.user_distance);
                setupFieldForSecondPage(R.string.total_distance);
                setupFieldForSecondPage(R.string.voltage);
                setupFieldForSecondPage(R.string.voltage_sag);
                setupFieldForSecondPage(R.string.current);
                setupFieldForSecondPage(R.string.dynamic_current_limit);
                setupFieldForSecondPage(R.string.power);
                setupFieldForSecondPage(R.string.motor_power);
                setupFieldForSecondPage(R.string.mode);
                setupFieldForSecondPage(R.string.model);
                setupFieldForSecondPage(R.string.version);
                setupFieldForSecondPage(R.string.serial_number);
                break;

            case INMOTION:
                setupFieldForSecondPage(R.string.speed);
                setupFieldForSecondPage(R.string.top_speed);
                setupFieldForSecondPage(R.string.average_speed);
                setupFieldForSecondPage(R.string.average_riding_speed);
                setupFieldForSecondPage(R.string.battery);
                setupFieldForSecondPage(R.string.temperature);
                setupFieldForSecondPage(R.string.temperature2);
                setupFieldForSecondPage(R.string.angle);
                setupFieldForSecondPage(R.string.roll);
                setupFieldForSecondPage(R.string.ride_time);
                setupFieldForSecondPage(R.string.riding_time);
                setupFieldForSecondPage(R.string.distance);
                setupFieldForSecondPage(R.string.wheel_distance);
                setupFieldForSecondPage(R.string.user_distance);
                setupFieldForSecondPage(R.string.total_distance);
                setupFieldForSecondPage(R.string.voltage);
                setupFieldForSecondPage(R.string.voltage_sag);
                setupFieldForSecondPage(R.string.current);
                setupFieldForSecondPage(R.string.power);
                setupFieldForSecondPage(R.string.mode);
                setupFieldForSecondPage(R.string.model);
                setupFieldForSecondPage(R.string.version);
                setupFieldForSecondPage(R.string.serial_number);
                break;

            case NINEBOT_Z:
                setupFieldForSecondPage(R.string.speed);
                setupFieldForSecondPage(R.string.top_speed);
                setupFieldForSecondPage(R.string.average_speed);
                setupFieldForSecondPage(R.string.average_riding_speed);
                setupFieldForSecondPage(R.string.battery);
                setupFieldForSecondPage(R.string.temperature);
                setupFieldForSecondPage(R.string.ride_time);
                setupFieldForSecondPage(R.string.riding_time);
                setupFieldForSecondPage(R.string.distance);
                setupFieldForSecondPage(R.string.user_distance);
                setupFieldForSecondPage(R.string.total_distance);
                setupFieldForSecondPage(R.string.voltage);
                setupFieldForSecondPage(R.string.voltage_sag);
                setupFieldForSecondPage(R.string.current);
                setupFieldForSecondPage(R.string.power);
                setupFieldForSecondPage(R.string.model);
                setupFieldForSecondPage(R.string.version);
                setupFieldForSecondPage(R.string.serial_number);

                tvBms1Cell15.setVisibility(View.GONE);
                tvBms1Cell16.setVisibility(View.GONE);
                tvBms2Cell15.setVisibility(View.GONE);
                tvBms2Cell16.setVisibility(View.GONE);
                tvTitleBms1Cell15.setVisibility(View.GONE);
                tvTitleBms1Cell16.setVisibility(View.GONE);
                tvTitleBms2Cell15.setVisibility(View.GONE);
                tvTitleBms2Cell16.setVisibility(View.GONE);
                break;

            case NINEBOT:
                setupFieldForSecondPage(R.string.speed);
                setupFieldForSecondPage(R.string.top_speed);
                setupFieldForSecondPage(R.string.average_speed);
                setupFieldForSecondPage(R.string.average_riding_speed);
                setupFieldForSecondPage(R.string.battery);
                setupFieldForSecondPage(R.string.temperature);
                setupFieldForSecondPage(R.string.ride_time);
                setupFieldForSecondPage(R.string.riding_time);
                setupFieldForSecondPage(R.string.distance);
                setupFieldForSecondPage(R.string.user_distance);
                setupFieldForSecondPage(R.string.total_distance);
                setupFieldForSecondPage(R.string.voltage);
                setupFieldForSecondPage(R.string.voltage_sag);
                setupFieldForSecondPage(R.string.current);
                setupFieldForSecondPage(R.string.power);
                setupFieldForSecondPage(R.string.model);
                setupFieldForSecondPage(R.string.version);
                setupFieldForSecondPage(R.string.serial_number);
                break;
        }
        createSecondPage();
    }

    @SuppressLint("NonConstantResourceId")
    private void updateScreen(boolean updateGraph) {
        WheelData data = WheelData.getInstance();
        switch (viewPagerPage) {
            case R.id.page_main: // GUI View
                data.setBmsView(false);
                wheelView.setSpeed(data.getSpeed());
                wheelView.setBattery(data.getBatteryLevel());
                wheelView.setTemperature(data.getTemperature());
                wheelView.setRideTime(data.getRidingTimeString());
                wheelView.setTopSpeed(data.getTopSpeedDouble());
                wheelView.setDistance(data.getDistanceDouble());
                wheelView.setTotalDistance(data.getTotalDistanceDouble());
                wheelView.setVoltage(data.getVoltageDouble());
                wheelView.setCurrent(data.getCurrentDouble());
                wheelView.setAverageSpeed(data.getAverageRidingSpeedDouble());
                wheelView.setMaxPwm(data.getMaxPwm());
                wheelView.setMaxTemperature(data.getMaxTemp());
                wheelView.setPwm(data.getCalculatedPwm());
                wheelView.redrawTextBoxes();

                String profileName = WheelLog.AppConfig.getProfileName();
                if (profileName == null || profileName.trim().equals(""))
                    wheelView.setWheelModel(data.getModel().equals("") ? data.getName() : data.getModel());
                else
                    wheelView.setWheelModel(profileName);

                break;
            case R.id.page_params_list: // Text View
                WheelData.getInstance().setBmsView(false);

                if (use_mph) {
                    updateFieldForSecondPage(R.string.speed, String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getSpeedDouble())));
                    updateFieldForSecondPage(R.string.top_speed, String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getTopSpeedDouble())));
                    updateFieldForSecondPage(R.string.average_speed, String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getAverageSpeedDouble())));
                    updateFieldForSecondPage(R.string.average_riding_speed, String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getAverageRidingSpeedDouble())));
                    updateFieldForSecondPage(R.string.dynamic_speed_limit, String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getSpeedLimit())));
                    updateFieldForSecondPage(R.string.distance, String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getDistanceDouble())));
                    updateFieldForSecondPage(R.string.wheel_distance, String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getWheelDistanceDouble())));
                    updateFieldForSecondPage(R.string.user_distance, String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getUserDistanceDouble())));
                    updateFieldForSecondPage(R.string.total_distance, String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getTotalDistanceDouble())));
                } else {
                    updateFieldForSecondPage(R.string.speed, String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getSpeedDouble()));
                    updateFieldForSecondPage(R.string.top_speed, String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getTopSpeedDouble()));
                    updateFieldForSecondPage(R.string.average_speed, String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getAverageSpeedDouble()));
                    updateFieldForSecondPage(R.string.average_riding_speed, String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getAverageRidingSpeedDouble()));
                    updateFieldForSecondPage(R.string.dynamic_speed_limit, String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getSpeedLimit()));
                    updateFieldForSecondPage(R.string.distance, String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getDistanceDouble()));
                    updateFieldForSecondPage(R.string.wheel_distance, String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getWheelDistanceDouble()));
                    updateFieldForSecondPage(R.string.user_distance, String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getUserDistanceDouble()));
                    updateFieldForSecondPage(R.string.total_distance, String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getTotalDistanceDouble()));
                }

                updateFieldForSecondPage(R.string.voltage, String.format(Locale.US, "%.2f " + getString(R.string.volt), WheelData.getInstance().getVoltageDouble()));
                updateFieldForSecondPage(R.string.voltage_sag, String.format(Locale.US, "%.2f " + getString(R.string.volt), WheelData.getInstance().getVoltageSagDouble()));
                updateFieldForSecondPage(R.string.temperature, String.format(Locale.US, "%d°C", WheelData.getInstance().getTemperature()));
                updateFieldForSecondPage(R.string.temperature2, String.format(Locale.US, "%d°C", WheelData.getInstance().getTemperature2()));
                updateFieldForSecondPage(R.string.cpu_temp, String.format(Locale.US, "%d°C", WheelData.getInstance().getCpuTemp()));
                updateFieldForSecondPage(R.string.imu_temp, String.format(Locale.US, "%d°C", WheelData.getInstance().getImuTemp()));
                updateFieldForSecondPage(R.string.angle, String.format(Locale.US, "%.2f°", WheelData.getInstance().getAngle()));
                updateFieldForSecondPage(R.string.roll, String.format(Locale.US, "%.2f°", WheelData.getInstance().getRoll()));
                updateFieldForSecondPage(R.string.current, String.format(Locale.US, "%.2f " + getString(R.string.amp), WheelData.getInstance().getCurrentDouble()));
                updateFieldForSecondPage(R.string.dynamic_current_limit, String.format(Locale.US, "%.2f " + getString(R.string.amp), WheelData.getInstance().getCurrentLimit()));
                updateFieldForSecondPage(R.string.torque, String.format(Locale.US, "%.2f " + getString(R.string.newton), WheelData.getInstance().getTorque()));
                updateFieldForSecondPage(R.string.power, String.format(Locale.US, "%.2f " + getString(R.string.watt), WheelData.getInstance().getPowerDouble()));
                updateFieldForSecondPage(R.string.motor_power, String.format(Locale.US, "%.2f " + getString(R.string.watt), WheelData.getInstance().getMotorPower()));
                updateFieldForSecondPage(R.string.battery, String.format(Locale.US, "%d%%", WheelData.getInstance().getBatteryLevel()));
                updateFieldForSecondPage(R.string.fan_status, WheelData.getInstance().getFanStatus() == 0 ? getString(R.string.off) : getString(R.string.on));
                updateFieldForSecondPage(R.string.charging_status, WheelData.getInstance().getChargingStatus() == 0 ? getString(R.string.discharging) : getString(R.string.charging));
                updateFieldForSecondPage(R.string.version, String.format(Locale.US, "%s", WheelData.getInstance().getVersion()));
                updateFieldForSecondPage(R.string.output, String.format(Locale.US, "%d%%", WheelData.getInstance().getOutput()));
                updateFieldForSecondPage(R.string.cpuload, String.format(Locale.US, "%d%%", WheelData.getInstance().getCpuLoad()));
                updateFieldForSecondPage(R.string.name, WheelData.getInstance().getName());
                updateFieldForSecondPage(R.string.model, WheelData.getInstance().getModel());
                updateFieldForSecondPage(R.string.serial_number, WheelData.getInstance().getSerial());
                updateFieldForSecondPage(R.string.ride_time, WheelData.getInstance().getRideTimeString());
                updateFieldForSecondPage(R.string.riding_time, WheelData.getInstance().getRidingTimeString());
                updateFieldForSecondPage(R.string.mode, WheelData.getInstance().getModeStr());
                updateFieldForSecondPage(R.string.charging, WheelData.getInstance().getChargeTime());
                updateSecondPage();
                break;
            case R.id.page_graph: // Graph  View
                WheelData.getInstance().setBmsView(false);
                if (updateGraph) {
                    xAxis_labels = WheelData.getInstance().getXAxis();

                    if (xAxis_labels.size() > 0) {

                        LineDataSet dataSetSpeed;
                        LineDataSet dataSetCurrent;

                        if (chart1.getData() == null) {
                            dataSetSpeed = new LineDataSet(null, getString(R.string.speed_axis));
                            dataSetCurrent = new LineDataSet(null, getString(R.string.current_axis));
                            dataSetSpeed.setLineWidth(2);
                            dataSetCurrent.setLineWidth(2);
                            dataSetSpeed.setAxisDependency(YAxis.AxisDependency.LEFT);
                            dataSetCurrent.setAxisDependency(YAxis.AxisDependency.RIGHT);
                            dataSetSpeed.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                            dataSetCurrent.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                            dataSetSpeed.setColor(getResources().getColor(android.R.color.white));
                            dataSetCurrent.setColor(getResources().getColor(R.color.accent));
                            dataSetSpeed.setDrawCircles(false);
                            dataSetCurrent.setDrawCircles(false);
                            dataSetSpeed.setDrawValues(false);
                            dataSetCurrent.setDrawValues(false);
                            LineData chart1_lineData = new LineData();
                            chart1_lineData.addDataSet(dataSetCurrent);
                            chart1_lineData.addDataSet(dataSetSpeed);
                            chart1.setData(chart1_lineData);
                            findViewById(R.id.leftAxisLabel).setVisibility(View.VISIBLE);
                            findViewById(R.id.rightAxisLabel).setVisibility(View.VISIBLE);
                        } else {
                            dataSetSpeed = (LineDataSet) chart1.getData().getDataSetByLabel(getString(R.string.speed_axis), true);
                            dataSetCurrent = (LineDataSet) chart1.getData().getDataSetByLabel(getString(R.string.current_axis), true);
                        }

                        dataSetSpeed.clear();
                        dataSetCurrent.clear();

                        ArrayList<Float> currentAxis = new ArrayList<>(WheelData.getInstance().getCurrentAxis());
                        ArrayList<Float> speedAxis = new ArrayList<>(WheelData.getInstance().getSpeedAxis());

                        for (Float d : currentAxis) {
                            float value = 0;
                            if (d != null)
                                value = d;

                            dataSetCurrent.addEntry(new Entry(dataSetCurrent.getEntryCount(), value));
                        }

                        for (Float d : speedAxis) {
                            float value = 0;

                            if (d != null)
                                value = d;

                            if (use_mph)
                                dataSetSpeed.addEntry(new Entry(dataSetSpeed.getEntryCount(), kmToMiles(value)));
                            else
                                dataSetSpeed.addEntry(new Entry(dataSetSpeed.getEntryCount(), value));
                        }

                        dataSetCurrent.notifyDataSetChanged();
                        dataSetSpeed.notifyDataSetChanged();
                        chart1.getData().notifyDataChanged();
                        chart1.notifyDataSetChanged();
                        chart1.invalidate();
                        break;
                    }
                }
                break;
            case R.id.page_smart_bms: //BMS view
                data.setBmsView(true);
                tvBms1Sn.setText(data.getBms1().getSerialNumber());
                tvBms1Fw.setText(data.getBms1().getVersionNumber());
                tvBms1FactoryCap.setText(String.format(Locale.US, "%d mAh", data.getBms1().getFactoryCap()));
                tvBms1ActualCap.setText(String.format(Locale.US, "%d mAh", data.getBms1().getActualCap()));
                tvBms1Cycles.setText(String.format(Locale.US, "%d", data.getBms1().getFullCycles()));
                tvBms1ChrgCount.setText(String.format(Locale.US, "%d", data.getBms1().getChargeCount()));
                tvBms1MfgDate.setText(data.getBms1().getMfgDateStr());
                tvBms1Status.setText(String.format(Locale.US, "%d", data.getBms1().getStatus()));
                tvBms1RemCap.setText(String.format(Locale.US, "%d mAh", data.getBms1().getRemCap()));
                tvBms1RemPerc.setText(String.format(Locale.US, "%d %%", data.getBms1().getRemPerc()));
                tvBms1Current.setText(String.format(Locale.US, "%.2f A", data.getBms1().getCurrent()));
                tvBms1Voltage.setText(String.format(Locale.US, "%.2f V", data.getBms1().getVoltage()));
                tvBms1Temp1.setText(String.format(Locale.US, "%d°C", data.getBms1().getTemp1()));
                tvBms1Temp2.setText(String.format(Locale.US, "%d°C", data.getBms1().getTemp2()));
                tvBms1Health.setText(String.format(Locale.US, "%d %%", data.getBms1().getHealth()));
                int balanceMap = data.getBms1().getBalanceMap();
                String bal = "";
                if (((balanceMap) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell1.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[0], bal));
                if (((balanceMap >> 1) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell2.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[1], bal));
                if (((balanceMap >> 2) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell3.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[2], bal));
                if (((balanceMap >> 3) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell4.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[3], bal));
                if (((balanceMap >> 4) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell5.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[4], bal));
                if (((balanceMap >> 5) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell6.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[5], bal));
                if (((balanceMap >> 6) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell7.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[6], bal));
                if (((balanceMap >> 7) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell8.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[7], bal));
                if (((balanceMap >> 8) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell9.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[8], bal));
                if (((balanceMap >> 9) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell10.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[9], bal));
                if (((balanceMap >> 10) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell11.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[10], bal));
                if (((balanceMap >> 11) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell12.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[11], bal));
                if (((balanceMap >> 12) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell13.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[12], bal));
                if (((balanceMap >> 13) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell14.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[13], bal));
                if (data.getBms1().getCells()[14] == 0.0) {
                    tvBms1Cell15.setVisibility(View.GONE);
                    tvTitleBms1Cell15.setVisibility(View.GONE);
                } else {
                    tvBms1Cell15.setVisibility(View.VISIBLE);
                    tvTitleBms1Cell15.setVisibility(View.VISIBLE);
                }
                if (data.getBms1().getCells()[15] == 0.0) {
                    tvBms1Cell16.setVisibility(View.GONE);
                    tvTitleBms1Cell16.setVisibility(View.GONE);
                } else {
                    tvBms1Cell16.setVisibility(View.VISIBLE);
                    tvTitleBms1Cell16.setVisibility(View.VISIBLE);
                }
                if (((balanceMap >> 14) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell15.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[14], bal));
                if (((balanceMap >> 15) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell16.setText(String.format(Locale.US, "%.3f V %s", data.getBms1().getCells()[15], bal));
                tvBms2Sn.setText(data.getBms2().getSerialNumber());
                tvBms2Fw.setText(data.getBms2().getVersionNumber());
                tvBms2FactoryCap.setText(String.format(Locale.US, "%d mAh", data.getBms2().getFactoryCap()));
                tvBms2ActualCap.setText(String.format(Locale.US, "%d mAh", data.getBms2().getActualCap()));
                tvBms2Cycles.setText(String.format(Locale.US, "%d", data.getBms2().getFullCycles()));
                tvBms2ChrgCount.setText(String.format(Locale.US, "%d", data.getBms2().getChargeCount()));
                tvBms2MfgDate.setText(data.getBms2().getMfgDateStr());
                tvBms2Status.setText(String.format(Locale.US, "%d", data.getBms2().getStatus()));
                tvBms2RemCap.setText(String.format(Locale.US, "%d mAh", data.getBms2().getRemCap()));
                tvBms2RemPerc.setText(String.format(Locale.US, "%d %%", data.getBms2().getRemPerc()));
                tvBms2Current.setText(String.format(Locale.US, "%.2f A", data.getBms2().getCurrent()));
                tvBms2Voltage.setText(String.format(Locale.US, "%.2f V", data.getBms2().getVoltage()));
                tvBms2Temp1.setText(String.format(Locale.US, "%d°C", data.getBms2().getTemp1()));
                tvBms2Temp2.setText(String.format(Locale.US, "%d°C", data.getBms2().getTemp2()));
                tvBms2Health.setText(String.format(Locale.US, "%d %%", data.getBms2().getHealth()));
                balanceMap = data.getBms2().getBalanceMap();
                if (((balanceMap) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell1.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[0], bal));
                if (((balanceMap >> 1) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell2.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[1], bal));
                if (((balanceMap >> 2) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell3.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[2], bal));
                if (((balanceMap >> 3) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell4.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[3], bal));
                if (((balanceMap >> 4) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell5.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[4], bal));
                if (((balanceMap >> 5) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell6.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[5], bal));
                if (((balanceMap >> 6) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell7.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[6], bal));
                if (((balanceMap >> 7) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell8.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[7], bal));
                if (((balanceMap >> 8) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell9.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[8], bal));
                if (((balanceMap >> 9) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell10.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[9], bal));
                if (((balanceMap >> 10) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell11.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[10], bal));
                if (((balanceMap >> 11) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell12.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[11], bal));
                if (((balanceMap >> 12) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell13.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[12], bal));
                if (((balanceMap >> 13) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell14.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[13], bal));
                if (data.getBms2().getCells()[14] == 0.0) {
                    tvBms2Cell15.setVisibility(View.GONE);
                    tvTitleBms2Cell15.setVisibility(View.GONE);
                } else {
                    tvBms2Cell15.setVisibility(View.VISIBLE);
                    tvTitleBms2Cell15.setVisibility(View.VISIBLE);
                }
                if (data.getBms2().getCells()[15] == 0.0) {
                    tvBms2Cell16.setVisibility(View.GONE);
                    tvTitleBms2Cell16.setVisibility(View.GONE);
                } else {
                    tvBms2Cell16.setVisibility(View.VISIBLE);
                    tvTitleBms2Cell16.setVisibility(View.VISIBLE);
                }
                if (((balanceMap >> 14) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell15.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[14], bal));
                if (((balanceMap >> 15) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell16.setText(String.format(Locale.US, "%.3f V %s", data.getBms2().getCells()[15], bal));

        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateScreen(true);
    }

    private void createPager()
    {
        // add pages into main view
        ViewPager pager = findViewById(R.id.pager);
        LayoutInflater i = getLayoutInflater();
        i.inflate(R.layout.main_view_main, pager);
        i.inflate(R.layout.main_view_params_list, pager);
        i.inflate(R.layout.main_view_graph, pager);
        i.inflate(R.layout.main_view_smart_bms, pager); // TODO: inflate smart bms page only if needed (after detect wheel)

        // set page adapter and show 3 pages
        pagerAdapter = new ViewPageAdapter(this);
        pagerAdapter.showPage(R.id.page_main);
        pagerAdapter.showPage(R.id.page_params_list);
        pagerAdapter.showPage(R.id.page_graph);
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(4);

        loadPreferences(R.string.show_page_events); // аццкий костыль

        LinePageIndicator titleIndicator = findViewById(R.id.indicator);
        pagerAdapter.setPageIndicator(titleIndicator);
        titleIndicator.setViewPager(pager);
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                viewPagerPage = pagerAdapter.getPageIdByPosition(position);
                updateScreen(true);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (onDestroyProcess)
            android.os.Process.killProcess(android.os.Process.myPid());

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

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_frame, getPreferencesFragment(), Constants.PREFERENCES_FRAGMENT_TAG)
                .commit();

        createPager();

        mDeviceAddress = WheelLog.AppConfig.getLastMac();
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        wheelView = (WheelView) findViewById(R.id.wheelView);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        wheelView.setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector gestureDetector = new GestureDetector(
                    MainActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    BaseAdapter adapter = WheelData.getInstance().getAdapter();
                    if (adapter != null) {
                        adapter.switchFlashlight();
                    }
                    return super.onDoubleTap(e);
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (WheelLog.AppConfig.getUseBeepOnSingleTap()) {
                        // TODO: заменить на SomeUtil.playSound(getApplicationContext(), R.raw.beep);
                        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.beep);
                        mp.start();
                        mp.setOnCompletionListener(MediaPlayer::release);
                        return true;
                    }
                    return super.onSingleTapConfirmed(e);
                }
            });

            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

        tvBms1Sn = (TextView) findViewById(R.id.tvBms1Sn);
        tvBms2Sn = (TextView) findViewById(R.id.tvBms2Sn);
        tvBms1Fw = (TextView) findViewById(R.id.tvBms1Fw);
        tvBms2Fw = (TextView) findViewById(R.id.tvBms2Fw);
        tvBms1FactoryCap = (TextView) findViewById(R.id.tvBms1FactoryCap);
        tvBms2FactoryCap = (TextView) findViewById(R.id.tvBms2FactoryCap);
        tvBms1ActualCap = (TextView) findViewById(R.id.tvBms1ActualCap);
        tvBms2ActualCap = (TextView) findViewById(R.id.tvBms2ActualCap);
        tvBms1Cycles = (TextView) findViewById(R.id.tvBms1Cycles);
        tvBms2Cycles = (TextView) findViewById(R.id.tvBms2Cycles);
        tvBms1ChrgCount = (TextView) findViewById(R.id.tvBms1ChrgCount);
        tvBms2ChrgCount = (TextView) findViewById(R.id.tvBms2ChrgCount);
        tvBms1MfgDate = (TextView) findViewById(R.id.tvBms1MfgDate);
        tvBms2MfgDate = (TextView) findViewById(R.id.tvBms2MfgDate);
        tvBms1Status = (TextView) findViewById(R.id.tvBms1Status);
        tvBms2Status = (TextView) findViewById(R.id.tvBms2Status);
        tvBms1RemCap = (TextView) findViewById(R.id.tvBms1RemCap);
        tvBms2RemCap = (TextView) findViewById(R.id.tvBms2RemCap);
        tvBms1RemPerc = (TextView) findViewById(R.id.tvBms1RemPerc);
        tvBms2RemPerc = (TextView) findViewById(R.id.tvBms2RemPerc);
        tvBms1Current = (TextView) findViewById(R.id.tvBms1Current);
        tvBms2Current = (TextView) findViewById(R.id.tvBms2Current);
        tvBms1Voltage = (TextView) findViewById(R.id.tvBms1Voltage);
        tvBms2Voltage = (TextView) findViewById(R.id.tvBms2Voltage);
        tvBms1Temp1 = (TextView) findViewById(R.id.tvBms1Temp1);
        tvBms2Temp1 = (TextView) findViewById(R.id.tvBms2Temp1);
        tvBms1Temp2 = (TextView) findViewById(R.id.tvBms1Temp2);
        tvBms2Temp2 = (TextView) findViewById(R.id.tvBms2Temp2);
        tvBms1Health = (TextView) findViewById(R.id.tvBms1Health);
        tvBms2Health = (TextView) findViewById(R.id.tvBms2Health);
        tvBms1Cell1 = (TextView) findViewById(R.id.tvBms1Cell1);
        tvBms2Cell1 = (TextView) findViewById(R.id.tvBms2Cell1);
        tvBms1Cell2 = (TextView) findViewById(R.id.tvBms1Cell2);
        tvBms2Cell2 = (TextView) findViewById(R.id.tvBms2Cell2);
        tvBms1Cell3 = (TextView) findViewById(R.id.tvBms1Cell3);
        tvBms2Cell3 = (TextView) findViewById(R.id.tvBms2Cell3);
        tvBms1Cell4 = (TextView) findViewById(R.id.tvBms1Cell4);
        tvBms2Cell4 = (TextView) findViewById(R.id.tvBms2Cell4);
        tvBms1Cell5 = (TextView) findViewById(R.id.tvBms1Cell5);
        tvBms2Cell5 = (TextView) findViewById(R.id.tvBms2Cell5);
        tvBms1Cell6 = (TextView) findViewById(R.id.tvBms1Cell6);
        tvBms2Cell6 = (TextView) findViewById(R.id.tvBms2Cell6);
        tvBms1Cell7 = (TextView) findViewById(R.id.tvBms1Cell7);
        tvBms2Cell7 = (TextView) findViewById(R.id.tvBms2Cell7);
        tvBms1Cell8 = (TextView) findViewById(R.id.tvBms1Cell8);
        tvBms2Cell8 = (TextView) findViewById(R.id.tvBms2Cell8);
        tvBms1Cell9 = (TextView) findViewById(R.id.tvBms1Cell9);
        tvBms2Cell9 = (TextView) findViewById(R.id.tvBms2Cell9);
        tvBms1Cell10 = (TextView) findViewById(R.id.tvBms1Cell10);
        tvBms2Cell10 = (TextView) findViewById(R.id.tvBms2Cell10);
        tvBms1Cell11 = (TextView) findViewById(R.id.tvBms1Cell11);
        tvBms2Cell11 = (TextView) findViewById(R.id.tvBms2Cell11);
        tvBms1Cell12 = (TextView) findViewById(R.id.tvBms1Cell12);
        tvBms2Cell12 = (TextView) findViewById(R.id.tvBms2Cell12);
        tvBms1Cell13 = (TextView) findViewById(R.id.tvBms1Cell13);
        tvBms2Cell13 = (TextView) findViewById(R.id.tvBms2Cell13);
        tvBms1Cell14 = (TextView) findViewById(R.id.tvBms1Cell14);
        tvBms2Cell14 = (TextView) findViewById(R.id.tvBms2Cell14);
        tvTitleBms1Cell15 = (TextView) findViewById(R.id.tvTitleBms1Cell15);
        tvBms1Cell15 = (TextView) findViewById(R.id.tvBms1Cell15);
        tvTitleBms2Cell15 = (TextView) findViewById(R.id.tvTitleBms2Cell15);
        tvBms2Cell15 = (TextView) findViewById(R.id.tvBms2Cell15);
        tvTitleBms1Cell16 = (TextView) findViewById(R.id.tvTitleBms1Cell16);
        tvBms1Cell16 = (TextView) findViewById(R.id.tvBms1Cell16);
        tvTitleBms2Cell16 = (TextView) findViewById(R.id.tvTitleBms2Cell16);
        tvBms2Cell16 = (TextView) findViewById(R.id.tvBms2Cell16);


        mDrawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                getPreferencesFragment().showMainMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        Typeface typefacePrime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? getResources().getFont(R.font.prime)
                : ResourcesCompat.getFont(this, R.font.prime);
        TextClock textClock = (TextClock) findViewById(R.id.textClock);
        textClock.setTypeface(typefacePrime);

        chart1 = (LineChart) findViewById(R.id.chart);
        chart1.setDrawGridBackground(false);
        chart1.getDescription().setEnabled(false);
        chart1.setHardwareAccelerationEnabled(true);
        chart1.setHighlightPerTapEnabled(false);
        chart1.setHighlightPerDragEnabled(false);
        chart1.getLegend().setTextColor(getResources().getColor(android.R.color.white));
        chart1.setNoDataText(getString(R.string.no_chart_data));
        chart1.setNoDataTextColor(getResources().getColor(android.R.color.white));

        YAxis leftAxis = chart1.getAxisLeft();
        YAxis rightAxis = chart1.getAxisRight();
        leftAxis.setAxisMinValue(0f);
        rightAxis.setAxisMinValue(0f);
        leftAxis.setDrawGridLines(false);
        rightAxis.setDrawGridLines(false);
        leftAxis.setTextColor(getResources().getColor(android.R.color.white));
        rightAxis.setTextColor(getResources().getColor(android.R.color.white));

        XAxis xAxis = chart1.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(android.R.color.white));
        xAxis.setValueFormatter(chartAxisValueFormatter);

        loadPreferences();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!mBluetoothAdapter.isEnabled())
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RESULT_REQUEST_ENABLE_BT);
        } else {
            startBluetoothService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBluetoothLeService != null &&
                mConnectionState != mBluetoothLeService.getConnectionState())
            setConnectionState(mBluetoothLeService.getConnectionState());

        if (WheelData.getInstance().getWheelType() != WHEEL_TYPE.Unknown)
            configureDisplay(WheelData.getInstance().getWheelType());

        registerReceiver(mBluetoothUpdateReceiver, makeIntentFilter());
        updateScreen(true);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setMenuIconStates();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothUpdateReceiver);
    }

    private static Boolean onDestroyProcess = false;

    @Override
    protected void onDestroy() {
        stopPebbleService();
        stopGarminConnectIQ();
        stopLoggingService();
        WheelData.getInstance().full_reset();
        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection);
            stopService(new Intent(getApplicationContext(), BluetoothLeService.class));
            mBluetoothLeService = null;
        }
        super.onDestroy();
        onDestroyProcess = true;
        new CountDownTimer(60000, 100) {

            @Override
            public void onTick(long millisUntilFinished) {
                // do something after 1s
            }

            @Override
            public void onFinish() {

                android.os.Process.killProcess(android.os.Process.myPid());
                // do something end times 5s
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
        miLogging = mMenu.findItem(R.id.miLogging);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.miSearch:
                MainActivityPermissionsDispatcher.startScanActivityWithCheck(this);
                return true;
            case R.id.miWheel:
                toggleConnectToWheel();
                return true;
            case R.id.miLogging:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    toggleLoggingService();
                } else {
                    MainActivityPermissionsDispatcher.toggleLoggingServiceLegacyWithCheck(this);
                }
                return true;
            case R.id.miWatch:
                togglePebbleService();
                if (WheelLog.AppConfig.getGarminConnectIqEnable())
                    toggleGarminConnectIQ();
                else
                    stopGarminConnectIQ();
                return true;
            case R.id.miSettings:
                mDrawer.openDrawer(GravityCompat.START, true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        View settings_layout = findViewById(R.id.settings_layout);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (mDrawer.isDrawerOpen(settings_layout)) {
                    mDrawer.closeDrawers();
                } else {
                    mDrawer.openDrawer(GravityCompat.START, true);
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (mDrawer.isDrawerOpen(settings_layout)) {
                    if (getPreferencesFragment().isMainMenu()) {
                        mDrawer.closeDrawer(GravityCompat.START, true);
                    } else {
                        getPreferencesFragment().showMainMenu();
                    }
                } else {
                    if (doubleBackToExitPressedOnce) {
                        finish();
                        return true;
                    }

                    doubleBackToExitPressedOnce = true;
                    showSnackBar(R.string.back_to_exit);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doubleBackToExitPressedOnce = false;
                        }
                    }, 2000);
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void loadPreferences() {
        loadPreferences(-1);
    }

    private void loadPreferences(int settingsKey) {
        switch (settingsKey) {
            case R.string.auto_log:
                if (WheelLog.AppConfig.getAutoLog() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    MainActivityPermissionsDispatcher.acquireStoragePermissionWithCheck(this);
                break;
            case R.string.log_location_data:
                if (WheelLog.AppConfig.getLogLocationData())
                    MainActivityPermissionsDispatcher.acquireLocationPermissionWithCheck(this);
                break;
            case R.string.auto_upload_ec:
                if (WheelLog.AppConfig.getAutoUploadEc()) {
                    if (ElectroClub.getInstance().getUserToken() == null)
                        startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), RESULT_AUTH_REQUEST);
                    else
                        ElectroClub.getInstance().getAndSelectGarageByMacOrPrimary(mDeviceAddress, s -> null); // TODO check user token
                } else {
                    // TODO: need to implement a logout
                    // logout after uncheck
                    ElectroClub.getInstance().setUserToken(null);
                    ElectroClub.getInstance().setUserId(null);
                    WheelLog.AppConfig.setEcToken(null);
                }
                break;
            case R.string.show_page_events:
                if (WheelLog.AppConfig.getPageEvents()) {
                    if (findViewById(R.id.page_events) == null) {
                        ViewPager pager = findViewById(R.id.pager);
                        getLayoutInflater().inflate(R.layout.main_view_events, pager);
                    }
                    pagerAdapter.showPage(R.id.page_events);
                    eventsTextView = findViewById(R.id.events_textbox);
                } else {
                    pagerAdapter.hidePage(R.id.page_events);
                    eventsTextView = null;
                }
                return;
        }

        String viewBlocksString = WheelLog.AppConfig.getViewBlocksString();
        String[] viewBlocks;
        if (viewBlocksString == null) {
            viewBlocks = new String[]{
                    getString(R.string.voltage),
                    getString(R.string.average_riding_speed),
                    getString(R.string.riding_time),
                    getString(R.string.top_speed),
                    getString(R.string.distance),
                    getString(R.string.total)
            };
        } else {
            viewBlocks = viewBlocksString.split(MultiSelectPreference.getSeparator());
        }

        wheelView.updateViewBlocksVisibility(viewBlocks);
        wheelView.invalidate();
        updateScreen(true);
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void acquireStoragePermission() {
    }

    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION})
    void acquireLocationPermission() {
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void storagePermissionDenied() {
        WheelLog.AppConfig.setAutoLog(false);
        getPreferencesFragment().refreshVolatileSettings();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void locationPermissionDenied() {
        WheelLog.AppConfig.setLogLocationData(false);
        getPreferencesFragment().refreshVolatileSettings();
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
            snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                }
            });
        }
        snackbar.setDuration(timeout);
        snackbar.setText(msg);
        snackbar.show();
        logEvent(msg);
    }

    private void hideSnackBar() {
        if (snackbar == null)
            return;

        snackbar.dismiss();
    }

    private void logEvent(String message) {
        if (eventsTextView == null) {
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String formattedMessage = String.format("[%s] %s\n", formatter.format(new Date()), message);
        if (eventsCurrentCount < eventsMaxCount) {
            eventsTextView.append(formattedMessage);
            eventsCurrentCount++;
        } else {
            eventsTextView.setText(String.format("%s%s", StringUtil.deleteFirstSentence(eventsTextView.getText()), formattedMessage));
        }
    }

    private void stopLoggingService() {
        if (LoggingService.isInstanceCreated())
            toggleLoggingService();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void toggleLoggingServiceLegacy() {
        toggleLoggingService();
    }

    void toggleLoggingService() {
        Intent dataLoggerServiceIntent = new Intent(getApplicationContext(), LoggingService.class);

        if (LoggingService.isInstanceCreated())
            stopService(dataLoggerServiceIntent);
        else if (mConnectionState == BluetoothLeService.STATE_CONNECTED)
            ContextCompat.startForegroundService(this, dataLoggerServiceIntent);
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

    private void startBluetoothService() {
        Intent bluetoothServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        ContextCompat.startForegroundService(this, bluetoothServiceIntent);
        bindService(bluetoothServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void toggleConnectToWheel() {
        sendBroadcast(new Intent(Constants.ACTION_REQUEST_CONNECTION_TOGGLE));
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
                if (resultCode == RESULT_OK) {
                    mDeviceAddress = data.getStringExtra("MAC");
                    Timber.i("Device selected = %s", mDeviceAddress);
                    String mDeviceName = data.getStringExtra("NAME");
                    Timber.i("Device selected = %s", mDeviceName);
                    mBluetoothLeService.setDeviceAddress(mDeviceAddress);
                    WheelData.getInstance().full_reset();
                    WheelData.getInstance().setBtName(mDeviceName);
                    updateScreen(true);
                    setMenuIconStates();
                    mBluetoothLeService.close();
                    toggleConnectToWheel();
                    if (WheelLog.AppConfig.getAutoUploadEc() && WheelLog.AppConfig.getEcToken() != null) {
                        ElectroClub.getInstance().getAndSelectGarageByMacOrPrimary(
                                mDeviceAddress,
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
            case RESULT_AUTH_REQUEST:
                if (resultCode == RESULT_OK) {
                    WheelLog.AppConfig.setEcToken(ElectroClub.getInstance().getUserToken());
                    WheelLog.AppConfig.setEcUserId(ElectroClub.getInstance().getUserId());
                    ElectroClub.getInstance().getAndSelectGarageByMacOrPrimary(mDeviceAddress, s -> null);
                } else {
                    WheelLog.AppConfig.setAutoUploadEc(false);
                    WheelLog.AppConfig.setEcToken(null);
                    WheelLog.AppConfig.setEcUserId(null);
                    getPreferencesFragment().refreshVolatileSettings();
                }
                break;
        }
    }

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PREFERENCE_CHANGED);
        intentFilter.addAction(Constants.ACTION_PREFERENCE_RESET);
        intentFilter.addAction(Constants.ACTION_WHEEL_SETTING_CHANGED);
        intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_RECOGNIZED);
        intentFilter.addAction(Constants.ACTION_ALARM_TRIGGERED);
        intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_CHANGED);
        return intentFilter;
    }

    IAxisValueFormatter chartAxisValueFormatter = new IAxisValueFormatter() {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            if (value < xAxis_labels.size())
                return xAxis_labels.get((int) value);
            else
                return "";
        }

        // we don't draw numbers, so no decimal digits needed
        @Override
        public int getDecimalDigits() {
            return 0;
        }
    };

    private PreferencesFragment getPreferencesFragment() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(Constants.PREFERENCES_FRAGMENT_TAG);
        return frag == null ? new PreferencesFragment() : (PreferencesFragment) frag;
    }
}