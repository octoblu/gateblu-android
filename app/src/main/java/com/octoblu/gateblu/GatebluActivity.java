package com.octoblu.gateblu;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GatebluActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "Gateblu:GatebluActivity";
    private final List<Device> devices = new ArrayList<>();
    private final List<WebView> webviews = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private DeviceGridAdapter deviceGridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        setContentView(R.layout.activity_gateblu);
        this.devices.addAll(generateDevices());

        deviceGridAdapter = new DeviceGridAdapter(getApplicationContext(), devices);
        GridView gridView = (GridView)findViewById(R.id.devices_grid);
        gridView.setAdapter(deviceGridAdapter);
        gridView.setOnItemClickListener(this);

        Intent nobleServiceIntent = new Intent(this, NobleService.class);
        startService(nobleServiceIntent);

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

        Notification notification = buildPersistentNotification();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    private List<Device> generateDevices() {
        List<Device> devices = new ArrayList<>(2);
        devices.add(new Device("Blight", "device:bean", "e6596bd1-86da-11e4-a63b-43e66bcc8635", "0lbyf226ug1u4n29ut86ktq3le8c9pb9"));
        devices.add(new Device("Pestilence", "device:bean", "4d622a21-87ca-11e4-ab86-37bc0463e7ff", "000t127u0htsgiudi2d72oqqet6mkj4i"));
        return devices;
    }

    private Notification buildPersistentNotification() {
        Intent intent = new Intent(this, GatebluActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_menu_manage);
        builder.setContentTitle("Gateblu is running");
        builder.setContentText("running, running, running");
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;

        return notification;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gateblu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device device = deviceGridAdapter.getItem(position);
        device.toggle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "I have this many: " + webviews.size());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private class IgnoreReturnValue implements ValueCallback<String> {
        @Override
        public void onReceiveValue(String value) {

        }
    }
}
