package com.octoblu.gateblu;

import android.bluetooth.BluetoothDevice;
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

    private List<OnScanListener> onScanListeners = null;
    private List<WebSocket> connections = new ArrayList<>();

    public NobleWebSocketServer(InetSocketAddress address) {
        super(address);
        Log.d("GatebluWebsocket", "Instantiated Server");
        onScanListeners = new ArrayList<OnScanListener>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d("GatebluWebsocket", "something connected!");
        connections.add(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d("GatebluWebsocket", message);
        try {
            processMessage(message, connections.indexOf(conn));
        } catch (JSONException e) {
            Log.d("GatebluWebsocket", "something bad happened related to json parsing");
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

    public void sendDiscoveredDevice(BluetoothDevice device, int connId) {
        WebSocket conn = connections.get(connId);
        if(conn == null) {
            return;
        }
        JSONObject message = createDiscoverMessage(device);
        Log.d("GatebluWebsocket", "sending message: " + message.toString());
        conn.send(message.toString());
    }

    private JSONObject createDiscoverMessage(BluetoothDevice device) {
        JSONObject message;

        try {
            message = new JSONObject();
            message.put("action", "discover");
        } catch (JSONException e) {
            return null;
        }

        return message;
    }

    public static abstract class OnScanListener{
        public abstract void onScanListener(UUID[] uuids, int connId);
    }


}
