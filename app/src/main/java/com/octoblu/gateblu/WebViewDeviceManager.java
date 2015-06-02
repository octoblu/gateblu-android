package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.MissingResourceException;

public class WebViewDeviceManager implements DeviceManager {

    public static final String TAG = "WebViewDeviceManager";
    private final Context context;
    private final HashMap<String, WebViewDevice> devicesMap;
    private final Handler uiThreadHandler;

    public WebViewDeviceManager(Context context, Handler uiThreadHandler) {
        this.context = context;
        this.uiThreadHandler = uiThreadHandler;
        this.devicesMap = new HashMap<>();
    }

    @Override
    public void addDevice(final JSONObject data) {
        Log.i(TAG, "addDevice: " + data.toString());

        WebViewDevice device;
        try {
            device = WebViewDevice.fromJSONObject(data, context);
        } catch (MissingResourceException exception) {
            Log.e(TAG, exception.getMessage(), exception);
            return;
        }

        devicesMap.put(device.getUuid(), device);
    }

    @Override
    public void removeDevice(JSONObject data) {
        Log.i(TAG, "removeDevice: " + data.toString());
        String uuid;
        try {
            uuid = SaneJSONObject.fromJSONObject(data).getStringOrThrow("uuid");
        } catch (MissingResourceException exception) {
            Log.e(TAG, exception.getMessage(), exception);
            return;
        }

        WebViewDevice device = devicesMap.remove(uuid);
        if(device == null) {
            return;
        }
        device.stop();
    }

    @Override
    public void startDevice(JSONObject data) {
        Log.i(TAG, "startDevice: " + data.toString());
        final String uuid;
        try {
            uuid = SaneJSONObject.fromJSONObject(data).getStringOrThrow("uuid");
        } catch (MissingResourceException exception) {
            Log.e(TAG, exception.getMessage(), exception);
            return;
        }

        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                devicesMap.get(uuid).start();
            }
        });
    }

    @Override
    public void stopDevice(JSONObject data) {
        Log.i(TAG, "stopDevice");
        String uuid;
        try {
            uuid = SaneJSONObject.fromJSONObject(data).getStringOrThrow("uuid");
        } catch (MissingResourceException exception) {
            Log.e(TAG, exception.getMessage(), exception);
            return;
        }
        stopDevice(uuid);
    }

    private void stopDevice(final String uuid) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                devicesMap.get(uuid).stop();
            }
        });
    }

    @Override
    public void stopAll() {
        for(String uuid : devicesMap.keySet()) {
            stopDevice(uuid);
        }
        devicesMap.clear();
    }
}
