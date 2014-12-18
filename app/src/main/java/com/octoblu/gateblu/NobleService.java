package com.octoblu.gateblu;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Created by redaphid on 12/17/14.
 */
public class NobleService extends IntentService {
    private NobleWebSocketServer webSocketServer;


    public NobleService() {
        super("NobleService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        webSocketServer = new NobleWebSocketServer(new InetSocketAddress(0xB1E));

        webSocketServer.setOnScanListener(new NobleWebSocketServer.OnScanListener() {
            @Override
            public void onScanListener(UUID[] uuids, final int connId) {
                Log.d("GatebluService", "Finding a bluetooth adapter");
                BluetoothAdapter adapter = bluetoothManager.getAdapter();

                if (adapter == null || !adapter.isEnabled()) {
                    Log.d("GatebluService", "No bluetooth adapter found.");
                    Toast.makeText(getApplicationContext(), "Bluetooth must be enabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                adapter.startLeScan(uuids, new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        webSocketServer.sendDiscoveredDevice(device, rssi, connId);
                    }
                });

            }
        });

        webSocketServer.start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
