package com.octoblu.gateblu;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

public class GatebluActivity extends AppCompatActivity {
    public static final String TAG = "GatebluActivity";

    //region Variables
    private DeviceGridAdapter deviceGridAdapter;

    private GridView devicesGridView;
    private LinearLayout offView;
    private LinearLayout claimGatebluView;
    private LinearLayout noDevicesView;
    private LinearLayout loadingSpinner;
    private GatebluApplication application;
    //endregion

    // region Activity Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_gateblu);

        offView = (LinearLayout) findViewById(R.id.off_info);
        devicesGridView = (GridView)findViewById(R.id.devices_grid);
        noDevicesView = (LinearLayout) findViewById(R.id.no_devices_info);
        claimGatebluView = (LinearLayout) findViewById(R.id.claim_gateblu);
        loadingSpinner = (LinearLayout) findViewById(R.id.loading_spinner);

        final Button button = (Button) findViewById(R.id.claim_gateblu_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                GatebluActivity.this.claimGateblu(v);
            }
        });

        application = (GatebluApplication) getApplication();
        application.on(GatebluApplication.CONFIG, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidateOptionsMenu();
                        refreshDeviceGrid();
                    }
                });
            }
        });

        application.on(GatebluApplication.CLAIM_GATEBLU, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String uuid = (String) args[0];
                String token = (String) args[1];
                claimGatebluInOctoblu(uuid, token);
            }
        });

        refreshDeviceGrid();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        application.off();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
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
        boolean stopped = application.getState() == "off";
        menu.findItem(R.id.action_stop_gateblu).setVisible(!stopped);
        menu.findItem(R.id.action_start_gateblu).setVisible(stopped);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_show_uuid:
                showUuidDialog();
                return true;
            case R.id.action_reset_gateblu:
                showResetGatebluDialog();
                return true;
            case R.id.action_stop_gateblu:
                application.stop();
                return true;
            case R.id.action_start_gateblu:
                application.start();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    // region View Helpers
    private void claimGateblu(View view){
        Log.d(TAG, "I wants to claim gateblu");
        application.claimGateblu();
    }

    private void claimGatebluInOctoblu(String uuid, String token){
        String url = String.format("https://app.octoblu.com/node-wizard/claim/%s/%s", uuid, token);
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
        application.whoami();
    }

    private void refreshDeviceGrid() {
        String state = application.getState();
        switch (state) {
            case GatebluApplication.STATES.OFF:
                showOff();
                break;
            case GatebluApplication.STATES.LOADING:
                showLoading();
                break;
            case GatebluApplication.STATES.CLAIM_GATEBLU:
                showClaimGateblu();
                break;
            case GatebluApplication.STATES.NO_DEVICES:
                showNoDevices();
                break;
            default:
                showReady();
                break;
        }
        setTitle(application.getName());
        invalidateOptionsMenu();
    }

    private void showOff() {
        hideAll();
        offView.setVisibility(View.VISIBLE);
    }

    private void showClaimGateblu() {
        hideAll();
        setRandomRobotImage();
        claimGatebluView.setVisibility(View.VISIBLE);
    }

    private void showNoDevices() {
        hideAll();
        setRandomRobotImage();
        noDevicesView.setVisibility(View.VISIBLE);
    }

    private void showLoading() {
        hideAll();
        loadingSpinner.setVisibility(View.VISIBLE);
    }

    private void showReady() {
        hideAll();
        devicesGridView.setVisibility(View.VISIBLE);
        deviceGridAdapter = new DeviceGridAdapter(getApplicationContext(), application.getDevices());
        devicesGridView.setAdapter(deviceGridAdapter);
    }

    private void hideAll() {
        offView.setVisibility(View.GONE);
        noDevicesView.setVisibility(View.GONE);
        devicesGridView.setVisibility(View.GONE);
        loadingSpinner.setVisibility(View.GONE);
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


    private void showUuidDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View uuidDialogView = inflater.inflate(R.layout.uuid_dialog, null);
        TextView gatebluUuidTextView = (TextView) uuidDialogView.findViewById(R.id.gateblu_uuid);
        TextView gatebluTokenTextView = (TextView) uuidDialogView.findViewById(R.id.gateblu_token);

        gatebluUuidTextView.setText(application.getUuid());
        gatebluTokenTextView.setText(application.getToken());

        uuidDialogView.findViewById(R.id.action_copy_uuid).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(getApplication(), "UUID copied to clipboard", Toast.LENGTH_SHORT).show();
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(application.UUID, application.getUuid()));
                return false;
            }
        });
        uuidDialogView.findViewById(R.id.action_copy_token).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(getApplication(), "Token copied to clipboard", Toast.LENGTH_SHORT).show();
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(application.TOKEN, application.getToken()));
                return false;
            }
        });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(uuidDialogView);
        dialogBuilder.setTitle("Tap UUID or token to copy.");
        dialogBuilder.setNegativeButton("Close", null);
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }
    // endregion

    private int getRandomRobotResourceId() {
        return getResources().getIdentifier("robot" + Util.randInt(1, 9), "drawable", getPackageName());
    }
}
