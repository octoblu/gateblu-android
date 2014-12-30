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

    private LocalBroadcastManager localBroadcastManager;
    private Meshblu meshblu;

    public MeshbluService() {
        super("MeshbluService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        String uuid = intent.getStringExtra(UUID);
        String token = intent.getStringExtra(TOKEN);

        try {
//            meshblu = new Meshblu("834d3711-8aef-11e4-b94a-b19d17114b8a", "0ab9dwv0vgkdwjyvinx8c59a5h8mpldi");
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
                broadcastReady(authCredentials);
                fetchGateblu(authCredentials);
            }
        });

        meshblu.connect();
    }

    private void broadcastReady(JSONObject authCredentials) {
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

    private void fetchGateblu(JSONObject gatebluJSON) {
        meshblu.whoami(gatebluJSON, new Emitter.Listener(){
            @Override
            public void call(Object... args) {
                try {
                    JSONObject gatebluJSON = (JSONObject) args[0];
                    JSONArray devicesJSON = gatebluJSON.getJSONArray("devices");
                    broadcastDevicesJSON(devicesJSON);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing whoami response from Meshblu", e);
                }
            }
        });
    }

    private void broadcastDevicesJSON(JSONArray devicesJSON) {
        Intent intent = new Intent(ACTION_SEND_DEVICES);
        intent.putExtra("devices", devicesJSON.toString());
        localBroadcastManager.sendBroadcast(intent);
    }
}
