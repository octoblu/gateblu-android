package com.octoblu.gateblu;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class DeviceManagerServer extends WebSocketServer {

    private static final String TAG = "DeviceManagerServer";
    private final DeviceManager deviceManager;

    public DeviceManagerServer(DeviceManager deviceManager) throws UnknownHostException {
        super( new InetSocketAddress( 0xD00D ) );
        this.deviceManager = deviceManager;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i(TAG, "onOpen");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.i(TAG, "onClose");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.i(TAG, "onMessage:" + message);
        String action, messageId;
        try {
            JSONObject jsonObject = new JSONObject(message);
            action = jsonObject.getString("action");
            messageId = jsonObject.getString("id");
        } catch (JSONException e) {
            Log.e(TAG, "onMessage JSONException", e);
            return;
        }

        switch (action) {
            case "addDevice":
                deviceManager.addDevice();
                break;
            case "removeDevice":
                deviceManager.removeDevice();
                break;
            case "startDevice":
                deviceManager.startDevice();
                break;
            case "stopDevice":
                deviceManager.stopDevice();
                break;
        }

        JSONObject responseJSON = new JSONObject();
        putInJSONObject(responseJSON, "id", messageId);
        conn.send(responseJSON.toString());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "onError", ex);
    }

    private void putInJSONObject(JSONObject responseJSON, String key, String value) {
        try {
            responseJSON.put(key, value);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException", e);
        }
    }
}
