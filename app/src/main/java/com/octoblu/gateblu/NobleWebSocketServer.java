package com.octoblu.gateblu;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by redaphid on 12/17/14.
 */
public class NobleWebSocketServer extends WebSocketServer {
    private final static String TAG = "gateblu:NobleWebSocketServer";
    private List<OnScanListener> onScanListeners = new ArrayList<>();
    private List<OnStopScanListener> onStopScanListeners = new ArrayList<>();
    private List<ConnectListener> connectListeners = new ArrayList<>();
    private List<DiscoverServicesListener> discoverServicesListeners = new ArrayList<>();
    private List<WebSocket> connections = new ArrayList<>();
    private List<DiscoverCharacteristicsListener> discoverCharacteristicsListeners = new ArrayList<>();

    public NobleWebSocketServer(InetSocketAddress address) {
        super(address);
        Log.d(TAG, "Instantiated Server");
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
        Log.d(TAG, "onMessage: " + message);
        try {
            routeMessage(connections.indexOf(conn), message);
        } catch (JSONException e) {
            Log.e(TAG, "something bad happened related to json parsing", e);
        }
    }

    private void routeMessage(int connIndex, String message) throws JSONException {
        JSONObject jsonObject;
        jsonObject = new JSONObject(message);
        String action = jsonObject.getString("action");

        switch(action){
            case "startScanning":
                startScanning(connIndex, jsonObject);
                break;
            case "stopScanning":
                stopScanning(connIndex);
                break;
            case "connect":
                connect(connIndex, jsonObject);
                break;
            case "discoverServices":
                discoverServices(connIndex, jsonObject);
                break;
            case "discoverCharacteristics":
                discoverCharacteristics(connIndex, jsonObject);
                break;
            default:
                Log.w(TAG, "I can't even '" + action + "'");
        }
    }



    private void connect(int connIndex, JSONObject jsonObject) throws JSONException {
        String peripheralUuid = jsonObject.getString("peripheralUuid");

        for(ConnectListener listener : connectListeners){
            listener.onConnect(connIndex, peripheralUuid);
        }

        sendConnectedToDevice(connIndex, peripheralUuid);
    }

    private void discoverServices(int connIndex, JSONObject jsonObject) throws JSONException {
        String peripheralUuid = jsonObject.getString("peripheralUuid");
        List<String> uuids = parseJSONStringArrayOfUuids(jsonObject, "uuids");

        for(DiscoverServicesListener listener : discoverServicesListeners){
            listener.onDiscoverServices(connIndex, peripheralUuid, uuids);
        }
    }

    private void discoverCharacteristics(int connIndex, JSONObject jsonObject) throws JSONException {
        String peripheralUuid = jsonObject.getString("peripheralUuid");
        String serviceUuid = jsonObject.getString("serviceUuid");
        List<String> characteristicUuids = parseJSONStringArrayOfUuids(jsonObject, "characteristicUuids");

        for(DiscoverCharacteristicsListener listener : discoverCharacteristicsListeners) {
            listener.onDiscoverCharacteristics(connIndex, peripheralUuid, serviceUuid, characteristicUuids);
        }
    }

    private void startScanning(int connIndex, JSONObject jsonObject) throws JSONException {
        List<String> uuids = parseJSONStringArrayOfUuids(jsonObject, "serviceUuids");

        for(OnScanListener scanListener : onScanListeners) {
            scanListener.onScanListener(uuids, connIndex);
        }
    }

    private void stopScanning(int connIndex) {
        for(OnStopScanListener stopScanListener : onStopScanListeners){
            stopScanListener.onStopScanListener(connIndex);
        }
    }

