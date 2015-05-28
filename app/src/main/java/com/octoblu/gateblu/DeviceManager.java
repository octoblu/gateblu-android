package com.octoblu.gateblu;

import org.json.JSONObject;

public interface DeviceManager {
    void addDevice(JSONObject data);
    void removeDevice(JSONObject data);
    void startDevice(JSONObject data);
    void stopDevice(JSONObject data);
}
