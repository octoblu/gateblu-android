package com.octoblu.gateblu;

import android.util.Log;

public class Device {
    private static final String TAG = "Gateblu:Device";
    private String name;
    private String type;

    public Device(String name, String type) {
        this.name = name;
        this.type = type;
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

    public void toggle() {
        Log.d(TAG, "I be toggling");
    }
}
