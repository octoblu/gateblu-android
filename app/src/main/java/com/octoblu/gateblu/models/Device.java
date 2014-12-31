package com.octoblu.gateblu.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Device {
    private static final String TAG = "Gateblu:Device";
    public static final String UUID = "uuid";
    public static final String TOKEN = "token";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String CONNECTOR = "connector";
    private final ArrayList<OnlineChangedListener> onOnlineChangedListeners;
    private boolean online;
    private String uuid, token, connector, name, type;

    public Device(String uuid, String token, String connector, String name, String type) {
        this.uuid = uuid;
        this.token = token;
        this.connector = connector;
        this.name = name;
        this.type = type;
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

    public void update(JSONObject deviceJSON) throws JSONException {
        this.name  = deviceJSON.getString(NAME);
        this.type  = deviceJSON.getString(TYPE);
    }

    public String getUuid() {
        return uuid;
    }

    public String getToken() {
        return token;
    }

    public String getConnector() {
        return connector;
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
        String connector = deviceJSON.getString(CONNECTOR);
        return new Device(uuid, token, connector, name, type);
    }

    public static abstract class OnlineChangedListener{
        public abstract void onOnlineChanged();
    }
}
