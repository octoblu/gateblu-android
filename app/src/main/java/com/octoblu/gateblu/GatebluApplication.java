package com.octoblu.gateblu;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;
import com.octoblu.meshblu.MeshbluService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class GatebluApplication extends Application {
    public static final String TAG = "Gateblu:GatebluApplication";
    public static final String PREFERENCES_FILE_NAME = "meshblu_preferences";
    public static final String UUID = "uuid";
    public static final String TOKEN = "token";
    public static final String EVENT_DEVICES_UPDATED = "devicesUpdated";
    public static final String ACTION_STOP_CONNECTORS = "stopConnectors";
    public static final String RESUME = "resume";
    public static final int PERSISTENT_NOTIFICATION_ID = 1;

    private boolean meshbluHasConnected = false;
    private boolean fetchedDevices      = false;
    private boolean connectorsAreRunning = false;

    private final List<WebView> webviews = new ArrayList<>();
    private final List<Device> devices = new ArrayList<>();
    private Intent meshbluServiceIntent;

    private Emitter emitter = new Emitter();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Gateblu:GatebluApplication", "onCreate");

        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        Intent nobleServiceIntent = new Intent(this, NobleService.class);
        startService(nobleServiceIntent);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onReceiveDevicesJSON(intent);
            }
        }, new IntentFilter(MeshbluService.ACTION_SEND_DEVICES));

        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onMeshbluReady(intent);
            }
        }, new IntentFilter(MeshbluService.ACTION_READY));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive: " + ACTION_STOP_CONNECTORS);
                stopAllConnectors();
            }
        }, new IntentFilter(ACTION_STOP_CONNECTORS));

        restartMeshbluService();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.w(TAG, "onTerminate");
    }

    public List<Device> getDevices() {
        return devices;
    }

    public boolean areConnectorsRunning(){
        return connectorsAreRunning;
    }

    public boolean hasFetchedDevices() {
        return fetchedDevices;
    }

    public boolean hasMeshbluConnected() {
        return meshbluHasConnected;
    }

    public boolean hasNoDevices() {
        return devices.size() == 0;
    }

    // region Event Listeners
    public void on(String event, Emitter.Listener fn) {
        emitter.on(event, fn);
    }

    public void off() {
        emitter.off();
    }

    private void onMeshbluReady(Intent intent) {
        meshbluHasConnected = true;
        SharedPreferences.Editor preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0).edit();
        preferences.putString(UUID, intent.getStringExtra(MeshbluService.UUID));
        preferences.putString(TOKEN, intent.getStringExtra(MeshbluService.TOKEN));
        preferences.commit();
    }

    public void onReceiveDevicesJSON(Intent intent) {
        List<Device> devices;

        try {
            JSONArray devicesJSON = new JSONArray(intent.getStringExtra("devices"));
            devices = Device.fromJSONArray(devicesJSON);
            Log.d(TAG, "onReceiveDevicesJSON: " + devicesJSON);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON from intent", e);
            return;
        }

        this.devices.clear();
        this.devices.addAll(devices);
        fetchedDevices = true;

        emitter.emit(EVENT_DEVICES_UPDATED);

        startAllConnectors();
    }
    // endregion

    // region Actions
    public void resetGateblu() {
        stopAllConnectors();
        devices.clear();

        SharedPreferences.Editor preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0).edit();
        preferences.clear();
        preferences.commit();

        restartMeshbluService();

    }

    private void restartMeshbluService() {
        if(meshbluServiceIntent != null){
            stopService(meshbluServiceIntent);
        }
        meshbluHasConnected = false;
        connectorsAreRunning = false;

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        String uuid = preferences.getString(UUID, null);
        String token = preferences.getString(TOKEN, null);

        meshbluServiceIntent = new Intent(this, MeshbluService.class);
        meshbluServiceIntent.putExtra(MeshbluService.UUID, uuid);
        meshbluServiceIntent.putExtra(MeshbluService.TOKEN, token);
        startService(meshbluServiceIntent);
    }

    public void startAllConnectors() {
        stopAllConnectors(); // For safety

        for (Device device : devices) {
            Log.i(TAG, "Starting up a: " + device.getConnector());
            WebView webView = new WebView(this);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            webView.loadUrl("file:///android_asset/www/gateblu.html");
            webView.evaluateJavascript("window.meshbluDevice = {uuid: \"" + device.getUuid() + "\", token: \"" + device.getToken() + "\", connector: \"" + device.getConnector() + "\"};", new Util.IgnoreReturnValue());
            webviews.add(webView);
        }

        showPersistentNotification();
        connectorsAreRunning = true;
        emitter.emit(EVENT_DEVICES_UPDATED);
    }

    public void stopAllConnectors() {
        for(WebView webView : webviews) {
            webView.destroy();
        }
        webviews.clear();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        connectorsAreRunning = false;
        emitter.emit(EVENT_DEVICES_UPDATED);
    }
    // endregion

    // region View Helpers
    private void showPersistentNotification() {
        Intent resumeIntent = new Intent(this, GatebluActivity.class);
        resumeIntent.setAction(RESUME);
        PendingIntent resumePendingIntent = PendingIntent.getActivity(this, 0, resumeIntent, 0);

        Intent stopIntent = new Intent(ACTION_STOP_CONNECTORS);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_spiral);
        builder.setContentTitle("Gateblu is running");
        builder.setContentText("running, running, running");
        builder.setContentIntent(resumePendingIntent);
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);
        builder.setOngoing(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, builder.build());
    }
    // endregion
}
