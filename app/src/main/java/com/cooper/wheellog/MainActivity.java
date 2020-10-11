package com.cooper.wheellog;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.Constants.ALARM_TYPE;
import com.cooper.wheellog.utils.Constants.WHEEL_TYPE;
import com.cooper.wheellog.utils.GotwayAdapter;
import com.cooper.wheellog.utils.IWheelAdapter;
import com.cooper.wheellog.utils.SettingsUtil;
import com.cooper.wheellog.utils.Typefaces;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

    Menu mMenu;
    MenuItem miSearch;
    MenuItem miWheel;
    MenuItem miWatch;
    MenuItem miLogging;

    TextView tvSpeed;
    TextView tvTemperature;
    TextView tvTemperature2;
    TextView tvAngle;
    TextView tvRoll;
    TextView tvCurrent;
    TextView tvPower;
    TextView tvVoltage;
    TextView tvVoltageSag;
    TextView tvBattery;
    TextView tvOutput;
    TextView tvCpuLoad;
    TextView tvFanStatus;
    TextView tvChargingStatus;
    TextView tvTopSpeed;
    TextView tvAverageSpeed;
    TextView tvAverageRidingSpeed;
    TextView tvDistance;
    TextView tvWheelDistance;
    TextView tvUserDistance;
    TextView tvModel;
    TextView tvName;
    TextView tvVersion;
    TextView tvSerial;
    TextView tvTotalDistance;
    TextView tvRideTime;
    TextView tvRidingTime;
    TextView tvMode;

    LineChart chart1;

    WheelView wheelView;

    TextView tvBmsWaitText;
    TextView tvTitleBmsBattery1;
    TextView tvTitleBmsBattery2;
    TextView tvTitleBms1Sn;
    TextView tvBms1Sn;
    TextView tvTitleBms2Sn;
    TextView tvBms2Sn;
    TextView tvTitleBms1Fw;
    TextView tvBms1Fw;
    TextView tvTitleBms2Fw;
    TextView tvBms2Fw;
    TextView tvTitleBms1FactoryCap;
    TextView tvBms1FactoryCap;
    TextView tvTitleBms2FactoryCap;
    TextView tvBms2FactoryCap;
    TextView tvTitleBms1ActualCap;
    TextView tvBms1ActualCap;
    TextView tvTitleBms2ActualCap;
    TextView tvBms2ActualCap;
    TextView tvTitleBms1Cycles;
    TextView tvBms1Cycles;
    TextView tvTitleBms2Cycles;
    TextView tvBms2Cycles;
    TextView tvTitleBms1ChrgCount;
    TextView tvBms1ChrgCount;
    TextView tvTitleBms2ChrgCount;
    TextView tvBms2ChrgCount;
    TextView tvTitleBms1MfgDate;
    TextView tvBms1MfgDate;
    TextView tvTitleBms2MfgDate;
    TextView tvBms2MfgDate;
    TextView tvTitleBms1Status;
    TextView tvBms1Status;
    TextView tvTitleBms2Status;
    TextView tvBms2Status;
    TextView tvTitleBms1RemCap;
    TextView tvBms1RemCap;
    TextView tvTitleBms2RemCap;
    TextView tvBms2RemCap;
    TextView tvTitleBms1RemPerc;
    TextView tvBms1RemPerc;
    TextView tvTitleBms2RemPerc;
    TextView tvBms2RemPerc;
    TextView tvTitleBms1Current;
    TextView tvBms1Current;
    TextView tvTitleBms2Current;
    TextView tvBms2Current;
    TextView tvTitleBms1Voltage;
    TextView tvBms1Voltage;
    TextView tvTitleBms2Voltage;
    TextView tvBms2Voltage;
    TextView tvTitleBms1Temp1;
    TextView tvBms1Temp1;
    TextView tvTitleBms2Temp1;
    TextView tvBms2Temp1;
    TextView tvTitleBms1Temp2;
    TextView tvBms1Temp2;
    TextView tvTitleBms2Temp2;
    TextView tvBms2Temp2;
    TextView tvTitleBms1Health;
    TextView tvBms1Health;
    TextView tvTitleBms2Health;
    TextView tvBms2Health;
    TextView tvTitleBms1Cell1;
    TextView tvBms1Cell1;
    TextView tvTitleBms2Cell1;
    TextView tvBms2Cell1;
    TextView tvTitleBms1Cell2;
    TextView tvBms1Cell2;
    TextView tvTitleBms2Cell2;
    TextView tvBms2Cell2;
    TextView tvTitleBms1Cell3;
    TextView tvBms1Cell3;
    TextView tvTitleBms2Cell3;
    TextView tvBms2Cell3;
    TextView tvTitleBms1Cell4;
    TextView tvBms1Cell4;
    TextView tvTitleBms2Cell4;
    TextView tvBms2Cell4;
    TextView tvTitleBms1Cell5;
    TextView tvBms1Cell5;
    TextView tvTitleBms2Cell5;
    TextView tvBms2Cell5;
    TextView tvTitleBms1Cell6;
    TextView tvBms1Cell6;
    TextView tvTitleBms2Cell6;
    TextView tvBms2Cell6;
    TextView tvTitleBms1Cell7;
    TextView tvBms1Cell7;
    TextView tvTitleBms2Cell7;
    TextView tvBms2Cell7;
    TextView tvTitleBms1Cell8;
    TextView tvBms1Cell8;
    TextView tvTitleBms2Cell8;
    TextView tvBms2Cell8;
    TextView tvTitleBms1Cell9;
    TextView tvBms1Cell9;
    TextView tvTitleBms2Cell9;
    TextView tvBms2Cell9;
    TextView tvTitleBms1Cell10;
    TextView tvBms1Cell10;
    TextView tvTitleBms2Cell10;
    TextView tvBms2Cell10;
    TextView tvTitleBms1Cell11;
    TextView tvBms1Cell11;
    TextView tvTitleBms2Cell11;
    TextView tvBms2Cell11;
    TextView tvTitleBms1Cell12;
    TextView tvBms1Cell12;
    TextView tvTitleBms2Cell12;
    TextView tvBms2Cell12;
    TextView tvTitleBms1Cell13;
    TextView tvBms1Cell13;
    TextView tvTitleBms2Cell13;
    TextView tvBms2Cell13;
    TextView tvTitleBms1Cell14;
    TextView tvBms1Cell14;
    TextView tvTitleBms2Cell14;
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
    int viewPagerPage = 0;
    private ArrayList<String> xAxis_labels = new ArrayList<>();
    private boolean use_mph = false;
    private DrawerLayout mDrawer;

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

            loadPreferences(false);
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
                //case Constants.ACTION_WHEEL_SETTING_CHANGED:
                //	if (intent.hasExtra(Constants.INTENT_EXTRA_WHEEL_REFRESH)) {
                //		setWheelPreferences();
                //	}
                //	break;
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
                    loadPreferences(true);
                    // save to specific pref depending on MAC
                    SettingsUtil.savePreferencesTo(getBaseContext(), SettingsUtil.getLastAddress(getBaseContext()));
                    break;
                case Constants.ACTION_PREFERENCE_RESET:
                    Timber.i("Reset battery lowest");
                    wheelView.resetBatteryLowest();
                    break;

                case Constants.ACTION_WHEEL_TYPE_RECOGNIZED:
                    //System.out.println("WheelRecognizedMain");
                    String wheel_type = intent.getStringExtra(Constants.INTENT_EXTRA_WHEEL_TYPE);
                    //showSnackBar(getResources().getString(R.string.wheel_type_recognized, wheel_type), 5000);
                    //((PreferencesFragment) getPreferencesFragment()).show_main_menu();
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
                    SettingsUtil.setLastAddress(getApplicationContext(), mDeviceAddress);

                    // restore specific preferences depending on MAC
                    // before that we pause the SharedPreferenceChangeListener
                    // in MainPreferencesFragment to avoid endless loop
                    MainPreferencesFragment pref = (MainPreferencesFragment) getPreferencesFragment();
                    if (pref != null)
                        pref.onPause();
                    SettingsUtil.restorePreferencesFrom(getApplicationContext(), mDeviceAddress);
                    if (pref != null)
                        pref.onResume();
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
        ((MainPreferencesFragment) getPreferencesFragment()).refreshWheelSettings(WheelData.getInstance().getWheelLight(),
                WheelData.getInstance().getWheelLed(),
                WheelData.getInstance().getWheelHandleButton(),
                WheelData.getInstance().getWheelMaxSpeed(),
                WheelData.getInstance().getSpeakerVolume(),
                WheelData.getInstance().getPedalsPosition());
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

        switch (mConnectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_orange);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                miWheel.setIcon(R.drawable.anim_wheel_icon);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                ((AnimationDrawable) miWheel.getIcon()).start();
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(64);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                miWheel.setIcon(R.drawable.ic_action_wheel_white);
                miWheel.setTitle(R.string.connect_to_wheel);
                miSearch.setEnabled(true);
                miSearch.getIcon().setAlpha(255);
                break;
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
    }

    private void configureDisplay(WHEEL_TYPE wheelType) {
        TextView tvWaitText = (TextView) findViewById(R.id.tvWaitText);
        TextView tvTitleSpeed = (TextView) findViewById(R.id.tvTitleSpeed);
        TextView tvTitleMaxSpeed = (TextView) findViewById(R.id.tvTitleTopSpeed);
        TextView tvTitleAverageSpeed = (TextView) findViewById(R.id.tvTitleAverageSpeed);
        TextView tvTitleAverageRidingSpeed = (TextView) findViewById(R.id.tvTitleAverageRidingSpeed);
        TextView tvTitleBattery = (TextView) findViewById(R.id.tvTitleBattery);
        TextView tvTitleDistance = (TextView) findViewById(R.id.tvTitleDistance);
        TextView tvTitleWheelDistance = (TextView) findViewById(R.id.tvTitleWheelDistance);
        TextView tvTitleUserDistance = (TextView) findViewById(R.id.tvTitleUserDistance);
        TextView tvTitleRideTime = (TextView) findViewById(R.id.tvTitleRideTime);
        TextView tvTitleRidingTime = (TextView) findViewById(R.id.tvTitleRidingTime);
        TextView tvTitleVoltage = (TextView) findViewById(R.id.tvTitleVoltage);
        TextView tvTitleVoltageSag = (TextView) findViewById(R.id.tvTitleVoltageSag);
        TextView tvTitleCurrent = (TextView) findViewById(R.id.tvTitleCurrent);
        TextView tvTitlePower = (TextView) findViewById(R.id.tvTitlePower);
        TextView tvTitleTemperature = (TextView) findViewById(R.id.tvTitleTemperature);
        TextView tvTitleTemperature2 = (TextView) findViewById(R.id.tvTitleTemperature2);
        TextView tvTitleAngle = (TextView) findViewById(R.id.tvTitleAngle);
        TextView tvTitleRoll = (TextView) findViewById(R.id.tvTitleRoll);
        TextView tvTitleFanStatus = (TextView) findViewById(R.id.tvTitleFanStatus);
        TextView tvTitleChargingStatus = (TextView) findViewById(R.id.tvTitleChargingStatus);
        TextView tvTitleOutput = (TextView) findViewById(R.id.tvTitleOutput);
        TextView tvTitleCpuLoad = (TextView) findViewById(R.id.tvTitleCpuLoad);
        TextView tvTitleMode = (TextView) findViewById(R.id.tvTitleMode);
        TextView tvTitleTotalDistance = (TextView) findViewById(R.id.tvTitleTotalDistance);
        TextView tvTitleName = (TextView) findViewById(R.id.tvTitleName);
        TextView tvTitleModel = (TextView) findViewById(R.id.tvTitleModel);
        TextView tvTitleVersion = (TextView) findViewById(R.id.tvTitleVersion);
        TextView tvTitleSerial = (TextView) findViewById(R.id.tvTitleSerial);
        tvBmsWaitText.setVisibility(View.VISIBLE);
        tvTitleBmsBattery1.setVisibility(View.GONE);
        tvTitleBmsBattery2.setVisibility(View.GONE);
        tvBms1Sn.setVisibility(View.GONE);
        tvBms1Fw.setVisibility(View.GONE);
        tvBms1FactoryCap.setVisibility(View.GONE);
        tvBms1ActualCap.setVisibility(View.GONE);
        tvBms1Cycles.setVisibility(View.GONE);
        tvBms1ChrgCount.setVisibility(View.GONE);
        tvBms1MfgDate.setVisibility(View.GONE);
        tvBms1Status.setVisibility(View.GONE);
        tvBms1RemCap.setVisibility(View.GONE);
        tvBms1RemPerc.setVisibility(View.GONE);
        tvBms1Current.setVisibility(View.GONE);
        tvBms1Voltage.setVisibility(View.GONE);
        tvBms1Temp1.setVisibility(View.GONE);
        tvBms1Temp2.setVisibility(View.GONE);
        tvBms1Health.setVisibility(View.GONE);
        tvBms1Cell1.setVisibility(View.GONE);
        tvBms1Cell2.setVisibility(View.GONE);
        tvBms1Cell3.setVisibility(View.GONE);
        tvBms1Cell4.setVisibility(View.GONE);
        tvBms1Cell5.setVisibility(View.GONE);
        tvBms1Cell6.setVisibility(View.GONE);
        tvBms1Cell7.setVisibility(View.GONE);
        tvBms1Cell8.setVisibility(View.GONE);
        tvBms1Cell9.setVisibility(View.GONE);
        tvBms1Cell10.setVisibility(View.GONE);
        tvBms1Cell11.setVisibility(View.GONE);
        tvBms1Cell12.setVisibility(View.GONE);
        tvBms1Cell13.setVisibility(View.GONE);
        tvBms1Cell14.setVisibility(View.GONE);
        tvBms1Cell15.setVisibility(View.GONE);
        tvBms1Cell16.setVisibility(View.GONE);
        tvBms2Sn.setVisibility(View.GONE);
        tvBms2Fw.setVisibility(View.GONE);
        tvBms2FactoryCap.setVisibility(View.GONE);
        tvBms2ActualCap.setVisibility(View.GONE);
        tvBms2Cycles.setVisibility(View.GONE);
        tvBms2ChrgCount.setVisibility(View.GONE);
        tvBms2MfgDate.setVisibility(View.GONE);
        tvBms2Status.setVisibility(View.GONE);
        tvBms2RemCap.setVisibility(View.GONE);
        tvBms2RemPerc.setVisibility(View.GONE);
        tvBms2Current.setVisibility(View.GONE);
        tvBms2Voltage.setVisibility(View.GONE);
        tvBms2Temp1.setVisibility(View.GONE);
        tvBms2Temp2.setVisibility(View.GONE);
        tvBms2Health.setVisibility(View.GONE);
        tvBms2Cell1.setVisibility(View.GONE);
        tvBms2Cell2.setVisibility(View.GONE);
        tvBms2Cell3.setVisibility(View.GONE);
        tvBms2Cell4.setVisibility(View.GONE);
        tvBms2Cell5.setVisibility(View.GONE);
        tvBms2Cell6.setVisibility(View.GONE);
        tvBms2Cell7.setVisibility(View.GONE);
        tvBms2Cell8.setVisibility(View.GONE);
        tvBms2Cell9.setVisibility(View.GONE);
        tvBms2Cell10.setVisibility(View.GONE);
        tvBms2Cell11.setVisibility(View.GONE);
        tvBms2Cell12.setVisibility(View.GONE);
        tvBms2Cell13.setVisibility(View.GONE);
        tvBms2Cell14.setVisibility(View.GONE);
        tvBms2Cell15.setVisibility(View.GONE);
        tvBms2Cell16.setVisibility(View.GONE);
        tvTitleBms1Sn.setVisibility(View.GONE);
        tvTitleBms1Fw.setVisibility(View.GONE);
        tvTitleBms1FactoryCap.setVisibility(View.GONE);
        tvTitleBms1ActualCap.setVisibility(View.GONE);
        tvTitleBms1Cycles.setVisibility(View.GONE);
        tvTitleBms1ChrgCount.setVisibility(View.GONE);
        tvTitleBms1MfgDate.setVisibility(View.GONE);
        tvTitleBms1Status.setVisibility(View.GONE);
        tvTitleBms1RemCap.setVisibility(View.GONE);
        tvTitleBms1RemPerc.setVisibility(View.GONE);
        tvTitleBms1Current.setVisibility(View.GONE);
        tvTitleBms1Voltage.setVisibility(View.GONE);
        tvTitleBms1Temp1.setVisibility(View.GONE);
        tvTitleBms1Temp2.setVisibility(View.GONE);
        tvTitleBms1Health.setVisibility(View.GONE);
        tvTitleBms1Cell1.setVisibility(View.GONE);
        tvTitleBms1Cell2.setVisibility(View.GONE);
        tvTitleBms1Cell3.setVisibility(View.GONE);
        tvTitleBms1Cell4.setVisibility(View.GONE);
        tvTitleBms1Cell5.setVisibility(View.GONE);
        tvTitleBms1Cell6.setVisibility(View.GONE);
        tvTitleBms1Cell7.setVisibility(View.GONE);
        tvTitleBms1Cell8.setVisibility(View.GONE);
        tvTitleBms1Cell9.setVisibility(View.GONE);
        tvTitleBms1Cell10.setVisibility(View.GONE);
        tvTitleBms1Cell11.setVisibility(View.GONE);
        tvTitleBms1Cell12.setVisibility(View.GONE);
        tvTitleBms1Cell13.setVisibility(View.GONE);
        tvTitleBms1Cell14.setVisibility(View.GONE);
        tvTitleBms1Cell15.setVisibility(View.GONE);
        tvTitleBms1Cell16.setVisibility(View.GONE);
        tvTitleBms2Sn.setVisibility(View.GONE);
        tvTitleBms2Fw.setVisibility(View.GONE);
        tvTitleBms2FactoryCap.setVisibility(View.GONE);
        tvTitleBms2ActualCap.setVisibility(View.GONE);
        tvTitleBms2Cycles.setVisibility(View.GONE);
        tvTitleBms2ChrgCount.setVisibility(View.GONE);
        tvTitleBms2MfgDate.setVisibility(View.GONE);
        tvTitleBms2Status.setVisibility(View.GONE);
        tvTitleBms2RemCap.setVisibility(View.GONE);
        tvTitleBms2RemPerc.setVisibility(View.GONE);
        tvTitleBms2Current.setVisibility(View.GONE);
        tvTitleBms2Voltage.setVisibility(View.GONE);
        tvTitleBms2Temp1.setVisibility(View.GONE);
        tvTitleBms2Temp2.setVisibility(View.GONE);
        tvTitleBms2Health.setVisibility(View.GONE);
        tvTitleBms2Cell1.setVisibility(View.GONE);
        tvTitleBms2Cell2.setVisibility(View.GONE);
        tvTitleBms2Cell3.setVisibility(View.GONE);
        tvTitleBms2Cell4.setVisibility(View.GONE);
        tvTitleBms2Cell5.setVisibility(View.GONE);
        tvTitleBms2Cell6.setVisibility(View.GONE);
        tvTitleBms2Cell7.setVisibility(View.GONE);
        tvTitleBms2Cell8.setVisibility(View.GONE);
        tvTitleBms2Cell9.setVisibility(View.GONE);
        tvTitleBms2Cell10.setVisibility(View.GONE);
        tvTitleBms2Cell11.setVisibility(View.GONE);
        tvTitleBms2Cell12.setVisibility(View.GONE);
        tvTitleBms2Cell13.setVisibility(View.GONE);
        tvTitleBms2Cell14.setVisibility(View.GONE);
        tvTitleBms2Cell15.setVisibility(View.GONE);
        tvTitleBms2Cell16.setVisibility(View.GONE);


        switch (wheelType) {
            case Unknown:
                break;
            case KINGSONG:
                tvWaitText.setVisibility(View.GONE);
                tvTitleSpeed.setVisibility(View.VISIBLE);
                tvSpeed.setVisibility(View.VISIBLE);
                tvTitleMaxSpeed.setVisibility(View.VISIBLE);
                tvTopSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageSpeed.setVisibility(View.VISIBLE);
                tvAverageSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvTitleBattery.setVisibility(View.VISIBLE);
                tvBattery.setVisibility(View.VISIBLE);
                tvTitleDistance.setVisibility(View.VISIBLE);
                tvDistance.setVisibility(View.VISIBLE);
                tvTitleWheelDistance.setVisibility(View.VISIBLE);
                tvWheelDistance.setVisibility(View.VISIBLE);
                tvTitleUserDistance.setVisibility(View.VISIBLE);
                tvUserDistance.setVisibility(View.VISIBLE);
                tvTitleRideTime.setVisibility(View.VISIBLE);
                tvRideTime.setVisibility(View.VISIBLE);
                tvTitleRidingTime.setVisibility(View.VISIBLE);
                tvRidingTime.setVisibility(View.VISIBLE);
                tvTitleVoltage.setVisibility(View.VISIBLE);
                tvVoltage.setVisibility(View.VISIBLE);
                tvTitleVoltageSag.setVisibility(View.VISIBLE);
                tvVoltageSag.setVisibility(View.VISIBLE);
                tvTitleCurrent.setVisibility(View.VISIBLE);
                tvCurrent.setVisibility(View.VISIBLE);
                tvTitlePower.setVisibility(View.VISIBLE);
                tvPower.setVisibility(View.VISIBLE);
                tvTitleTemperature.setVisibility(View.VISIBLE);
                tvTemperature.setVisibility(View.VISIBLE);
                tvTitleTemperature2.setVisibility(View.VISIBLE);
                tvTemperature2.setVisibility(View.VISIBLE);
                tvTitleFanStatus.setVisibility(View.VISIBLE);
                tvFanStatus.setVisibility(View.VISIBLE);
                tvTitleFanStatus.setVisibility(View.VISIBLE);
                tvChargingStatus.setVisibility(View.VISIBLE);
                tvTitleChargingStatus.setVisibility(View.VISIBLE);
                tvOutput.setVisibility(View.VISIBLE);
                tvTitleOutput.setVisibility(View.VISIBLE);
                tvCpuLoad.setVisibility(View.VISIBLE);
                tvTitleCpuLoad.setVisibility(View.VISIBLE);
                tvMode.setVisibility(View.VISIBLE);
                tvTitleTotalDistance.setVisibility(View.VISIBLE);
                tvTotalDistance.setVisibility(View.VISIBLE);
                tvTitleName.setVisibility(View.VISIBLE);
                tvName.setVisibility(View.VISIBLE);
                tvTitleModel.setVisibility(View.VISIBLE);
                tvModel.setVisibility(View.VISIBLE);
                tvTitleVersion.setVisibility(View.VISIBLE);
                tvVersion.setVisibility(View.VISIBLE);
                tvTitleSerial.setVisibility(View.VISIBLE);
                tvSerial.setVisibility(View.VISIBLE);
                break;
            case VETERAN:
            case GOTWAY:
                tvWaitText.setVisibility(View.GONE);
                tvTitleSpeed.setVisibility(View.VISIBLE);
                tvSpeed.setVisibility(View.VISIBLE);
                tvTitleMaxSpeed.setVisibility(View.VISIBLE);
                tvTopSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageSpeed.setVisibility(View.VISIBLE);
                tvAverageSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvTitleBattery.setVisibility(View.VISIBLE);
                tvBattery.setVisibility(View.VISIBLE);
                tvTitleDistance.setVisibility(View.VISIBLE);
                tvDistance.setVisibility(View.VISIBLE);
                tvTitleWheelDistance.setVisibility(View.VISIBLE);
                tvWheelDistance.setVisibility(View.VISIBLE);
                tvTitleUserDistance.setVisibility(View.VISIBLE);
                tvUserDistance.setVisibility(View.VISIBLE);
                tvTitleRideTime.setVisibility(View.VISIBLE);
                tvRideTime.setVisibility(View.VISIBLE);
                tvTitleRidingTime.setVisibility(View.VISIBLE);
                tvRidingTime.setVisibility(View.VISIBLE);
                tvTitleVoltage.setVisibility(View.VISIBLE);
                tvVoltage.setVisibility(View.VISIBLE);
                tvTitleVoltageSag.setVisibility(View.VISIBLE);
                tvVoltageSag.setVisibility(View.VISIBLE);
                tvTitleCurrent.setVisibility(View.VISIBLE);
                tvCurrent.setVisibility(View.VISIBLE);
                tvTitlePower.setVisibility(View.VISIBLE);
                tvPower.setVisibility(View.VISIBLE);
                tvTitleTemperature.setVisibility(View.VISIBLE);
                tvTemperature.setVisibility(View.VISIBLE);
                tvTitleTotalDistance.setVisibility(View.VISIBLE);
                tvTotalDistance.setVisibility(View.VISIBLE);
                tvTitleVersion.setVisibility(View.VISIBLE);
                tvVersion.setVisibility(View.VISIBLE);
                tvTitleModel.setVisibility(View.VISIBLE);
                tvModel.setVisibility(View.VISIBLE);
                break;
            case INMOTION:
                tvWaitText.setVisibility(View.GONE);
                tvTitleSpeed.setVisibility(View.VISIBLE);
                tvSpeed.setVisibility(View.VISIBLE);
                tvTitleMaxSpeed.setVisibility(View.VISIBLE);
                tvTopSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageSpeed.setVisibility(View.VISIBLE);
                tvAverageSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvTitleBattery.setVisibility(View.VISIBLE);
                tvBattery.setVisibility(View.VISIBLE);
                tvTitleDistance.setVisibility(View.VISIBLE);
                tvDistance.setVisibility(View.VISIBLE);
                tvTitleUserDistance.setVisibility(View.VISIBLE);
                tvUserDistance.setVisibility(View.VISIBLE);
                tvTitleRideTime.setVisibility(View.VISIBLE);
                tvRideTime.setVisibility(View.VISIBLE);
                tvTitleRidingTime.setVisibility(View.VISIBLE);
                tvRidingTime.setVisibility(View.VISIBLE);
                tvTitleVoltage.setVisibility(View.VISIBLE);
                tvVoltage.setVisibility(View.VISIBLE);
                tvTitleVoltageSag.setVisibility(View.VISIBLE);
                tvVoltageSag.setVisibility(View.VISIBLE);
                tvTitleCurrent.setVisibility(View.VISIBLE);
                tvCurrent.setVisibility(View.VISIBLE);
                tvTitlePower.setVisibility(View.VISIBLE);
                tvPower.setVisibility(View.VISIBLE);
                tvTitleTemperature.setVisibility(View.VISIBLE);
                tvTemperature.setVisibility(View.VISIBLE);
                tvTitleTemperature2.setVisibility(View.VISIBLE);
                tvTemperature2.setVisibility(View.VISIBLE);
                tvTitleMode.setVisibility(View.VISIBLE);
                tvMode.setVisibility(View.VISIBLE);
                tvTitleAngle.setVisibility(View.VISIBLE);
                tvAngle.setVisibility(View.VISIBLE);
                tvTitleRoll.setVisibility(View.VISIBLE);
                tvRoll.setVisibility(View.VISIBLE);
                tvTitleTotalDistance.setVisibility(View.VISIBLE);
                tvTotalDistance.setVisibility(View.VISIBLE);
                tvTitleModel.setVisibility(View.VISIBLE);
                tvModel.setVisibility(View.VISIBLE);
                tvTitleVersion.setVisibility(View.VISIBLE);
                tvVersion.setVisibility(View.VISIBLE);
                tvTitleSerial.setVisibility(View.VISIBLE);
                tvSerial.setVisibility(View.VISIBLE);
                break;

            case NINEBOT_Z:
                tvWaitText.setVisibility(View.GONE);
                tvTitleSpeed.setVisibility(View.VISIBLE);
                tvSpeed.setVisibility(View.VISIBLE);
                tvTitleMaxSpeed.setVisibility(View.VISIBLE);
                tvTopSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageSpeed.setVisibility(View.VISIBLE);
                tvAverageSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvTitleBattery.setVisibility(View.VISIBLE);
                tvBattery.setVisibility(View.VISIBLE);
                tvTitleDistance.setVisibility(View.VISIBLE);
                tvDistance.setVisibility(View.VISIBLE);
                tvTitleUserDistance.setVisibility(View.VISIBLE);
                tvUserDistance.setVisibility(View.VISIBLE);
                tvTitleRideTime.setVisibility(View.VISIBLE);
                tvRideTime.setVisibility(View.VISIBLE);
                tvTitleRidingTime.setVisibility(View.VISIBLE);
                tvRidingTime.setVisibility(View.VISIBLE);
                tvTitleVoltage.setVisibility(View.VISIBLE);
                tvVoltage.setVisibility(View.VISIBLE);
                tvTitleCurrent.setVisibility(View.VISIBLE);
                tvTitleVoltageSag.setVisibility(View.VISIBLE);
                tvVoltageSag.setVisibility(View.VISIBLE);
                tvCurrent.setVisibility(View.VISIBLE);
                tvTitlePower.setVisibility(View.VISIBLE);
                tvPower.setVisibility(View.VISIBLE);
                tvTitleTemperature.setVisibility(View.VISIBLE);
                tvTemperature.setVisibility(View.VISIBLE);
                tvTitleTemperature2.setVisibility(View.GONE);
                tvTemperature2.setVisibility(View.GONE);
                tvTitleMode.setVisibility(View.GONE);
                tvMode.setVisibility(View.GONE);
                tvTitleAngle.setVisibility(View.VISIBLE);
                tvAngle.setVisibility(View.VISIBLE);
                tvTitleRoll.setVisibility(View.VISIBLE);
                tvRoll.setVisibility(View.VISIBLE);
                tvTitleTotalDistance.setVisibility(View.VISIBLE);
                tvTotalDistance.setVisibility(View.VISIBLE);
                tvTitleModel.setVisibility(View.VISIBLE);
                tvModel.setVisibility(View.VISIBLE);
                tvTitleVersion.setVisibility(View.VISIBLE);
                tvVersion.setVisibility(View.VISIBLE);
                tvTitleSerial.setVisibility(View.VISIBLE);
                tvSerial.setVisibility(View.VISIBLE);

                tvBmsWaitText.setVisibility(View.GONE);
                tvTitleBmsBattery1.setVisibility(View.VISIBLE);
                tvTitleBmsBattery2.setVisibility(View.VISIBLE);

                tvBms1Sn.setVisibility(View.VISIBLE);
                tvBms1Fw.setVisibility(View.VISIBLE);
                tvBms1FactoryCap.setVisibility(View.VISIBLE);
                tvBms1ActualCap.setVisibility(View.VISIBLE);
                tvBms1Cycles.setVisibility(View.VISIBLE);
                tvBms1ChrgCount.setVisibility(View.VISIBLE);
                tvBms1MfgDate.setVisibility(View.VISIBLE);
                tvBms1Status.setVisibility(View.VISIBLE);
                tvBms1RemCap.setVisibility(View.VISIBLE);
                tvBms1RemPerc.setVisibility(View.VISIBLE);
                tvBms1Current.setVisibility(View.VISIBLE);
                tvBms1Voltage.setVisibility(View.VISIBLE);
                tvBms1Temp1.setVisibility(View.VISIBLE);
                tvBms1Temp2.setVisibility(View.VISIBLE);
                tvBms1Health.setVisibility(View.VISIBLE);
                tvBms1Cell1.setVisibility(View.VISIBLE);
                tvBms1Cell2.setVisibility(View.VISIBLE);
                tvBms1Cell3.setVisibility(View.VISIBLE);
                tvBms1Cell4.setVisibility(View.VISIBLE);
                tvBms1Cell5.setVisibility(View.VISIBLE);
                tvBms1Cell6.setVisibility(View.VISIBLE);
                tvBms1Cell7.setVisibility(View.VISIBLE);
                tvBms1Cell8.setVisibility(View.VISIBLE);
                tvBms1Cell9.setVisibility(View.VISIBLE);
                tvBms1Cell10.setVisibility(View.VISIBLE);
                tvBms1Cell11.setVisibility(View.VISIBLE);
                tvBms1Cell12.setVisibility(View.VISIBLE);
                tvBms1Cell13.setVisibility(View.VISIBLE);
                tvBms1Cell14.setVisibility(View.VISIBLE);
                tvBms1Cell15.setVisibility(View.GONE);
                tvBms1Cell16.setVisibility(View.GONE);
                tvBms2Sn.setVisibility(View.VISIBLE);
                tvBms2Fw.setVisibility(View.VISIBLE);
                tvBms2FactoryCap.setVisibility(View.VISIBLE);
                tvBms2ActualCap.setVisibility(View.VISIBLE);
                tvBms2Cycles.setVisibility(View.VISIBLE);
                tvBms2ChrgCount.setVisibility(View.VISIBLE);
                tvBms2MfgDate.setVisibility(View.VISIBLE);
                tvBms2Status.setVisibility(View.VISIBLE);
                tvBms2RemCap.setVisibility(View.VISIBLE);
                tvBms2RemPerc.setVisibility(View.VISIBLE);
                tvBms2Current.setVisibility(View.VISIBLE);
                tvBms2Voltage.setVisibility(View.VISIBLE);
                tvBms2Temp1.setVisibility(View.VISIBLE);
                tvBms2Temp2.setVisibility(View.VISIBLE);
                tvBms2Health.setVisibility(View.VISIBLE);
                tvBms2Cell1.setVisibility(View.VISIBLE);
                tvBms2Cell2.setVisibility(View.VISIBLE);
                tvBms2Cell3.setVisibility(View.VISIBLE);
                tvBms2Cell4.setVisibility(View.VISIBLE);
                tvBms2Cell5.setVisibility(View.VISIBLE);
                tvBms2Cell6.setVisibility(View.VISIBLE);
                tvBms2Cell7.setVisibility(View.VISIBLE);
                tvBms2Cell8.setVisibility(View.VISIBLE);
                tvBms2Cell9.setVisibility(View.VISIBLE);
                tvBms2Cell10.setVisibility(View.VISIBLE);
                tvBms2Cell11.setVisibility(View.VISIBLE);
                tvBms2Cell12.setVisibility(View.VISIBLE);
                tvBms2Cell13.setVisibility(View.VISIBLE);
                tvBms2Cell14.setVisibility(View.VISIBLE);
                tvBms2Cell15.setVisibility(View.GONE);
                tvBms2Cell16.setVisibility(View.GONE);
                tvTitleBms1Sn.setVisibility(View.VISIBLE);
                tvTitleBms1Fw.setVisibility(View.VISIBLE);
                tvTitleBms1FactoryCap.setVisibility(View.VISIBLE);
                tvTitleBms1ActualCap.setVisibility(View.VISIBLE);
                tvTitleBms1Cycles.setVisibility(View.VISIBLE);
                tvTitleBms1ChrgCount.setVisibility(View.VISIBLE);
                tvTitleBms1MfgDate.setVisibility(View.VISIBLE);
                tvTitleBms1Status.setVisibility(View.VISIBLE);
                tvTitleBms1RemCap.setVisibility(View.VISIBLE);
                tvTitleBms1RemPerc.setVisibility(View.VISIBLE);
                tvTitleBms1Current.setVisibility(View.VISIBLE);
                tvTitleBms1Voltage.setVisibility(View.VISIBLE);
                tvTitleBms1Temp1.setVisibility(View.VISIBLE);
                tvTitleBms1Temp2.setVisibility(View.VISIBLE);
                tvTitleBms1Health.setVisibility(View.VISIBLE);
                tvTitleBms1Cell1.setVisibility(View.VISIBLE);
                tvTitleBms1Cell2.setVisibility(View.VISIBLE);
                tvTitleBms1Cell3.setVisibility(View.VISIBLE);
                tvTitleBms1Cell4.setVisibility(View.VISIBLE);
                tvTitleBms1Cell5.setVisibility(View.VISIBLE);
                tvTitleBms1Cell6.setVisibility(View.VISIBLE);
                tvTitleBms1Cell7.setVisibility(View.VISIBLE);
                tvTitleBms1Cell8.setVisibility(View.VISIBLE);
                tvTitleBms1Cell9.setVisibility(View.VISIBLE);
                tvTitleBms1Cell10.setVisibility(View.VISIBLE);
                tvTitleBms1Cell11.setVisibility(View.VISIBLE);
                tvTitleBms1Cell12.setVisibility(View.VISIBLE);
                tvTitleBms1Cell13.setVisibility(View.VISIBLE);
                tvTitleBms1Cell14.setVisibility(View.VISIBLE);
                tvTitleBms1Cell15.setVisibility(View.GONE);
                tvTitleBms1Cell16.setVisibility(View.GONE);
                tvTitleBms2Sn.setVisibility(View.VISIBLE);
                tvTitleBms2Fw.setVisibility(View.VISIBLE);
                tvTitleBms2FactoryCap.setVisibility(View.VISIBLE);
                tvTitleBms2ActualCap.setVisibility(View.VISIBLE);
                tvTitleBms2Cycles.setVisibility(View.VISIBLE);
                tvTitleBms2ChrgCount.setVisibility(View.VISIBLE);
                tvTitleBms2MfgDate.setVisibility(View.VISIBLE);
                tvTitleBms2Status.setVisibility(View.VISIBLE);
                tvTitleBms2RemCap.setVisibility(View.VISIBLE);
                tvTitleBms2RemPerc.setVisibility(View.VISIBLE);
                tvTitleBms2Current.setVisibility(View.VISIBLE);
                tvTitleBms2Voltage.setVisibility(View.VISIBLE);
                tvTitleBms2Temp1.setVisibility(View.VISIBLE);
                tvTitleBms2Temp2.setVisibility(View.VISIBLE);
                tvTitleBms2Health.setVisibility(View.VISIBLE);
                tvTitleBms2Cell1.setVisibility(View.VISIBLE);
                tvTitleBms2Cell2.setVisibility(View.VISIBLE);
                tvTitleBms2Cell3.setVisibility(View.VISIBLE);
                tvTitleBms2Cell4.setVisibility(View.VISIBLE);
                tvTitleBms2Cell5.setVisibility(View.VISIBLE);
                tvTitleBms2Cell6.setVisibility(View.VISIBLE);
                tvTitleBms2Cell7.setVisibility(View.VISIBLE);
                tvTitleBms2Cell8.setVisibility(View.VISIBLE);
                tvTitleBms2Cell9.setVisibility(View.VISIBLE);
                tvTitleBms2Cell10.setVisibility(View.VISIBLE);
                tvTitleBms2Cell11.setVisibility(View.VISIBLE);
                tvTitleBms2Cell12.setVisibility(View.VISIBLE);
                tvTitleBms2Cell13.setVisibility(View.VISIBLE);
                tvTitleBms2Cell14.setVisibility(View.VISIBLE);
                tvTitleBms2Cell15.setVisibility(View.GONE);
                tvTitleBms2Cell16.setVisibility(View.GONE);

                break;
            case NINEBOT:
                tvWaitText.setVisibility(View.GONE);
                tvTitleSpeed.setVisibility(View.VISIBLE);
                tvSpeed.setVisibility(View.VISIBLE);
                tvTitleMaxSpeed.setVisibility(View.VISIBLE);
                tvTopSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageSpeed.setVisibility(View.VISIBLE);
                tvAverageSpeed.setVisibility(View.VISIBLE);
                tvTitleAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvAverageRidingSpeed.setVisibility(View.VISIBLE);
                tvTitleBattery.setVisibility(View.VISIBLE);
                tvBattery.setVisibility(View.VISIBLE);
                tvTitleDistance.setVisibility(View.VISIBLE);
                tvDistance.setVisibility(View.VISIBLE);
                tvTitleUserDistance.setVisibility(View.VISIBLE);
                tvUserDistance.setVisibility(View.VISIBLE);
                tvTitleRideTime.setVisibility(View.VISIBLE);
                tvRideTime.setVisibility(View.VISIBLE);
                tvTitleRidingTime.setVisibility(View.VISIBLE);
                tvRidingTime.setVisibility(View.VISIBLE);
                tvTitleVoltage.setVisibility(View.VISIBLE);
                tvVoltage.setVisibility(View.VISIBLE);
                tvTitleVoltageSag.setVisibility(View.VISIBLE);
                tvVoltageSag.setVisibility(View.VISIBLE);
                tvTitleCurrent.setVisibility(View.VISIBLE);
                tvCurrent.setVisibility(View.VISIBLE);
                tvTitlePower.setVisibility(View.VISIBLE);
                tvPower.setVisibility(View.VISIBLE);
                tvTitleTemperature.setVisibility(View.VISIBLE);
                tvTemperature.setVisibility(View.VISIBLE);
                tvTitleTemperature2.setVisibility(View.GONE);
                tvTemperature2.setVisibility(View.GONE);
                tvTitleMode.setVisibility(View.GONE);
                tvMode.setVisibility(View.GONE);
                tvTitleAngle.setVisibility(View.VISIBLE);
                tvAngle.setVisibility(View.VISIBLE);
                tvTitleRoll.setVisibility(View.VISIBLE);
                tvRoll.setVisibility(View.VISIBLE);
                tvTitleTotalDistance.setVisibility(View.VISIBLE);
                tvTotalDistance.setVisibility(View.VISIBLE);
                tvTitleModel.setVisibility(View.VISIBLE);
                tvModel.setVisibility(View.VISIBLE);
                tvTitleVersion.setVisibility(View.VISIBLE);
                tvVersion.setVisibility(View.VISIBLE);
                tvTitleSerial.setVisibility(View.VISIBLE);
                tvSerial.setVisibility(View.VISIBLE);
                break;
            default:
                tvWaitText.setVisibility(View.VISIBLE);
                tvTitleSpeed.setVisibility(View.GONE);
                tvSpeed.setVisibility(View.GONE);
                tvTitleMaxSpeed.setVisibility(View.GONE);
                tvTopSpeed.setVisibility(View.GONE);
                tvTitleAverageSpeed.setVisibility(View.GONE);
                tvAverageSpeed.setVisibility(View.GONE);
                tvTitleAverageRidingSpeed.setVisibility(View.GONE);
                tvAverageRidingSpeed.setVisibility(View.GONE);
                tvTitleBattery.setVisibility(View.GONE);
                tvBattery.setVisibility(View.GONE);
                tvTitleDistance.setVisibility(View.GONE);
                tvDistance.setVisibility(View.GONE);
                tvTitleWheelDistance.setVisibility(View.GONE);
                tvWheelDistance.setVisibility(View.GONE);
                tvTitleUserDistance.setVisibility(View.GONE);
                tvUserDistance.setVisibility(View.GONE);
                tvTitleRideTime.setVisibility(View.GONE);
                tvRideTime.setVisibility(View.GONE);
                tvTitleRidingTime.setVisibility(View.GONE);
                tvRidingTime.setVisibility(View.GONE);
                tvTitleVoltage.setVisibility(View.GONE);
                tvVoltage.setVisibility(View.GONE);
                tvTitleVoltageSag.setVisibility(View.GONE);
                tvVoltageSag.setVisibility(View.GONE);
                tvTitleCurrent.setVisibility(View.GONE);
                tvCurrent.setVisibility(View.GONE);
                tvTitlePower.setVisibility(View.GONE);
                tvPower.setVisibility(View.GONE);
                tvTitleTemperature.setVisibility(View.GONE);
                tvTemperature.setVisibility(View.GONE);
                tvTitleTemperature2.setVisibility(View.GONE);
                tvTemperature2.setVisibility(View.GONE);
                tvTitleAngle.setVisibility(View.GONE);
                tvAngle.setVisibility(View.GONE);
                tvTitleRoll.setVisibility(View.GONE);
                tvRoll.setVisibility(View.GONE);
                tvTitleFanStatus.setVisibility(View.GONE);
                tvFanStatus.setVisibility(View.GONE);
                tvTitleChargingStatus.setVisibility(View.GONE);
                tvChargingStatus.setVisibility(View.GONE);
                tvTitleOutput.setVisibility(View.GONE);
                tvOutput.setVisibility(View.GONE);
                tvTitleCpuLoad.setVisibility(View.GONE);
                tvCpuLoad.setVisibility(View.GONE);
                tvTitleMode.setVisibility(View.GONE);
                tvMode.setVisibility(View.GONE);
                tvTitleTotalDistance.setVisibility(View.GONE);
                tvTotalDistance.setVisibility(View.GONE);
                tvTitleName.setVisibility(View.GONE);
                tvName.setVisibility(View.GONE);
                tvTitleModel.setVisibility(View.GONE);
                tvModel.setVisibility(View.GONE);
                tvTitleVersion.setVisibility(View.GONE);
                tvVersion.setVisibility(View.GONE);
                tvTitleSerial.setVisibility(View.GONE);
                tvSerial.setVisibility(View.GONE);
                break;
        }
    }

    private void updateScreen(boolean updateGraph) {
        WheelData data = WheelData.getInstance();
        switch (viewPagerPage) {
            case 0: // GUI View
                data.setBmsView(false);
                wheelView.setSpeed(data.getSpeed());
                wheelView.setBattery(data.getBatteryLevel());
                wheelView.setTemperature(data.getTemperature());
                wheelView.setRideTime(data.getRidingTimeString());
                wheelView.setTopSpeed(data.getTopSpeedDouble());
                wheelView.setDistance(data.getDistanceDouble());
                wheelView.setTotalDistance(data.getTotalDistanceDouble());
                wheelView.setVoltage(data.getVoltageDouble());
                wheelView.setCurrent(data.getPowerDouble());
                wheelView.setAverageSpeed(data.getAverageRidingSpeedDouble());
                wheelView.setWheelModel(data.getModel().equals("") ? data.getName() : data.getModel());
                wheelView.redrawTextBoxes();
                wheelView.setMaxPwm(data.getMaxPwm());
                wheelView.setMaxTemperature(data.getMaxTemp());
                wheelView.setPwm(data.getCalculatedPwm());
                break;
            case 1: // Text View
                WheelData.getInstance().setBmsView(false);
                if (use_mph) {
                    tvSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getSpeedDouble())));
                    tvTopSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getTopSpeedDouble())));
                    tvAverageSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getAverageSpeedDouble())));
                    tvAverageRidingSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.mph), kmToMiles(WheelData.getInstance().getAverageRidingSpeedDouble())));
                    tvDistance.setText(String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getDistanceDouble())));
                    tvWheelDistance.setText(String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getWheelDistanceDouble())));
                    tvUserDistance.setText(String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getUserDistanceDouble())));
                    tvTotalDistance.setText(String.format(Locale.US, "%.2f " + getString(R.string.milli), kmToMiles(WheelData.getInstance().getTotalDistanceDouble())));
                } else {
                    tvSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getSpeedDouble()));
                    tvTopSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getTopSpeedDouble()));
                    tvAverageSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getAverageSpeedDouble()));
                    tvAverageRidingSpeed.setText(String.format(Locale.US, "%.1f " + getString(R.string.kmh), WheelData.getInstance().getAverageRidingSpeedDouble()));
                    tvDistance.setText(String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getDistanceDouble()));
                    tvWheelDistance.setText(String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getWheelDistanceDouble()));
                    tvUserDistance.setText(String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getUserDistanceDouble()));
                    tvTotalDistance.setText(String.format(Locale.US, "%.3f " + getString(R.string.km), WheelData.getInstance().getTotalDistanceDouble()));
                }

                tvVoltage.setText(String.format(Locale.US, "%.2f " + getString(R.string.volt), WheelData.getInstance().getVoltageDouble()));
                tvVoltageSag.setText(String.format(Locale.US, "%.2f " + getString(R.string.volt), WheelData.getInstance().getVoltageSagDouble()));
                tvTemperature.setText(String.format(Locale.US, "%d°C", WheelData.getInstance().getTemperature()));
                tvTemperature2.setText(String.format(Locale.US, "%d°C", WheelData.getInstance().getTemperature2()));
                tvAngle.setText(String.format(Locale.US, "%.2f°", WheelData.getInstance().getAngle()));
                tvRoll.setText(String.format(Locale.US, "%.2f°", WheelData.getInstance().getRoll()));
                tvCurrent.setText(String.format(Locale.US, "%.2f " + getString(R.string.amp), WheelData.getInstance().getCurrentDouble()));
                tvPower.setText(String.format(Locale.US, "%.2f " + getString(R.string.watt), WheelData.getInstance().getPowerDouble()));
                tvBattery.setText(String.format(Locale.US, "%d%%", WheelData.getInstance().getBatteryLevel()));
                tvFanStatus.setText(WheelData.getInstance().getFanStatus() == 0 ? getString(R.string.off) : getString(R.string.on));
                tvChargingStatus.setText(WheelData.getInstance().getChargingStatus() == 0 ? getString(R.string.discharging) : getString(R.string.charging));
                tvVersion.setText(String.format(Locale.US, "%s", WheelData.getInstance().getVersion()));
                tvOutput.setText(String.format(Locale.US, "%d%%", WheelData.getInstance().getOutput()));
                tvCpuLoad.setText(String.format(Locale.US, "%d%%", WheelData.getInstance().getCpuLoad()));
                tvName.setText(WheelData.getInstance().getName());
                tvModel.setText(WheelData.getInstance().getModel());
                tvSerial.setText(WheelData.getInstance().getSerial());
                tvRideTime.setText(WheelData.getInstance().getRideTimeString());
                tvRidingTime.setText(WheelData.getInstance().getRidingTimeString());
                tvMode.setText(WheelData.getInstance().getModeStr());
                break;
            case 2: // Graph  View
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
            case 3: //BMS view
                WheelData.getInstance().setBmsView(true);
                tvBms1Sn.setText(WheelData.getInstance().getBms1SerialNumber());
                tvBms1Fw.setText(WheelData.getInstance().getBms1VersionNumber());
                tvBms1FactoryCap.setText(String.format(Locale.US, "%d mAh", WheelData.getInstance().getBms1FactoryCap()));
                tvBms1ActualCap.setText(String.format(Locale.US, "%d mAh", WheelData.getInstance().getBms1ActualCap()));
                tvBms1Cycles.setText(String.format(Locale.US, "%d", WheelData.getInstance().getBms1FullCycles()));
                tvBms1ChrgCount.setText(String.format(Locale.US, "%d", WheelData.getInstance().getBms1ChargeCount()));
                tvBms1MfgDate.setText(WheelData.getInstance().getBms1MfgDateStr());
                tvBms1Status.setText(String.format(Locale.US, "%d", WheelData.getInstance().getBms1Status()));
                tvBms1RemCap.setText(String.format(Locale.US, "%d mAh", WheelData.getInstance().getBms1RemCap()));
                tvBms1RemPerc.setText(String.format(Locale.US, "%d %%", WheelData.getInstance().getBms1RemPerc()));
                tvBms1Current.setText(String.format(Locale.US, "%.2f A", WheelData.getInstance().getBms1Current()));
                tvBms1Voltage.setText(String.format(Locale.US, "%.2f V", WheelData.getInstance().getBms1Voltage()));
                tvBms1Temp1.setText(String.format(Locale.US, "%d°C", WheelData.getInstance().getBms1Temp1()));
                tvBms1Temp2.setText(String.format(Locale.US, "%d°C", WheelData.getInstance().getBms1Temp2()));
                tvBms1Health.setText(String.format(Locale.US, "%d %%", WheelData.getInstance().getBms1Health()));
                int balanceMap = WheelData.getInstance().getBms1BalanceMap();
                String bal = "";
                if (((balanceMap) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell1.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell1(), bal));
                if (((balanceMap >> 1) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell2.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell2(), bal));
                if (((balanceMap >> 2) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell3.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell3(), bal));
                if (((balanceMap >> 3) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell4.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell4(), bal));
                if (((balanceMap >> 4) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell5.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell5(), bal));
                if (((balanceMap >> 5) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell6.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell6(), bal));
                if (((balanceMap >> 6) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell7.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell7(), bal));
                if (((balanceMap >> 7) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell8.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell8(), bal));
                if (((balanceMap >> 8) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell9.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell9(), bal));
                if (((balanceMap >> 9) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell10.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell10(), bal));
                if (((balanceMap >> 10) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell11.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell11(), bal));
                if (((balanceMap >> 11) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell12.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell12(), bal));
                if (((balanceMap >> 12) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell13.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell13(), bal));
                if (((balanceMap >> 13) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell14.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell14(), bal));
                if (WheelData.getInstance().getBms1Cell15() == 0.0) {
                    tvBms1Cell15.setVisibility(View.GONE);
                    tvTitleBms1Cell15.setVisibility(View.GONE);
                } else {
                    tvBms1Cell15.setVisibility(View.VISIBLE);
                    tvTitleBms1Cell15.setVisibility(View.VISIBLE);
                }
                if (WheelData.getInstance().getBms1Cell16() == 0.0) {
                    tvBms1Cell16.setVisibility(View.GONE);
                    tvTitleBms1Cell16.setVisibility(View.GONE);
                } else {
                    tvBms1Cell16.setVisibility(View.VISIBLE);
                    tvTitleBms1Cell16.setVisibility(View.VISIBLE);
                }
                if (((balanceMap >> 14) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell15.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell15(), bal));
                if (((balanceMap >> 15) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms1Cell16.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getBms1Cell16(), bal));
                tvBms2Sn.setText(WheelData.getInstance().getbms2SerialNumber());
                tvBms2Fw.setText(WheelData.getInstance().getbms2VersionNumber());
                tvBms2FactoryCap.setText(String.format(Locale.US, "%d mAh", WheelData.getInstance().getbms2FactoryCap()));
                tvBms2ActualCap.setText(String.format(Locale.US, "%d mAh", WheelData.getInstance().getbms2ActualCap()));
                tvBms2Cycles.setText(String.format(Locale.US, "%d", WheelData.getInstance().getbms2FullCycles()));
                tvBms2ChrgCount.setText(String.format(Locale.US, "%d", WheelData.getInstance().getbms2ChargeCount()));
                tvBms2MfgDate.setText(WheelData.getInstance().getbms2MfgDateStr());
                tvBms2Status.setText(String.format(Locale.US, "%d", WheelData.getInstance().getbms2Status()));
                tvBms2RemCap.setText(String.format(Locale.US, "%d mAh", WheelData.getInstance().getbms2RemCap()));
                tvBms2RemPerc.setText(String.format(Locale.US, "%d %%", WheelData.getInstance().getbms2RemPerc()));
                tvBms2Current.setText(String.format(Locale.US, "%.2f A", WheelData.getInstance().getbms2Current()));
                tvBms2Voltage.setText(String.format(Locale.US, "%.2f V", WheelData.getInstance().getbms2Voltage()));
                tvBms2Temp1.setText(String.format(Locale.US, "%d°C", WheelData.getInstance().getbms2Temp1()));
                tvBms2Temp2.setText(String.format(Locale.US, "%d°C", WheelData.getInstance().getbms2Temp2()));
                tvBms2Health.setText(String.format(Locale.US, "%d %%", WheelData.getInstance().getbms2Health()));
                balanceMap = WheelData.getInstance().getbms2BalanceMap();
                if (((balanceMap) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell1.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell1(), bal));
                if (((balanceMap >> 1) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell2.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell2(), bal));
                if (((balanceMap >> 2) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell3.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell3(), bal));
                if (((balanceMap >> 3) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell4.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell4(), bal));
                if (((balanceMap >> 4) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell5.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell5(), bal));
                if (((balanceMap >> 5) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell6.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell6(), bal));
                if (((balanceMap >> 6) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell7.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell7(), bal));
                if (((balanceMap >> 7) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell8.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell8(), bal));
                if (((balanceMap >> 8) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell9.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell9(), bal));
                if (((balanceMap >> 9) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell10.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell10(), bal));
                if (((balanceMap >> 10) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell11.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell11(), bal));
                if (((balanceMap >> 11) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell12.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell12(), bal));
                if (((balanceMap >> 12) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell13.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell13(), bal));
                if (((balanceMap >> 13) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell14.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell14(), bal));
                if (WheelData.getInstance().getbms2Cell15() == 0.0) {
                    tvBms2Cell15.setVisibility(View.GONE);
                    tvTitleBms2Cell15.setVisibility(View.GONE);
                } else {
                    tvBms2Cell15.setVisibility(View.VISIBLE);
                    tvTitleBms2Cell15.setVisibility(View.VISIBLE);
                }
                if (WheelData.getInstance().getbms2Cell16() == 0.0) {
                    tvBms2Cell16.setVisibility(View.GONE);
                    tvTitleBms2Cell16.setVisibility(View.GONE);
                } else {
                    tvBms2Cell16.setVisibility(View.VISIBLE);
                    tvTitleBms2Cell16.setVisibility(View.VISIBLE);
                }
                if (((balanceMap >> 14) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell15.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell15(), bal));
                if (((balanceMap >> 15) & 0x01) == 1) bal = "[B]"; else bal = "";
                tvBms2Cell16.setText(String.format(Locale.US, "%.3f V %s", WheelData.getInstance().getbms2Cell16(), bal));

        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateScreen(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            String message = "[ec] " + method + " ok: " + success;
            Timber.i(message);
            MainActivity.this.runOnUiThread(() -> showSnackBar(message, 4000));
            return null;
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_frame, getPreferencesFragment(), Constants.PREFERENCES_FRAGMENT_TAG)
                .commit();

        ViewPageAdapter adapter = new ViewPageAdapter(this);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(4);

        LinePageIndicator titleIndicator = (LinePageIndicator) findViewById(R.id.indicator);
        titleIndicator.setViewPager(pager);
        pager.addOnPageChangeListener(pageChangeListener);

        mDeviceAddress = SettingsUtil.getLastAddress(getApplicationContext());
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        tvSpeed = (TextView) findViewById(R.id.tvSpeed);
        tvCurrent = (TextView) findViewById(R.id.tvCurrent);
        tvPower = (TextView) findViewById(R.id.tvPower);
        tvTemperature = (TextView) findViewById(R.id.tvTemperature);
        tvTemperature2 = (TextView) findViewById(R.id.tvTemperature2);
        tvAngle = (TextView) findViewById(R.id.tvAngle);
        tvRoll = (TextView) findViewById(R.id.tvRoll);
        tvVoltage = (TextView) findViewById(R.id.tvVoltage);
        tvVoltageSag = (TextView) findViewById(R.id.tvVoltageSag);
        tvBattery = (TextView) findViewById(R.id.tvBattery);
        tvFanStatus = (TextView) findViewById(R.id.tvFanStatus);
        tvChargingStatus = (TextView) findViewById(R.id.tvChargingStatus);
        tvOutput = (TextView) findViewById(R.id.tvOutput);
        tvCpuLoad = (TextView) findViewById(R.id.tvCpuLoad);
        tvTopSpeed = (TextView) findViewById(R.id.tvTopSpeed);
        tvAverageSpeed = (TextView) findViewById(R.id.tvAverageSpeed);
        tvAverageRidingSpeed = (TextView) findViewById(R.id.tvAverageRidingSpeed);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        tvWheelDistance = (TextView) findViewById(R.id.tvWheelDistance);
        tvUserDistance = (TextView) findViewById(R.id.tvUserDistance);
        tvTotalDistance = (TextView) findViewById(R.id.tvTotalDistance);
        tvModel = (TextView) findViewById(R.id.tvModel);
        tvName = (TextView) findViewById(R.id.tvName);
        tvVersion = (TextView) findViewById(R.id.tvVersion);
        tvSerial = (TextView) findViewById(R.id.tvSerial);
        tvRideTime = (TextView) findViewById(R.id.tvRideTime);
        tvRidingTime = (TextView) findViewById(R.id.tvRidingTime);
        tvMode = (TextView) findViewById(R.id.tvMode);
        wheelView = (WheelView) findViewById(R.id.wheelView);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        tvBmsWaitText = (TextView) findViewById(R.id.tvBmsWaitText);
        tvTitleBmsBattery1 = (TextView) findViewById(R.id.tvTitleBmsBattery1);
        tvTitleBmsBattery2 = (TextView) findViewById(R.id.tvTitleBmsBattery2);
        tvTitleBms1Sn = (TextView) findViewById(R.id.tvTitleBms1Sn);
        tvBms1Sn = (TextView) findViewById(R.id.tvBms1Sn);
        tvTitleBms2Sn = (TextView) findViewById(R.id.tvTitleBms2Sn);
        tvBms2Sn = (TextView) findViewById(R.id.tvBms2Sn);
        tvTitleBms1Fw = (TextView) findViewById(R.id.tvTitleBms1Fw);
        tvBms1Fw = (TextView) findViewById(R.id.tvBms1Fw);
        tvTitleBms2Fw = (TextView) findViewById(R.id.tvTitleBms2Fw);
        tvBms2Fw = (TextView) findViewById(R.id.tvBms2Fw);
        tvTitleBms1FactoryCap = (TextView) findViewById(R.id.tvTitleBms1FactoryCap);
        tvBms1FactoryCap = (TextView) findViewById(R.id.tvBms1FactoryCap);
        tvTitleBms2FactoryCap = (TextView) findViewById(R.id.tvTitleBms2FactoryCap);
        tvBms2FactoryCap = (TextView) findViewById(R.id.tvBms2FactoryCap);
        tvTitleBms1ActualCap = (TextView) findViewById(R.id.tvTitleBms1ActualCap);
        tvBms1ActualCap = (TextView) findViewById(R.id.tvBms1ActualCap);
        tvTitleBms2ActualCap = (TextView) findViewById(R.id.tvTitleBms2ActualCap);
        tvBms2ActualCap = (TextView) findViewById(R.id.tvBms2ActualCap);
        tvTitleBms1Cycles = (TextView) findViewById(R.id.tvTitleBms1Cycles);
        tvBms1Cycles = (TextView) findViewById(R.id.tvBms1Cycles);
        tvTitleBms2Cycles = (TextView) findViewById(R.id.tvTitleBms2Cycles);
        tvBms2Cycles = (TextView) findViewById(R.id.tvBms2Cycles);
        tvTitleBms1ChrgCount = (TextView) findViewById(R.id.tvTitleBms1ChrgCount);
        tvBms1ChrgCount = (TextView) findViewById(R.id.tvBms1ChrgCount);
        tvTitleBms2ChrgCount = (TextView) findViewById(R.id.tvTitleBms2ChrgCount);
        tvBms2ChrgCount = (TextView) findViewById(R.id.tvBms2ChrgCount);
        tvTitleBms1MfgDate = (TextView) findViewById(R.id.tvTitleBms1MfgDate);
        tvBms1MfgDate = (TextView) findViewById(R.id.tvBms1MfgDate);
        tvTitleBms2MfgDate = (TextView) findViewById(R.id.tvTitleBms2MfgDate);
        tvBms2MfgDate = (TextView) findViewById(R.id.tvBms2MfgDate);
        tvTitleBms1Status = (TextView) findViewById(R.id.tvTitleBms1Status);
        tvBms1Status = (TextView) findViewById(R.id.tvBms1Status);
        tvTitleBms2Status = (TextView) findViewById(R.id.tvTitleBms2Status);
        tvBms2Status = (TextView) findViewById(R.id.tvBms2Status);
        tvTitleBms1RemCap = (TextView) findViewById(R.id.tvTitleBms1RemCap);
        tvBms1RemCap = (TextView) findViewById(R.id.tvBms1RemCap);
        tvTitleBms2RemCap = (TextView) findViewById(R.id.tvTitleBms2RemCap);
        tvBms2RemCap = (TextView) findViewById(R.id.tvBms2RemCap);
        tvTitleBms1RemPerc = (TextView) findViewById(R.id.tvTitleBms1RemPerc);
        tvBms1RemPerc = (TextView) findViewById(R.id.tvBms1RemPerc);
        tvTitleBms2RemPerc = (TextView) findViewById(R.id.tvTitleBms2RemPerc);
        tvBms2RemPerc = (TextView) findViewById(R.id.tvBms2RemPerc);
        tvTitleBms1Current = (TextView) findViewById(R.id.tvTitleBms1Current);
        tvBms1Current = (TextView) findViewById(R.id.tvBms1Current);
        tvTitleBms2Current = (TextView) findViewById(R.id.tvTitleBms2Current);
        tvBms2Current = (TextView) findViewById(R.id.tvBms2Current);
        tvTitleBms1Voltage = (TextView) findViewById(R.id.tvTitleBms1Voltage);
        tvBms1Voltage = (TextView) findViewById(R.id.tvBms1Voltage);
        tvTitleBms2Voltage = (TextView) findViewById(R.id.tvTitleBms2Voltage);
        tvBms2Voltage = (TextView) findViewById(R.id.tvBms2Voltage);
        tvTitleBms1Temp1 = (TextView) findViewById(R.id.tvTitleBms1Temp1);
        tvBms1Temp1 = (TextView) findViewById(R.id.tvBms1Temp1);
        tvTitleBms2Temp1 = (TextView) findViewById(R.id.tvTitleBms2Temp1);
        tvBms2Temp1 = (TextView) findViewById(R.id.tvBms2Temp1);
        tvTitleBms1Temp2 = (TextView) findViewById(R.id.tvTitleBms1Temp2);
        tvBms1Temp2 = (TextView) findViewById(R.id.tvBms1Temp2);
        tvTitleBms2Temp2 = (TextView) findViewById(R.id.tvTitleBms2Temp2);
        tvBms2Temp2 = (TextView) findViewById(R.id.tvBms2Temp2);
        tvTitleBms1Health = (TextView) findViewById(R.id.tvTitleBms1Health);
        tvBms1Health = (TextView) findViewById(R.id.tvBms1Health);
        tvTitleBms2Health = (TextView) findViewById(R.id.tvTitleBms2Health);
        tvBms2Health = (TextView) findViewById(R.id.tvBms2Health);
        tvTitleBms1Cell1 = (TextView) findViewById(R.id.tvTitleBms1Cell1);
        tvBms1Cell1 = (TextView) findViewById(R.id.tvBms1Cell1);
        tvTitleBms2Cell1 = (TextView) findViewById(R.id.tvTitleBms2Cell1);
        tvBms2Cell1 = (TextView) findViewById(R.id.tvBms2Cell1);
        tvTitleBms1Cell2 = (TextView) findViewById(R.id.tvTitleBms1Cell2);
        tvBms1Cell2 = (TextView) findViewById(R.id.tvBms1Cell2);
        tvTitleBms2Cell2 = (TextView) findViewById(R.id.tvTitleBms2Cell2);
        tvBms2Cell2 = (TextView) findViewById(R.id.tvBms2Cell2);
        tvTitleBms1Cell3 = (TextView) findViewById(R.id.tvTitleBms1Cell3);
        tvBms1Cell3 = (TextView) findViewById(R.id.tvBms1Cell3);
        tvTitleBms2Cell3 = (TextView) findViewById(R.id.tvTitleBms2Cell3);
        tvBms2Cell3 = (TextView) findViewById(R.id.tvBms2Cell3);
        tvTitleBms1Cell4 = (TextView) findViewById(R.id.tvTitleBms1Cell4);
        tvBms1Cell4 = (TextView) findViewById(R.id.tvBms1Cell4);
        tvTitleBms2Cell4 = (TextView) findViewById(R.id.tvTitleBms2Cell4);
        tvBms2Cell4 = (TextView) findViewById(R.id.tvBms2Cell4);
        tvTitleBms1Cell5 = (TextView) findViewById(R.id.tvTitleBms1Cell5);
        tvBms1Cell5 = (TextView) findViewById(R.id.tvBms1Cell5);
        tvTitleBms2Cell5 = (TextView) findViewById(R.id.tvTitleBms2Cell5);
        tvBms2Cell5 = (TextView) findViewById(R.id.tvBms2Cell5);
        tvTitleBms1Cell6 = (TextView) findViewById(R.id.tvTitleBms1Cell6);
        tvBms1Cell6 = (TextView) findViewById(R.id.tvBms1Cell6);
        tvTitleBms2Cell6 = (TextView) findViewById(R.id.tvTitleBms2Cell6);
        tvBms2Cell6 = (TextView) findViewById(R.id.tvBms2Cell6);
        tvTitleBms1Cell7 = (TextView) findViewById(R.id.tvTitleBms1Cell7);
        tvBms1Cell7 = (TextView) findViewById(R.id.tvBms1Cell7);
        tvTitleBms2Cell7 = (TextView) findViewById(R.id.tvTitleBms2Cell7);
        tvBms2Cell7 = (TextView) findViewById(R.id.tvBms2Cell7);
        tvTitleBms1Cell8 = (TextView) findViewById(R.id.tvTitleBms1Cell8);
        tvBms1Cell8 = (TextView) findViewById(R.id.tvBms1Cell8);
        tvTitleBms2Cell8 = (TextView) findViewById(R.id.tvTitleBms2Cell8);
        tvBms2Cell8 = (TextView) findViewById(R.id.tvBms2Cell8);
        tvTitleBms1Cell9 = (TextView) findViewById(R.id.tvTitleBms1Cell9);
        tvBms1Cell9 = (TextView) findViewById(R.id.tvBms1Cell9);
        tvTitleBms2Cell9 = (TextView) findViewById(R.id.tvTitleBms2Cell9);
        tvBms2Cell9 = (TextView) findViewById(R.id.tvBms2Cell9);
        tvTitleBms1Cell10 = (TextView) findViewById(R.id.tvTitleBms1Cell10);
        tvBms1Cell10 = (TextView) findViewById(R.id.tvBms1Cell10);
        tvTitleBms2Cell10 = (TextView) findViewById(R.id.tvTitleBms2Cell10);
        tvBms2Cell10 = (TextView) findViewById(R.id.tvBms2Cell10);
        tvTitleBms1Cell11 = (TextView) findViewById(R.id.tvTitleBms1Cell11);
        tvBms1Cell11 = (TextView) findViewById(R.id.tvBms1Cell11);
        tvTitleBms2Cell11 = (TextView) findViewById(R.id.tvTitleBms2Cell11);
        tvBms2Cell11 = (TextView) findViewById(R.id.tvBms2Cell11);
        tvTitleBms1Cell12 = (TextView) findViewById(R.id.tvTitleBms1Cell12);
        tvBms1Cell12 = (TextView) findViewById(R.id.tvBms1Cell12);
        tvTitleBms2Cell12 = (TextView) findViewById(R.id.tvTitleBms2Cell12);
        tvBms2Cell12 = (TextView) findViewById(R.id.tvBms2Cell12);
        tvTitleBms1Cell13 = (TextView) findViewById(R.id.tvTitleBms1Cell13);
        tvBms1Cell13 = (TextView) findViewById(R.id.tvBms1Cell13);
        tvTitleBms2Cell13 = (TextView) findViewById(R.id.tvTitleBms2Cell13);
        tvBms2Cell13 = (TextView) findViewById(R.id.tvBms2Cell13);
        tvTitleBms1Cell14 = (TextView) findViewById(R.id.tvTitleBms1Cell14);
        tvBms1Cell14 = (TextView) findViewById(R.id.tvBms1Cell14);
        tvTitleBms2Cell14 = (TextView) findViewById(R.id.tvTitleBms2Cell14);
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
                ((MainPreferencesFragment) getPreferencesFragment()).showMainMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        Typeface typefacePrime = Typefaces.get(this, "fonts/prime.otf");
        TextClock textClock = (TextClock) findViewById(R.id.textClock);
        TextView tvWaitText = (TextView) findViewById(R.id.tvWaitText);
        textClock.setTypeface(typefacePrime);
        tvWaitText.setTypeface(typefacePrime);

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

        loadPreferences(true);

        if (SettingsUtil.isFirstRun(this)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawer.openDrawer(GravityCompat.START, true);
                }
            }, 1000);
        }

