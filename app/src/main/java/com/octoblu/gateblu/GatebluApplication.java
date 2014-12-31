package com.octoblu.gateblu;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.json.JSONObject;

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
    public static final String RUN_CONNECTORS = "runConnectors";

    private boolean meshbluHasConnected = false;
    private boolean fetchedDevices      = false;
    private boolean connectorsAreRunning = false;

    private final List<WebView> webviews = new ArrayList<>();
    private final List<Device> devices = new ArrayList<>();

    private Emitter emitter = new Emitter();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Gateblu:GatebluApplication", "onCreate");

        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        Intent nobleServiceIntent = new Intent(this, NobleService.class);
        startService(nobleServiceIntent);

        Intent notificationDismissalService = new Intent(this, NotificationDismissalService.class);
        startService(notificationDismissalService);

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
                onReceiveDeviceJSON(intent);
            }
        }, new IntentFilter(MeshbluService.ACTION_SEND_DEVICE));

        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onMeshbluReady(intent);
            }
        }, new IntentFilter(MeshbluService.ACTION_READY));

        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showNoBluetoothAdapterFoundNotification();
            }
        }, new IntentFilter(NobleService.ACTION_NO_BLUETOOTH_ADAPTER_FOUND));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: " + ACTION_STOP_CONNECTORS);
                turnConnectorsOff();
            }
        }, new IntentFilter(ACTION_STOP_CONNECTORS));

        restartMeshbluService();
    }

    public List<Device> getDevices() {
        return devices;
    }


    private Device getDevice(String uuid) {
        for(Device device : devices) {
            if(device.getUuid().equals(uuid)){
                return device;
            }
        }
        return null;
    }

    // region State Indicators
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

    public boolean shouldRunConnectors() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        return preferences.getBoolean(RUN_CONNECTORS, true);
    }
    // endregion

    // region Event Listeners
    public void on(String event, Emitter.Listener fn) {
        emitter.on(event, fn);
    }

    public void off() {
        emitter.off();
    }

    private void onMeshbluReady(Intent intent) {
        meshbluHasConnected = true;
        SharedPreferences.Editor preferences = getPreferencesEditor();
        preferences.putString(UUID, intent.getStringExtra(MeshbluService.UUID));
        preferences.putString(TOKEN, intent.getStringExtra(MeshbluService.TOKEN));
        preferences.commit();
    }

    private SharedPreferences.Editor getPreferencesEditor() {
        return getSharedPreferences(PREFERENCES_FILE_NAME, 0).edit();
    }

    private void onReceiveDeviceJSON(Intent intent) {
        try {
            JSONObject deviceJSON = new JSONObject(intent.getStringExtra("device"));
            Log.i(TAG, "onReceiveDeviceJSON: " + deviceJSON.toString());

            String uuid = deviceJSON.getString(UUID);
            Device device = getDevice(uuid);
            if(device == null){
                return;
            }

            device.update(deviceJSON);
            emitDevicesUpdated();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON from intent", e);
            return;
        }
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

        emitDevicesUpdated();

        restartAllConnectors();
    }

    private void emitDevicesUpdated() {
        emitter.emit(EVENT_DEVICES_UPDATED);
    }
    // endregion

    // region Actions
    public void resetGateblu() {
        stopAllConnectors();
        devices.clear();

        SharedPreferences.Editor preferences = getPreferencesEditor();
        preferences.clear();
        preferences.commit();

        restartMeshbluService();
        emitDevicesUpdated();
    }

    public void restartAllConnectors() {
        stopAllConnectors(); // For safety

        if(!shouldRunConnectors()){
            return;
        }

        for (Device device : devices) {
            Log.d(TAG, "Starting up a: " + device.getConnector());
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
        emitDevicesUpdated();
    }

    private void restartMeshbluService() {
        meshbluHasConnected = false;
        connectorsAreRunning = false;
        fetchedDevices = false;

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        String uuid = preferences.getString(UUID, null);
        String token = preferences.getString(TOKEN, null);
//        uuid = "834d3711-8aef-11e4-b94a-b19d17114b8a";
//        token = "0ab9dwv0vgkdwjyvinx8c59a5h8mpldi";

        Intent meshbluServiceIntent = new Intent(this, MeshbluService.class);
        meshbluServiceIntent.putExtra(MeshbluService.UUID, uuid);
        meshbluServiceIntent.putExtra(MeshbluService.TOKEN, token);
        startService(meshbluServiceIntent);
    }

    public void turnConnectorsOff() {
        SharedPreferences.Editor editor = getPreferencesEditor();
        editor.putBoolean(RUN_CONNECTORS, false);
        editor.apply();
        stopAllConnectors();
    }

    public void turnConnectorsOn() {
        SharedPreferences.Editor editor = getPreferencesEditor();
        editor.putBoolean(RUN_CONNECTORS, true);
        editor.apply();
        restartAllConnectors();
    }

    public void stopAllConnectors() {
        for(WebView webView : webviews) {
            webView.destroy();
        }
        webviews.clear();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        connectorsAreRunning = false;
        emitDevicesUpdated();
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
    // endregion
}
