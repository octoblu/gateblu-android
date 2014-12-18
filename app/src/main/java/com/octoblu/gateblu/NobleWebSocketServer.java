package com.octoblu.gateblu;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.util.JsonReader;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by redaphid on 12/17/14.
 */
public class NobleWebSocketServer extends WebSocketServer {
    private final static String TAG = "gateblu:NobleWebSocketServer";
    private List<OnScanListener> onScanListeners = null;
    private List<WebSocket> connections = new ArrayList<>();

    public NobleWebSocketServer(InetSocketAddress address) {
        super(address);
        Log.d(TAG, "Instantiated Server");
        onScanListeners = new ArrayList<OnScanListener>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, "something connected!");
        connections.add(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, message);
        try {
            processMessage(message, connections.indexOf(conn));
        } catch (JSONException e) {
            Log.d(TAG, "something bad happened related to json parsing");
            e.printStackTrace();
            conn.send("bad json!");
        }
    }

    private void processMessage(String message, int connIndex) throws JSONException {
        JSONObject jsonObject;
        jsonObject = new JSONObject(message);
        String action = jsonObject.getString("action");

        if("startScanning".equals(action)) {
            startScanning(jsonObject, connIndex);
        }
    }

    private void startScanning(JSONObject jsonObject, int connIndex) throws JSONException {
        JSONArray jsonUUIDs = jsonObject.getJSONArray("serviceUuids");
        UUID[] uuids = new UUID[jsonUUIDs.length()];

        for(int i = 0; i < jsonUUIDs.length(); i++){
            uuids[i] = toUUID(jsonUUIDs.getString(i));

        }

        for(OnScanListener scanListener : onScanListeners) {
            scanListener.onScanListener(uuids, connIndex);
        }
    }

    private UUID toUUID(String digits) {
        String uuid = digits.replaceAll(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5");
        return UUID.fromString(uuid);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    public void setOnScanListener(OnScanListener onScanListener) {
        onScanListeners.add(onScanListener);
    }

    public void sendDiscoveredDevice(BluetoothDevice device, int rssi, int connId) {
        WebSocket conn = connections.get(connId);
        if(conn == null) {
            return;
        }
        JSONObject message = createDiscoverMessage(device, rssi);
        Log.d(TAG, "sending message: " + message.toString());
        conn.send(message.toString());
    }

    private JSONObject createDiscoverMessage(BluetoothDevice device, int rssi) {
        try {
            JSONArray serviceUuids = new JSONArray();
            if(device.getUuids() != null) {
                for (ParcelUuid uuid : device.getUuids()) {
                    serviceUuids.put(uuid.getUuid().toString());
                }
            }

            JSONObject advertisement = new JSONObject();
            advertisement.put("localName", device.getName());
            advertisement.put("txtPowerLevel", 9001);
            advertisement.put("serviceUuids", serviceUuids);

            JSONObject message = new JSONObject();
            message.put("type", "discover");
            message.put("peripheralUuid", device.getAddress());
            message.put("rssi", rssi);
            message.put("advertisement", advertisement);
            return message;
        } catch (JSONException e) {
            return null;
        }
    }

    public static abstract class OnScanListener{
        public abstract void onScanListener(UUID[] uuids, int connId);
    }


}
