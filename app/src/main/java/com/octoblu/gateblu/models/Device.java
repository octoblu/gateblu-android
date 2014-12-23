package com.octoblu.gateblu.models;

import android.util.Log;

import java.util.ArrayList;
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

    public Map<String,String> toMap() {
        Map<String, String> deviceMap = new HashMap<>();
        deviceMap.put(UUID, uuid);
        deviceMap.put(TOKEN, token);
        deviceMap.put(NAME, name);
        deviceMap.put(TYPE, type);
        return deviceMap;
    }

    public static Device fromMap(Map<String,String> deviceMap){
        return new Device(deviceMap.get(NAME), deviceMap.get(TYPE), deviceMap.get(UUID), deviceMap.get(TOKEN));
    }

    public static List<Device> fromMapList(List<HashMap<String, String>> devicesMapList) {
        List<Device> devices = new ArrayList<>(devicesMapList.size());

        for(Map<String,String> deviceMap : devicesMapList){
            devices.add(fromMap(deviceMap));
        }

        return devices;
    }


    public static abstract class OnlineChangedListener{
        public abstract void onOnlineChanged();
    }
}
