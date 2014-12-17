package com.octoblu.gateblu;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

public class GatebluActivity extends ActionBarActivity {

    private final List<Device> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateblu);
        addDevices();

        DeviceGridAdapter adapter = new DeviceGridAdapter(getApplicationContext(), devices);

        GridView gridView = (GridView)findViewById(R.id.devices_grid);
        gridView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gateblu, menu);
        return true;
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
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
        devices.add(new Device("Blink(1)", "device:blink1"));
        devices.add(new Device("Hue", "device:hue"));
    }
}
