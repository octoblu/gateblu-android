package com.octoblu.gateblu;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;
import com.octoblu.sanejsonobject.SaneJSONObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GatebluApplication extends Application {
    public static final String TAG = "GatebluApplication";
    public static final String PREFERENCES_FILE_NAME = "meshblu_preferences";
    public static final String UUID = "uuid";
    public static final String TOKEN = "token";
    public static final String CONFIG = "config";
    public static final String CLAIM_GATEBLU = "claim_gateblu";
    public static final String ACTION_STOP_CONNECTORS = "stopConnectors";
    public static final String RESUME = "resume";
    public static final int PERSISTENT_NOTIFICATION_ID = 1;
    private static final String STOPPED = "stopped";

    public final class STATES {
        public static final String OFF = "off";
        public static final String NO_DEVICES = "no-devices";
        public static final String CLAIM_GATEBLU = "claim-gateblu";
        public static final String LOADING = "loading";
        public static final String READY = "ready";
    }

    private Emitter emitter = new Emitter();
    private Handler uiThreadHandler;
    private Gateblu gateblu;
    private Boolean claimingGateblu = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        WebView.setWebContentsDebuggingEnabled(true);
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        uiThreadHandler = new Handler();

        Intent nobleServiceIntent = new Intent(this, NobleService.class);
        startService(nobleServiceIntent);

        Intent notificationDismissalService = new Intent(this, NotificationDismissalService.class);
        startService(notificationDismissalService);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showNoBluetoothAdapterFoundNotification();
            }
        }, new IntentFilter(NobleService.ACTION_NO_BLUETOOTH_ADAPTER_FOUND));

        restartGateblu();
    }

    public List<Device> getDevices() {
        return gateblu.getDevices();
    }

    // region State Indicators
    public String getState(){
        if(gateblu == null){
            return STATES.OFF;
        }
        if(!gateblu.isReady()) {
            return STATES.LOADING;
        }
        if(!gateblu.hasOwner()) {
            return STATES.CLAIM_GATEBLU;
        }
        if(gateblu.hasNoDevices()) {
            return STATES.NO_DEVICES;
        }
        return STATES.READY;
    }

    public boolean hasNoDevices() {
        return gateblu.hasNoDevices();
    }


    public boolean isLoading() {
        return !gateblu.isReady();
    }
    // endregion

    // region Event Listeners
    public void on(String event, Emitter.Listener fn) {
        emitter.on(event, fn);
    }

    public void off() {
        emitter.off();
    }

    private SharedPreferences.Editor getPreferencesEditor() {
        return getSharedPreferences(PREFERENCES_FILE_NAME, 0).edit();
    }
    // endregion

    // region Actions
    public void resetGateblu() {
        SharedPreferences.Editor preferences = getPreferencesEditor();
        preferences.clear();
        preferences.commit();

        restartGateblu();
    }

    public void restartGateblu() {
        if(gateblu != null) {
            gateblu.stop();
            gateblu.off();
            gateblu = null;
        }

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        String uuid = preferences.getString(UUID, null);
        String token = preferences.getString(TOKEN, null);
        boolean stopped = preferences.getBoolean(STOPPED, false);
        if(stopped){
            return;
        }

        gateblu = new Gateblu(uuid, token, this, uiThreadHandler);
        gateblu.on(Gateblu.CONFIG, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                emitter.emit(CONFIG, args);
            }
        });
        gateblu.on(Gateblu.REGISTER, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject gatebluJSON = (JSONObject) args[0];
                saveCredentials(SaneJSONObject.fromJSONObject(gatebluJSON));
            }
        });
        gateblu.on(Gateblu.WHOAMI, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject gatebluJSON = (JSONObject) args[0];
                saveGatebluDevice(SaneJSONObject.fromJSONObject(gatebluJSON));
                emitter.emit(CONFIG);
                if(claimingGateblu){
                    new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            Log.i(TAG, "Check whoami again...");
                            gateblu.whoami();
                        }
                    }, 10000);
                }
            }
        });
        gateblu.on(Gateblu.GENERATED_TOKEN, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject gatebluJSONObject = (JSONObject) args[0];
                SaneJSONObject gatebluJSON = SaneJSONObject.fromJSONObject(gatebluJSONObject);
                if(claimingGateblu){
                    String uuid = gatebluJSON.getStringOrNull("uuid");
                    String token = gatebluJSON.getStringOrNull("token");
                    emitter.emit(CLAIM_GATEBLU, uuid, token);
                }

            }
        });
        gateblu.on(Gateblu.READY, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                gateblu.whoami();
            }
        });
        gateblu.restart();
        emitter.emit(CONFIG);
    }

    public void claimGateblu(){
        claimingGateblu = true;
        gateblu.generateToken(getUuid());
    }

    public void whoami(){
        gateblu.whoami();
    }

    public void start(){
        SharedPreferences.Editor preferences = getPreferencesEditor();
        preferences.putBoolean(STOPPED, false);
        preferences.commit();
        restartGateblu();
        emitter.emit(CONFIG);
    }

    public void stop(){
        SharedPreferences.Editor preferences = getPreferencesEditor();
        preferences.putBoolean(STOPPED, true);
        preferences.commit();
        restartGateblu();
        emitter.emit(CONFIG);
    }

    private void saveCredentials(SaneJSONObject gatebluJSON) {
        String uuid = gatebluJSON.getStringOrNull("uuid");
        String token = gatebluJSON.getStringOrNull("token");

        SharedPreferences.Editor preferences = getPreferencesEditor();
        preferences.clear();
        preferences.putString(UUID, uuid);
        preferences.putString(TOKEN, token);
        preferences.commit();
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                restartGateblu();
                emitter.emit(CONFIG);
            }
        });
    }

    private void saveGatebluDevice(SaneJSONObject gatebluJSON) {
        gateblu.setOwner(gatebluJSON.getStringOrNull("owner"));
        if(gateblu.hasOwner()){
            claimingGateblu = false;
        }
        gateblu.setName(gatebluJSON.getStringOrNull("name"));
    }

    // endregion

    // region View Helpers
    private void showNoBluetoothAdapterFoundNotification() {
        Intent resumeIntent = new Intent(this, GatebluActivity.class);
        resumeIntent.setAction(RESUME);
        PendingIntent resumePendingIntent = PendingIntent.getActivity(this, 0, resumeIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_error_white_24dp);
        builder.setColor(getResources().getColor(R.color.primary));
        builder.setContentTitle("Gateblu Error");
        builder.setContentText("No bluetooth adapter found, or bluetooth adapter is not enabled");
        builder.setContentIntent(resumePendingIntent);
        builder.setTicker("Gateblu Error");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, builder.build());
    }

    private void showPersistentNotification() {
        Intent resumeIntent = new Intent(this, GatebluActivity.class);
        resumeIntent.setAction(RESUME);
        PendingIntent resumePendingIntent = PendingIntent.getActivity(this, 0, resumeIntent, 0);

        Intent stopIntent = new Intent(ACTION_STOP_CONNECTORS);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_spiral);
        builder.setColor(getResources().getColor(R.color.primary));
        builder.setContentTitle("Gateblu is running");
        builder.setContentText("running, running, running");
        builder.setContentIntent(resumePendingIntent);
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);
        builder.setOngoing(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, builder.build());
    }

    public String getToken() {
        SharedPreferences preferences = getSharedPreferences(GatebluApplication.PREFERENCES_FILE_NAME, 0);
        return preferences.getString(GatebluApplication.TOKEN, null);
    }

    public String getUuid() {
        SharedPreferences preferences = getSharedPreferences(GatebluApplication.PREFERENCES_FILE_NAME, 0);
        return preferences.getString(GatebluApplication.UUID, null);
    }

    public String getName() {
        String gatebluName = gateblu.getName();
        if(gatebluName == null || gatebluName.isEmpty()){
            return "Gateblu";
        }
        return gatebluName;
    }

    // endregion
}
