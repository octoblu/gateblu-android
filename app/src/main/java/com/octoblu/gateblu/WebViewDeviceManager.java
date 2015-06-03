package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;

public class WebViewDeviceManager extends Emitter implements DeviceManager {

    public static final String TAG = "WebViewDeviceManager";
    private final Context context;
    private final HashMap<String, WebViewDevice> devicesMap;
    private final Handler uiThreadHandler;
    private boolean ready = false;

    public WebViewDeviceManager(Context context, Handler uiThreadHandler) {
        this.context = context;
        this.uiThreadHandler = uiThreadHandler;
        this.devicesMap = new HashMap<>();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setReady(boolean ready) {
        this.ready = ready;
        emit(CONFIG);
    }

    @Override
    public List<Device> getDevices() {
        List<Device> devices = new ArrayList<>(devicesMap.size());
        for(WebViewDevice webViewDevice : devicesMap.values()) {
            devices.add(webViewDevice.toDevice());
        }
        return devices;
    }

    @Override
    public boolean hasNoDevices() {
        return devicesMap.isEmpty();
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

        device.on(WebViewDevice.CONFIG, new Listener() {
            @Override
            public void call(Object... args) {
                emit(CONFIG);
            }
        });
        devicesMap.put(device.getUuid(), device);
        emit(CONFIG);
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

        removeDevice(uuid);
    }

    private void removeDevice(String uuid) {
        WebViewDevice device = devicesMap.remove(uuid);
        if(device == null) {
            return;
        }
        device.stop();
        emit(CONFIG);
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
                emit(CONFIG);
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
                emit(CONFIG);
            }
        });
    }

    @Override
    public void removeAll() {
        for(String uuid : devicesMap.keySet()) {
            removeDevice(uuid);
        }
        devicesMap.clear();
    }



}
