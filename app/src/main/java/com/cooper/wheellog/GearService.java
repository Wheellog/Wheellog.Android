package com.cooper.wheellog;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.NotificationUtil;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import static java.lang.String.format;


public class GearService extends SAAgent {
    GearBinder mBinder = new GearBinder();
    public final static String TAG = "GearService";
    public final static int SAP_SERVICE_CHANNEL_ID = 142; //Same as in sapservices.xml on both sides
    AbstractCollection<GearSAPServiceProviderConnection> mConnectionBag = new Vector<GearSAPServiceProviderConnection>();
    LocationManager mLocationManager;
    boolean mIsListening = false;
    private Timer keepAliveTimer;
    private Notification mNotification;

    public class GearBinder extends Binder {
        GearService getService() {
            return GearService.this;
        }
    }

    public GearService() {
        super(TAG, GearSAPServiceProviderConnection.class);
//        android.os.Debug.waitForDebugger();  // this line is key for debugging (run and attach debugger)
        Log.d(TAG, "Service instantiated");
    }

LocationListener locationListener = new LocationListener() {

    long mTime;
    float   mBearing, mSpeed;
    double mLatitude, mLongitude, mAltitude;
    boolean bHasAltitude, bHasBearing, bHasSpeed;
    boolean bGpsEnabled = true;

    @Override
    public  String toString() {
        //In general this isn't how I would encode something in JSON, but the amount
        //of data is small enough such that I've decided to use String.Format to
        //produce what's needed.
        final String returnValue =
                format(Locale.ROOT,"\"gpsEnabled\" :%b,"+
                                "\"hasSpeed\":%b, \"gpsSpeed\":%1.2f, \"hasBearing\":%b, \"bearing\":%1.4f,"+
                                "\"latitude\":%f, \"longitude\":%f,\"hasAltitude\":%b, \"altitude\":%1.3f",
                        bGpsEnabled,
                        bHasSpeed, mSpeed,
                        bHasBearing, mBearing,
                        mLatitude, mLongitude,
                        bHasAltitude, mAltitude
                );
        return returnValue;
    }
    @Override
    public void onLocationChanged(Location location) {
        if(bHasSpeed = location.hasSpeed())
            mSpeed = location.getSpeed();
        if(bHasAltitude = location.hasAltitude())
            mAltitude = location.getAltitude();
        if(location.hasSpeed())
            mSpeed = location.getSpeed();
        if(bHasBearing = location.hasBearing())
            mBearing = location.getBearing();
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mTime = location.getTime();
//        transmitMessage(); Me lo he llevado a la rutina que se ejecuta de forma temporizada
    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
        bGpsEnabled = true;
    }

    @Override
    public void onProviderDisabled(String s) {
        bGpsEnabled = false;
    }
};

    public void transmitMessage(String sendingString) {
        byte[] sendingMessage = sendingString.getBytes();

        Log.i(TAG, sendingString);

        Iterator connectionIterator = mConnectionBag.iterator();
        while(connectionIterator.hasNext()) {
            GearSAPServiceProviderConnection connection = (GearSAPServiceProviderConnection)connectionIterator.next();
            try {
                connection.send(SAP_SERVICE_CHANNEL_ID, sendingMessage);
            } catch(IOException exc) {
                //
            }
        }
    }

