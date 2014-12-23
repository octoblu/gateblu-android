package com.octoblu.meshblu;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import java.net.URISyntaxException;

public class MeshbluService extends IntentService {

    public static final String TAG = "Meshblu:MeshbluService";

    public MeshbluService() {
        super("MeshbluService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");

        final Meshblu meshblu;

        try {
            meshblu = new Meshblu();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to initialize meshblu", e);
            stopSelf();
            return;
        }

        meshblu.on(Meshblu.EVENT_READY, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "Connected to Meshblu as: " + meshblu.getUuid() + ":" + meshblu.getToken());
            }
        });

        meshblu.connect();
    }
}