//        if (SettingsUtil.isUseENG(this)) {
        //          if WheelLog.localeManager.getLocale(this, language);
//            setNewLocale("en",false);
//        }

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
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
                if (SettingsUtil.getGarminConnectIQEnable(this))
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
                    if (((MainPreferencesFragment) getPreferencesFragment()).isMainMenu())
                        mDrawer.closeDrawer(GravityCompat.START, true);
                    else ((MainPreferencesFragment) getPreferencesFragment()).showMainMenu();
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

    ViewPager.SimpleOnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            viewPagerPage = position;
            updateScreen(true);
        }
    };

    private void loadPreferences(boolean requests) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        use_mph = sharedPreferences.getBoolean(getString(R.string.use_mph), false);

        Set<String> view_blocks = sharedPreferences.getStringSet(getString(R.string.view_blocks), null);
        int max_speed = sharedPreferences.getInt(getString(R.string.max_speed), 30) * 10;
        wheelView.setMaxSpeed(max_speed);
        wheelView.setUseMPH(use_mph);
 /*       if (view_blocks != null) {
            wheelView.updateViewBlocksVisibility(view_blocks);
        }
*/

        if (view_blocks== null) {
            Set<String> view_blocks_def = new HashSet<String>();
            view_blocks_def.add(getString(R.string.voltage));
            view_blocks_def.add(getString(R.string.average_riding_speed));
            view_blocks_def.add(getString(R.string.riding_time));
            view_blocks_def.add(getString(R.string.top_speed));
            view_blocks_def.add(getString(R.string.distance));
            view_blocks_def.add(getString(R.string.total));
            wheelView.updateViewBlocksVisibility(view_blocks_def);
        } else {
            wheelView.updateViewBlocksVisibility(view_blocks);
        }
