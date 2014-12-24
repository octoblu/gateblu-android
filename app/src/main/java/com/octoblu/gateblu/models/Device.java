package com.octoblu.gateblu.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Device {
    private static final String TAG = "Gateblu:Device";
    public static final String UUID = "uuid";
    public static final String TOKEN = "token";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    private final ArrayList<OnlineChangedListener> onOnlineChangedListeners;
    private boolean online;
    private String name;
    private String type;
    private String uuid;
    private String token;

    public Device(String name, String type, String uuid, String token) {
        this.name = name;
        this.type = type;
        this.uuid = uuid;
        this.token = token;
        this.online = true;
        this.onOnlineChangedListeners = new ArrayList<OnlineChangedListener>();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getImageName() {
        String[] parts = getType().split(":");
        return parts[parts.length - 1];
    }

    public boolean isOnline() {
        return this.online;
    }

    public void setOnline(boolean online){
        boolean changed = (this.online != online);
        this.online = online;

        if(changed) {
            for(OnlineChangedListener listener : this.onOnlineChangedListeners){
                listener.onOnlineChanged();
            }
        }
    }

    public void setOnOnlineChangedListener(OnlineChangedListener onlineChangedListener){
        this.onOnlineChangedListeners.add(onlineChangedListener);
    }

    public void toggle() {
        this.setOnline(!this.online);
        Log.d(TAG, "I be toggling, am now: " + this.online);
    }

    public String getUuid() {
        return uuid;
    }

    public String getToken() {
        return token;
    }

    public static List<Device> fromJSONArray(JSONArray devicesJSON) throws JSONException {
        List<Device> devices = new ArrayList<>();

        for(int i=0; i<devicesJSON.length(); i++){
            JSONObject deviceJSON = devicesJSON.getJSONObject(i);
            devices.add(Device.fromJSONObject(deviceJSON));

        }

        return devices;
    }

    private static Device fromJSONObject(JSONObject deviceJSON) throws JSONException {
        String name = deviceJSON.has(NAME) ? deviceJSON.getString(NAME) : "Unknown";
        String type = deviceJSON.has(TYPE) ? deviceJSON.getString(TYPE) : "device:generic";
        String uuid = deviceJSON.getString(UUID);
        String token = deviceJSON.getString(TOKEN);
        return new Device(name, type, uuid, token);
    }

    public static abstract class OnlineChangedListener{
        public abstract void onOnlineChanged();
    }
}
