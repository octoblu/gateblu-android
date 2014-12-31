package com.octoblu.gateblu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;

public class GatebluActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    public static final String TAG = "Gateblu:GatebluActivity";

    //region Variables
    private DeviceGridAdapter deviceGridAdapter;

    private GridView gridView;
    private LinearLayout noDevicesInfoView;
    private LinearLayout spinner;
    private TextView spinnerText;
    private GatebluApplication application;
    //endregion

    // region Activity Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        setContentView(R.layout.activity_gateblu);

        gridView = (GridView)findViewById(R.id.devices_grid);
        noDevicesInfoView = (LinearLayout) findViewById(R.id.no_devices_info);
        spinner = (LinearLayout) findViewById(R.id.loading_spinner);
        spinnerText = (TextView) findViewById(R.id.loading_spinner_text);

        gridView.setOnItemClickListener(this);

        application = (GatebluApplication) getApplication();
        application.on(GatebluApplication.EVENT_DEVICES_UPDATED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                invalidateOptionsMenu();
                refreshDeviceGrid();
            }
        });

        refreshDeviceGrid();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        application.off();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        refreshDeviceGrid();
    }
    // endregion

    //region Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gateblu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_stop_all_connectors).setVisible(application.areConnectorsRunning());
        menu.findItem(R.id.action_start_all_connectors).setVisible(!application.areConnectorsRunning());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_stop_all_connectors:
                application.turnConnectorsOff();
                return true;
            case R.id.action_start_all_connectors:
                application.turnConnectorsOn();
                return true;
            case R.id.action_reset_gateblu:
                showResetGatebluDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    // region Event Listeners
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device device = deviceGridAdapter.getItem(position);
        device.toggle();
    }
    // endregion

    // region View Helpers
    private void refreshDeviceGrid() {
        if(!application.hasMeshbluConnected()){
            gridView.setVisibility(View.GONE);
            noDevicesInfoView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
            spinnerText.setText(R.string.connecting_to_meshblu_header);
            return;
        }
        if(!application.hasFetchedDevices()) {
            gridView.setVisibility(View.GONE);
            noDevicesInfoView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
            spinnerText.setText(R.string.fetching_devices_header);
            return;
        }
        if (application.hasNoDevices()) {
            setRandomRobotImage();
            gridView.setVisibility(View.GONE);
            noDevicesInfoView.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.GONE);
            return;
        }

        gridView.setVisibility(View.VISIBLE);
        noDevicesInfoView.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        deviceGridAdapter = new DeviceGridAdapter(getApplicationContext(), application.getDevices());
        gridView.setAdapter(deviceGridAdapter);
    }

    private void setRandomRobotImage() {
        ImageView robotImageView = (ImageView) findViewById(R.id.robot_image);
        robotImageView.setImageResource(getRandomRobotResourceId());
    }


    private void showResetGatebluDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Reset Gateblu?");
        dialogBuilder.setMessage("This will remove all devices and register an unclaimed Gateblu with Meshblu");
        dialogBuilder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                application.resetGateblu();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }
    // endregion


    private int getRandomRobotResourceId() {
        return getResources().getIdentifier("robot" + Util.randInt(1, 9), "drawable", getPackageName());
    }
}
