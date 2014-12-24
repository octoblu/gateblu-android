package com.octoblu.gateblu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.GridView;

import com.octoblu.gateblu.models.Device;
import com.octoblu.meshblu.MeshbluService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class GatebluActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    public static final String STOP = "stop";
    public static final String RESUME = "resume";
    public static final int PERSISTENT_NOTIFICATION_ID = 1;
    private static final String TAG = "Gateblu:GatebluActivity";
    private static final String PREFERENCES_FILE_NAME = "meshblu_preferences";
    public static final String UUID = "uuid";
    public static final String TOKEN = "token";
    private final List<Device> devices = new ArrayList<>();
    private final List<WebView> webviews = new ArrayList<>();
    private DeviceGridAdapter deviceGridAdapter;
    private boolean connectorsAreRunning = false;
    private Intent meshbluServiceIntent;
    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        setContentView(R.layout.activity_gateblu);

        gridView = (GridView)findViewById(R.id.devices_grid);
        gridView.setOnItemClickListener(this);
        refreshDeviceGrid();

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
                onReceiveAuthCredentials(intent);
            }
        }, new IntentFilter(MeshbluService.ACTION_SEND_AUTH_CREDENTIALS));

        startMeshbluService();
    }

    private void startMeshbluService() {
        if(meshbluServiceIntent != null){
            stopService(meshbluServiceIntent);
        }

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        String uuid = preferences.getString(UUID, null);
        String token = preferences.getString(TOKEN, null);

        meshbluServiceIntent = new Intent(this, MeshbluService.class);
        meshbluServiceIntent.putExtra(MeshbluService.UUID, uuid);
        meshbluServiceIntent.putExtra(MeshbluService.TOKEN, token);
        startService(meshbluServiceIntent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private Notification buildPersistentNotification() {
        Intent resumeIntent = new Intent(this, GatebluActivity.class);
        resumeIntent.setAction(RESUME);
        PendingIntent resumePendingIntent = PendingIntent.getActivity(this, 0, resumeIntent, 0);

        Intent stopIntent = new Intent(this, GatebluActivity.class);
        stopIntent.setAction(STOP);
        PendingIntent stopPendingIntent   = PendingIntent.getActivity(this, 1, stopIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_menu_manage);
        builder.setContentTitle("Gateblu is running");
        builder.setContentText("running, running, running");
        builder.setContentIntent(resumePendingIntent);
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);
        builder.setOngoing(true);

        return builder.build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gateblu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_stop_all_connectors).setVisible(connectorsAreRunning);
        menu.findItem(R.id.action_start_all_connectors).setVisible(!connectorsAreRunning);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_stop_all_connectors) {
            stopAllConnectors();
            return true;
        }

        if (id == R.id.action_start_all_connectors) {
            startAllConnectors();
            return true;
        }

        if (id == R.id.action_reset_gateblu) {
            showResetGatebluDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showResetGatebluDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Reset Gateblu?");
        dialogBuilder.setMessage("This will remove all devices and register an unclaimed Gateblu with Meshblu");
        dialogBuilder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetGateblu();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void resetGateblu() {
        stopAllConnectors();
        devices.clear();

        SharedPreferences.Editor preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0).edit();
        preferences.clear();
        preferences.commit();

        startMeshbluService();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device device = deviceGridAdapter.getItem(position);
        device.toggle();
    }

    private void startAllConnectors() {
        stopAllConnectors(); // For safety

        for (Device device : devices) {
            WebView webView = new WebView(this);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            webView.loadUrl("file:///android_asset/www/gateblu.html");
            webView.evaluateJavascript("window.meshbluDevice = {uuid: \"" + device.getUuid() + "\", token: \"" + device.getToken() + "\"};", new IgnoreReturnValue());
            webviews.add(webView);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, buildPersistentNotification());

        connectorsAreRunning = true;
        invalidateOptionsMenu();
    }

    private void stopAllConnectors() {
        for(WebView webView : webviews) {
            webView.destroy();
        }
        webviews.clear();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        connectorsAreRunning = false;
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(STOP.equals(getIntent().getAction())) {
            stopAllConnectors();
        }
    }

    private void onReceiveAuthCredentials(Intent intent) {
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

        refreshDeviceGrid();
        startMeshbluService();
    }

    private void refreshDeviceGrid() {
        deviceGridAdapter = new DeviceGridAdapter(getApplicationContext(), devices);
        gridView.setAdapter(deviceGridAdapter);
    }

    private class IgnoreReturnValue implements ValueCallback<String> {
        @Override
        public void onReceiveValue(String value) {}
    }
}