    public void startKeepAliveTimer() { //Se le pueden pasar parámetros
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                String message;
                if (WheelData.getInstance()!=null) {
                    message = String.format(Locale.ROOT, "{ \"speed\":%.2f," +
                                    "\"voltage\":%.2f,\"current\":%.2f,\"power\":%.2f," +
                                    "\"batteryLevel\":%d,\"distance\":%d,\"totalDistance\":%d,\"temperature\":%d," +
                                    "\"temperature2\":%d," +
                                    "\"angle\":%.2f,\"roll\":%.2f,\"isAlarmExecuting\":%d",
//                        "\"mode\":%s,\"alert\":%s"+
                            WheelData.getInstance().getSpeedDouble(),
                            WheelData.getInstance().getVoltageDouble(),
                            WheelData.getInstance().getCurrentDouble(),
                            WheelData.getInstance().getPowerDouble(),
                            WheelData.getInstance().getBatteryLevel(),
                            WheelData.getInstance().getDistance(),
                            WheelData.getInstance().getTotalDistance(),
                            WheelData.getInstance().getTemperature(),
                            WheelData.getInstance().getTemperature2(),
                            WheelData.getInstance().getAngle(),
                            WheelData.getInstance().getRoll(),
                            WheelData.getInstance().getAlarm()
//                        WheelData.getInstance().getModeStr(),
//                        WheelData.getInstance().getAlert()
                    );
                } else {
                    message = "{";
                }
                if(locationListener!=null) {
                    if (WheelData.getInstance()!=null) {
                    message = message + "," + getLocationMessage();
                    } else{
                    message = getLocationMessage();
                    }
                }
                message = message + "}";
                transmitMessage(message);
            }
        };
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(timerTask, 0, 200); //cada 500ms
    }

    public void removeConnection(GearSAPServiceProviderConnection connection) {
        mConnectionBag.remove(connection);
        reevaluateNeedToSend();
    }

    public void addConnection(GearSAPServiceProviderConnection connection) {
        mConnectionBag.add(connection);
        transmitMessage("Mensaje inicial");
            //onServiceConnectionResponse also calls reevaluateNeedTOSend, so there is no need to transmit anything now
            //No entiendo por qué manda un primer mensaje al conectar pero lo dejo por si evita timeouts.
    }

    public String getLocationMessage() {
        if(locationListener==null) {
            return "";
        }
        return locationListener.toString();
    }


    void startSendingData() {
        if(!mIsListening) {
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);

            if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
                mIsListening = true;
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
            }
            startKeepAliveTimer(); //Location retreived each second, but data sent twice a second.
        }
    }

    void stopSendingData() {
        if(mIsListening) {
            mLocationManager.removeUpdates(locationListener);
            mIsListening = false;
            keepAliveTimer.cancel();
            keepAliveTimer = null;
        }
    }

    void reevaluateNeedToSend() {
        if(mConnectionBag.size()==0)
            stopSendingData();
        else
            startSendingData();
    }

    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent agent, int i) {
    }

    @Override
//    protected void onServiceConnectionResponse(SASocket currentConnection, int result) {
    protected void onServiceConnectionResponse(SAPeerAgent agent, SASocket currentConnection, int result) {
        super.onServiceConnectionResponse(agent, currentConnection, result);
        if(result == CONNECTION_SUCCESS) {
            if(currentConnection != null){
                GearSAPServiceProviderConnection connection = (GearSAPServiceProviderConnection) currentConnection;
                connection.setParent(this);
                addConnection(connection);
                Toast.makeText(getBaseContext(), "GEAR CONNECTION ESTABLISHED", Toast.LENGTH_LONG).show();
                reevaluateNeedToSend(); //We start sending when watch connects
            } else {
                Log.e(TAG, "Connection object is null.");
            }
        } else if (result == CONNECTION_ALREADY_EXIST) {
            Log.e(TAG, "CONNECTION_ALREADY_EXISTS");
        } else {
            Log.e(TAG, "connection error result" + result);
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent agent) {
        acceptServiceConnectionRequest(agent);
        Log.i(TAG, "Accepting connection"); //The watch initiates always the connection
    }

    @Override
    public void onCreate() {
      //  startForeground();
        super.onCreate();
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        SA accessory = new SA();
        try {
            accessory.initialize(this); //Y expect this to do nothing for non-Samsung devices
        }
        catch (SsdkUnsupportedException exc) {
            Log.e(TAG, "Unsupported SDK");
        }
        catch(Exception exc) {
            Log.e(TAG, "initialization failed");
            exc.printStackTrace();
            stopSelf();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Toast.makeText(getBaseContext(), "Gear Service started", Toast.LENGTH_LONG).show();
        Log.i(TAG, "started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
    }

}
