package com.cooper.wheellog;

import android.util.Log;

import com.samsung.android.sdk.accessory.SASocket;


public class GearSAPServiceProviderConnection extends SASocket {
    private int connectionID;
    static int nextID = 1;
    public final static String TAG = "SAPServiceProvider";
    private GearService mParent;

    public void setParent(GearService gearService) {
        mParent = gearService;
        Log.d(TAG,"Set Parent");
    }

    public GearSAPServiceProviderConnection() {
        super(GearSAPServiceProviderConnection.class.getName());
        connectionID = ++nextID;
        Log.d(TAG,"GearSAPServiceProviderConnection");
    }

    @Override
    protected void onServiceConnectionLost(int reason) {
        if(mParent!=null) {
            mParent.removeConnection(this);;
        }
        Log.d(TAG,"Set OnServiceConnectionLost");
    }

    @Override
    public void onReceive(int channelID, byte[] data) {
        Log.d(TAG,"OnReceive");
    }

    @Override
    public void onError(int channelID, String errorString, int errorCode) {
        Log.e(TAG,"ERROR:"+errorString+ " | " + errorCode);
    }
}
