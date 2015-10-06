package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

import com.octoblu.gateblu.models.Device;
import com.octoblu.sanejsonobject.SaneJSONObject;

public class WebViewDeviceManager extends Emitter implements DeviceManager {

    public static final String TAG = "WebViewDeviceManager";
    public static final String SEND_LOG = "send_log";
    private final Context context;
    private final ConcurrentHashMap<String, WebViewDevice> devicesMap;
    private final Handler uiThreadHandler;
    private boolean ready = false;

    public WebViewDeviceManager(Context context, Handler uiThreadHandler) {
        this.context = context;
        this.uiThreadHandler = uiThreadHandler;
        this.devicesMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setReady(boolean ready) {
        this.ready = ready;
        emit(CONFIG);
        emit(READY);
    }

    @Override
    public List<Device> getDevices() {
        List<Device> devices = new ArrayList<>(devicesMap.size());

        Collection<WebViewDevice> webViewDevices = devicesMap.values();
        for(WebViewDevice webViewDevice : webViewDevices) {
            devices.add(webViewDevice.toDevice());
        }
        return devices;
    }

    @Override
    public Device getDevice(String uuid) {
        if(uuid == null){
            return null;
        }

        List<Device> devices = new ArrayList<>(devicesMap.size());

        Collection<WebViewDevice> webViewDevices = devicesMap.values();
        for(WebViewDevice webViewDevice : webViewDevices) {
            if(uuid == null){
                continue;
            }
            if(uuid.equals(webViewDevice.getUuid())){
                return webViewDevice.toDevice();
            }
        }
        return null;
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
        device.on(WebViewDevice.SEND_LOG, new Listener() {
            @Override
            public void call(Object... args) {
                SaneJSONObject log = (SaneJSONObject) args[0];
                emit(SEND_LOG, log);
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
        final WebViewDevice device = devicesMap.remove(uuid);
        if(device == null) {
            return;
        }

        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                device.stop();
                emit(CONFIG);
            }
        });
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
                SaneJSONObject log = new SaneJSONObject();
                log.putOrIgnore("workflow", "start-device");
                log.putOrIgnore("state", "begin");
                log.putOrIgnore("uuid", uuid);
                log.putOrIgnore("message", "");
                emit(SEND_LOG, log);
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
