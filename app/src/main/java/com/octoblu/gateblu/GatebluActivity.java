package com.octoblu.gateblu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GatebluActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private final List<Device> devices = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private WebView plugin;
    private DeviceGridAdapter deviceGridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");

        setContentView(R.layout.activity_gateblu);
        addDevices();

        deviceGridAdapter = new DeviceGridAdapter(getApplicationContext(), devices);
        GridView gridView = (GridView)findViewById(R.id.devices_grid);
        gridView.setAdapter(deviceGridAdapter);
        gridView.setOnItemClickListener(this);

        plugin = (WebView)findViewById(R.id.deviceManager);
        WebSettings settings = plugin.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        plugin.loadUrl("file:///android_asset/www/gateblu.html");

        Intent serviceIntent = new Intent(this, NobleService.class);
        startService(serviceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gateblu, menu);
        return true;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device device = deviceGridAdapter.getItem(position);
        device.toggle();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addDevices() {
        for(int i = 0; i < 20; i++) {
            devices.add(new Device("Blink(1)", "device:blink1"));
            devices.add(new Device("Hue", "device:hue"));
        }
    }
}
