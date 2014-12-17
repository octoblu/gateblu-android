package com.octoblu.gateblu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GatebluActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, CordovaInterface {

    private final List<Device> devices = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private CordovaWebView plugin;
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

        plugin = (CordovaWebView)findViewById(R.id.deviceManager);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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

    @Override
    public void startActivityForResult(CordovaPlugin cordovaPlugin, Intent intent, int i) {
        //I don't know what this is!
    }

    @Override
    public void setActivityResultCallback(CordovaPlugin cordovaPlugin) {

    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Object onMessage(String s, Object o) {
        Log.i("DeviceManager", s);
        return null;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }

}
