package com.octoblu.gateblu;

import android.util.Log;

public class WebViewDeviceManager implements DeviceManager {

    public static final String TAG = "WebViewDeviceManager";

    @Override
    public void addDevice() {
        Log.i(TAG, "addDevice");
    }

    @Override
    public void removeDevice() {
        Log.i(TAG, "removeDevice");
    }

    @Override
    public void startDevice() {
        Log.i(TAG, "startDevice");
    }

    @Override
    public void stopDevice() {
        Log.i(TAG, "stopDevice");
    }
}
