package com.octoblu.gateblu;

import org.json.JSONObject;

public interface DeviceManager {
    String CONFIG = "config";

    boolean isReady();
    void setReady(boolean b);

    void addDevice(JSONObject data);
    void removeDevice(JSONObject data);
    void startDevice(JSONObject data);

    void stopDevice(JSONObject data);

    void removeAll();

    boolean hasNoDevices();
}