//*/

        boolean alarms_enabled = sharedPreferences.getBoolean(getString(R.string.alarms_enabled), false);
        boolean use_ratio = sharedPreferences.getBoolean(getString(R.string.use_ratio), false);
        WheelData.getInstance().setUseRatio(use_ratio);
        boolean ks18l_scaler = sharedPreferences.getBoolean(getString(R.string.ks18l_scaler), false);
        WheelData.getInstance().set18Lkm(ks18l_scaler);

        boolean betterPercents = sharedPreferences.getBoolean(getString(R.string.use_better_percents), false);
        WheelData.getInstance().setBetterPercents(betterPercents);
        wheelView.setBetterPercent(betterPercents);

        boolean fixedPercents = sharedPreferences.getBoolean(getString(R.string.fixed_percents), false);
        WheelData.getInstance().setFixedPercents(fixedPercents);
        wheelView.setFixedPercents(fixedPercents);

        boolean useStopMusic = sharedPreferences.getBoolean(getString(R.string.use_stop_music), false);
        WheelData.getInstance().setUseStopMusic(useStopMusic);

        boolean currentOnDial = sharedPreferences.getBoolean(getString(R.string.current_on_dial), false);
        wheelView.setCurrentOnDial(currentOnDial);
        wheelView.invalidate();
        final boolean connectSound = sharedPreferences.getBoolean(getString(R.string.connection_sound), false);
        final int beepPeriod = sharedPreferences.getInt(getString(R.string.no_connection_sound), 0) * 1000;
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setConnectionSounds(connectSound, beepPeriod);
        }

        int gotway_voltage = Integer.parseInt(sharedPreferences.getString(getString(R.string.gotway_voltage), "1"));
        int gotway_negative = Integer.parseInt(sharedPreferences.getString(getString(R.string.gotway_negative), "0"));
        GotwayAdapter.getInstance().setGotwayVoltageScaler(gotway_voltage);
        GotwayAdapter.getInstance().setGotwayNegative(gotway_negative);

        // Set tiltback voltage with check gotway voltage changes
        double tiltbackVoltage = (float)sharedPreferences.getInt(getString(R.string.tiltback_voltage), 660) / 100;
        double correctedTiltbackVoltage = tiltbackVoltage;
        if (gotway_voltage == 0 && (tiltbackVoltage > 52.8 || tiltbackVoltage < 48))
            correctedTiltbackVoltage = 52.8;
        else if (gotway_voltage == 1 && (tiltbackVoltage > 66 || tiltbackVoltage < 60))
            correctedTiltbackVoltage = 66;
        else if (gotway_voltage == 2 && (tiltbackVoltage > 79.2 || tiltbackVoltage < 72))
            correctedTiltbackVoltage = 79.2;

        if (correctedTiltbackVoltage != tiltbackVoltage) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.tiltback_voltage), (int)(correctedTiltbackVoltage * 10));
            editor.commit();
        }

        WheelData.getInstance().setTiltbackVoltage(correctedTiltbackVoltage);

        //boolean gotway_84v = sharedPreferences.getBoolean(getString(R.string.gotway_84v), false);
        //WheelData.getInstance().setGotway84V(gotway_84v);
        WheelData.getInstance().setAlarmsEnabled(alarms_enabled);

        if (alarms_enabled) {
            int alarm1Speed = sharedPreferences.getInt(getString(R.string.alarm_1_speed), 29);
            int alarm2Speed = sharedPreferences.getInt(getString(R.string.alarm_2_speed), 0);
            int alarm3Speed = sharedPreferences.getInt(getString(R.string.alarm_3_speed), 0);
            int alarm1Battery = sharedPreferences.getInt(getString(R.string.alarm_1_battery), 100);
            int alarm2Battery = sharedPreferences.getInt(getString(R.string.alarm_2_battery), 0);
            int alarm3Battery = sharedPreferences.getInt(getString(R.string.alarm_3_battery), 0);
            int current_alarm = sharedPreferences.getInt(getString(R.string.alarm_current), 0);
            int temperature_alarm = sharedPreferences.getInt(getString(R.string.alarm_temperature), 0);
            boolean disablePhoneVibrate = sharedPreferences.getBoolean(getString(R.string.disable_phone_vibrate), false);
            boolean disablePhoneBeep = sharedPreferences.getBoolean(getString(R.string.disable_phone_beep), false);
            boolean alteredAlarms = sharedPreferences.getBoolean(getString(R.string.altered_alarms), false);
            int rotationSpeed = sharedPreferences.getInt(getString(R.string.rotation_speed), 500);
            int rotationVoltage = sharedPreferences.getInt(getString(R.string.rotation_voltage), 840);
            int powerFactor = sharedPreferences.getInt(getString(R.string.power_factor), 90);
            int alarmFactor1 = sharedPreferences.getInt(getString(R.string.alarm_factor1), 80);
            int alarmFactor2 = sharedPreferences.getInt(getString(R.string.alarm_factor2), 90);
            int alarmFactor3 = sharedPreferences.getInt(getString(R.string.alarm_factor3), 95);
            int warningSpeed = sharedPreferences.getInt(getString(R.string.warning_speed), 0);
            int warningPwm = sharedPreferences.getInt(getString(R.string.warning_pwm), 0);
            int warningSpeedPeriod = sharedPreferences.getInt(getString(R.string.warning_speed_period), 0);
            WheelData.getInstance().setPreferences(
                    alarm1Speed, alarm1Battery,
                    alarm2Speed, alarm2Battery,
                    alarm3Speed, alarm3Battery,
                    current_alarm, temperature_alarm, disablePhoneVibrate, disablePhoneBeep, alteredAlarms,
                    rotationSpeed, rotationVoltage, powerFactor, alarmFactor1, alarmFactor2, alarmFactor3, warningSpeed, warningSpeedPeriod, warningPwm);
            wheelView.setWarningSpeed(alarm1Speed, alteredAlarms);
        } else
            wheelView.setWarningSpeed(0, false);

        boolean auto_log = sharedPreferences.getBoolean(getString(R.string.auto_log), false);
        boolean log_location = sharedPreferences.getBoolean(getString(R.string.log_location_data), false);
        boolean auto_upload_ec = sharedPreferences.getBoolean(getString(R.string.auto_upload_ec), false);
        ElectroClub.getInstance().setUserToken(SettingsUtil.getECToken(this));
        ElectroClub.getInstance().setUserId(SettingsUtil.getECUserId(this));

        if (auto_log && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            MainActivityPermissionsDispatcher.acquireStoragePermissionWithCheck(this);

        if (log_location)
            MainActivityPermissionsDispatcher.acquireLocationPermissionWithCheck(this);

        if (requests) {
            if (auto_upload_ec) {
                if (ElectroClub.getInstance().getUserToken() == null) {
                    startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), RESULT_AUTH_REQUEST);
                } else {
                    ElectroClub.getInstance().getAndSelectGarageByMacOrPrimary(mDeviceAddress, s -> null);
                    // TODO check user token
                }
            } else {
                // TODO: need to implement a logout
                // logout after uncheck
                ElectroClub.getInstance().setUserToken(null);
                ElectroClub.getInstance().setUserId(null);
                SettingsUtil.setECToken(this, null);
            }
        }

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
        SettingsUtil.setAutoLog(this, false);
        ((MainPreferencesFragment) getPreferencesFragment()).refreshVolatileSettings();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void locationPermissionDenied() {
        SettingsUtil.setLogLocationEnabled(this, false);
        ((MainPreferencesFragment) getPreferencesFragment()).refreshVolatileSettings();
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
    }

    private void hideSnackBar() {
        if (snackbar == null)
            return;

        snackbar.dismiss();
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
        else
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
                    ElectroClub.getInstance().getAndSelectGarageByMacOrPrimary(
                            mDeviceAddress,
                            success -> null);
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
                    SettingsUtil.setECToken(this, ElectroClub.getInstance().getUserToken());
                    SettingsUtil.setECUserId(this, ElectroClub.getInstance().getUserId());
                    ElectroClub.getInstance().getAndSelectGarageByMacOrPrimary(mDeviceAddress, s -> null);
                } else {
                    SettingsUtil.setAutoUploadECEnabled(this, false);
                    SettingsUtil.setECToken(this, null);
                    SettingsUtil.setECUserId(this, null);
                    ((MainPreferencesFragment) getPreferencesFragment()).refreshVolatileSettings();
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

    private Fragment getPreferencesFragment() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(Constants.PREFERENCES_FRAGMENT_TAG);
        if (frag == null) {
            return new MainPreferencesFragment();
        }
        return frag;
    }
}