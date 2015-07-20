package com.octoblu.gateblu;

import com.octoblu.gateblu.models.Device;

import org.json.JSONObject;

import java.util.List;

public interface DeviceManager {
    String CONFIG = "config";
    String READY = "ready";

    void setReady(boolean b);
    boolean isReady();
    boolean hasNoDevices();
    void removeAll();

    List<Device> getDevices();

    void addDevice(JSONObject data);
    void removeDevice(JSONObject data);
    void startDevice(JSONObject data);
    void stopDevice(JSONObject data);
}
