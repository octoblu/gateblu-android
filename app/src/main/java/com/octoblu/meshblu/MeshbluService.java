package com.octoblu.meshblu;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.List;

public class MeshbluService extends IntentService{
    public final static String TAG = "meshblu:MeshbluService";

    private LocalBroadcastManager localBroadcastManager;
    private Meshblu meshblu;
    private JSONObject gatebluJSON;

    public MeshbluService() {
        super("MeshbluService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        try {
            meshblu = new Meshblu("834d3711-8aef-11e4-b94a-b19d17114b8a", "0ab9dwv0vgkdwjyvinx8c59a5h8mpldi");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to initialize meshblu", e);
            return;
        }

        meshblu.on(Meshblu.EVENT_READY, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Connected to Meshblu as: " + meshblu.getUuid() + ":" + meshblu.getToken());
                fetchGateblu((JSONObject) args[0]);
            }
        });

        meshblu.connect();
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
        Intent intent = new Intent("sendDevices");
        intent.putExtra("devices", devicesJSON.toString());
        localBroadcastManager.sendBroadcast(intent);
    }
}
