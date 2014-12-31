package com.octoblu.meshblu;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MeshbluService extends IntentService{
    public static final String TAG = "meshblu:MeshbluService";
    public static final String ACTION_SEND_DEVICES = "sendDevices";
    public static final String UUID = "uuid";
    public static final String TOKEN = "token";
    public static final String ACTION_READY = "ready";
    public static final String ACTION_SEND_DEVICE = "sendDevice";

    private LocalBroadcastManager localBroadcastManager;

    public MeshbluService() {
        super("MeshbluService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "onHandleIntent");

        String uuid = intent.getStringExtra(UUID);
        String token = intent.getStringExtra(TOKEN);

        final Meshblu meshblu;
        try {
            meshblu = new Meshblu(uuid, token);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to initialize meshblu", e);
            return;
        }

        meshblu.on(Meshblu.EVENT_READY, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Connected to Meshblu as: " + meshblu.getUuid() + ":" + meshblu.getToken());
                JSONObject authCredentials = (JSONObject) args[0];
                broadcastReady(meshblu, authCredentials);
                fetchGateblu(meshblu, authCredentials);
            }
        });

        meshblu.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    private void broadcastReady(Meshblu meshblu, JSONObject authCredentials) {
        String uuid;
        String token;
        try {
            uuid = authCredentials.getString(UUID);
            token = authCredentials.getString(TOKEN);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing authCredentials", e);
            return;
        }

        Intent intent = new Intent(ACTION_READY);
        intent.putExtra(UUID, uuid);
        intent.putExtra(TOKEN, token);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void fetchGateblu(final Meshblu meshblu, JSONObject gatebluJSON) {
        try {
            meshblu.whoami(gatebluJSON, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject gatebluJSON = (JSONObject) args[0];
                        JSONArray devicesJSON = gatebluJSON.getJSONArray("devices");
                        broadcastDevicesJSON(meshblu, devicesJSON);
                        fetchDevices(meshblu, devicesJSON);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing whoami response from Meshblu", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing whoami response from Meshblu", e);
        }
    }

    private void broadcastDeviceJSON(Meshblu meshblu, JSONObject deviceJSON) {
        Intent intent = new Intent(ACTION_SEND_DEVICE);
        intent.putExtra("device", deviceJSON.toString());
        localBroadcastManager.sendBroadcast(intent);
    }

    private void broadcastDevicesJSON(Meshblu meshblu, JSONArray devicesJSON) {
        Intent intent = new Intent(ACTION_SEND_DEVICES);
        intent.putExtra("devices", devicesJSON.toString());
        localBroadcastManager.sendBroadcast(intent);
    }

    private void fetchDevices(final Meshblu meshblu, JSONArray devicesJSON) throws JSONException {
        for(int i=0; i<devicesJSON.length(); i++){
            JSONObject deviceJSON = devicesJSON.getJSONObject(i);
            meshblu.devices(deviceJSON, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject responseJSON = (JSONObject) args[0];
                        JSONObject deviceJSON = responseJSON.getJSONArray("devices").getJSONObject(0);
                        broadcastDeviceJSON(meshblu, deviceJSON);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing devices response", e);
                    }
                }
            });
        }
    }
}
