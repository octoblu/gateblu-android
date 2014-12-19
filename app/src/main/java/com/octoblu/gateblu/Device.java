package com.octoblu.gateblu;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Device {
    private static final String TAG = "Gateblu:Device";
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

    public static abstract class OnlineChangedListener{
        public abstract void onOnlineChanged();
    }
}