    private String derosenthal(String digits) {
        String uuid = digits.replaceAll(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5");
        return uuid;
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    public void setOnScanListener(OnScanListener onScanListener) {
        onScanListeners.add(onScanListener);
    }

    public void setOnStopScanListener(OnStopScanListener onStopScanListener) {
        onStopScanListeners.add(onStopScanListener);
    }

    private void sendConnectedToDevice(int connId, String peripheralUuid) throws JSONException {
        WebSocket conn = connections.get(connId);
        if(conn == null || !conn.isOpen()) {
            return;
        }

        JSONObject message = new JSONObject();
        message.put("type", "connect");
        message.put("peripheralUuid", peripheralUuid);
        conn.send(message.toString());
    }

    public void sendDiscoveredDevice(ScanResult result, int connId) {
        WebSocket conn = connections.get(connId);
        if(conn == null || !conn.isOpen()) {
            return;
        }
        JSONObject message = createDiscoverMessage(result, result.getRssi());
        Log.d(TAG, "sending message: " + message.toString());
        conn.send(message.toString());
    }

    public void sendDiscoveredServices(int connId, String peripheralUuid, List<String> discoveredUuids) {
        WebSocket conn = connections.get(connId);
        if(conn == null || !conn.isOpen()) {
            return;
        }

        try {
            JSONArray serviceUuids = new JSONArray();
            for(String uuid : discoveredUuids) {
                serviceUuids.put(uuid);
            }

            JSONObject message = new JSONObject();
            message.put("type", "servicesDiscover");
            message.put("peripheralUuid", peripheralUuid);
            message.put("serviceUuids", serviceUuids);
            Log.d(TAG, "sending this: " + message.toString());
            conn.send(message.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Something really terrible just happened. ", e);
        }
    }

    public void sendDiscoveredCharacteristics(int connId, String peripheralUuid, String serviceUuid, List<BluetoothGattCharacteristic> characteristics) {
        WebSocket conn = connections.get(connId);
        if(conn == null || !conn.isOpen()) {
            return;
        }

        try {
            JSONArray characteristicsJSON = new JSONArray();
            for(BluetoothGattCharacteristic characteristic : characteristics) {
                JSONArray properties = new JSONArray();
                for(BluetoothGattDescriptor descriptor : characteristic.getDescriptors()){
                    properties.put(descriptor.getValue());
                }

                JSONObject jsonCharacteristic = new JSONObject();
                jsonCharacteristic.put("uuid", characteristic.getUuid().toString());
                jsonCharacteristic.put("properties", properties);
                characteristicsJSON.put(jsonCharacteristic);
            }

            JSONObject message = new JSONObject();
            message.put("type", "characteristicsDiscover");
            message.put("peripheralUuid", peripheralUuid);
            message.put("serviceUuid", serviceUuid);
            message.put("characteristics", characteristicsJSON);

            Log.e(TAG, message.toString());
            conn.send(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createDiscoverMessage(ScanResult scanResult, int rssi) {
        try {
            BluetoothDevice device = scanResult.getDevice();
            ScanRecord scanRecord = scanResult.getScanRecord(); //.getServiceUuids()
            JSONArray serviceUuids = new JSONArray();

            for (ParcelUuid uuid : scanRecord.getServiceUuids()) {
                serviceUuids.put(uuid.getUuid().toString());
            }

            JSONObject advertisement = new JSONObject();
            advertisement.put("localName", device.getName());
            advertisement.put("txtPowerLevel", scanResult.getScanRecord().getTxPowerLevel());
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

    public void setConnectListener(ConnectListener connectListener) {
        this.connectListeners.add(connectListener);
    }

    public void setDiscoverServicesListener(DiscoverServicesListener listener) {
        this.discoverServicesListeners.add(listener);
    }

    public void setDiscoverCharacteristicsListener(DiscoverCharacteristicsListener listener) {
        this.discoverCharacteristicsListeners.add(listener);
    }

    private List<String> parseJSONStringArrayOfUuids(JSONObject jsonObject, String key) throws JSONException {
        JSONArray jsonArray = jsonObject.getJSONArray(key);
        List<String> stringList = new ArrayList<>(jsonArray.length());

        for(int i = 0; i < jsonArray.length(); i++){
            stringList.add(derosenthal(jsonArray.getString(i)));
        }

        return stringList;
    }




    public static abstract class OnScanListener{
        public abstract void onScanListener(List<String> uuids, int connId);
    }


    public static abstract class OnStopScanListener {
        public abstract void onStopScanListener(int connIndex);
    }

    public static abstract class ConnectListener {
        public abstract void onConnect(int connIndex, String deviceAddress);
    }

    public static abstract class DiscoverServicesListener {
        public abstract void onDiscoverServices(int connIndex, String deviceAddress, List<String> uuids);
    }

    public static abstract class DiscoverCharacteristicsListener {
        public abstract void onDiscoverCharacteristics(int connIndex, String peripheralUuid, String serviceUuid, List<String> characteristicUuids);

    }
}
