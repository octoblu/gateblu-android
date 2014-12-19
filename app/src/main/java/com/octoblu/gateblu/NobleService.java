package com.octoblu.gateblu;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by redaphid on 12/17/14.
 */
public class NobleService extends IntentService {
    public static final String TAG = "GatebluService";
    private NobleWebSocketServer webSocketServer;
    private Map<String, ScanResult> scanResultMap = new HashMap<>();
    private Map<Integer, BluetoothLeScanner> scannerMap = new HashMap<>();

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
            public void onScanListener(List<String> uuids, final int connId) {
                Log.w(TAG, "Some uuids: " + uuids);
                BluetoothAdapter adapter = bluetoothManager.getAdapter();

                if (adapter == null || !adapter.isEnabled()) {
                    Log.w(TAG, "No bluetooth adapter found, or bluetooth adapter is not enabled");
                    Toast.makeText(getApplicationContext(), "Bluetooth must be enabled", Toast.LENGTH_SHORT).show();
                    return;
                }
                BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();

                List<ScanFilter> scanFilters = new CopyOnWriteArrayList<ScanFilter>();
                for(String uuid : uuids){
                    scanFilters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(uuid)).build());
                }

                bluetoothLeScanner.startScan(scanFilters, new ScanSettings.Builder().build(), new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        scanResultMap.put(result.getDevice().getAddress(), result);
                        webSocketServer.sendDiscoveredDevice(result, connId);
                    }
                });

                scannerMap.put(connId, bluetoothLeScanner);
            }
        });

        webSocketServer.setOnStopScanListener(new NobleWebSocketServer.OnStopScanListener() {
            @Override
            public void onStopScanListener(int connIndex) {
                BluetoothLeScanner scanner = scannerMap.get(connIndex);
                if(scanner == null){
                    return;
                }

                scanner.stopScan(null);
                scannerMap.remove(connIndex);
            }
        });

        webSocketServer.setConnectListener(new NobleWebSocketServer.ConnectListener(){
            @Override
            public void onConnect(int connIndex, String deviceAddress) {
                ScanResult scanResult = scanResultMap.get(deviceAddress);
            }
        });

        webSocketServer.setDiscoverServicesListener(new NobleWebSocketServer.DiscoverServicesListener(){
            @Override
            public void onDiscoverServices(final int connIndex, final String deviceAddress, final List<String> uuids) {
                ScanResult scanResult = scanResultMap.get(deviceAddress);

                List<String> discoveredUuids = new ArrayList<>();
                for(ParcelUuid parcelUuid : scanResult.getScanRecord().getServiceUuids()) {
                    discoveredUuids.add(parcelUuid.getUuid().toString());
                }

                webSocketServer.sendDiscoveredServices(connIndex, deviceAddress, discoveredUuids);
            }
        });

        webSocketServer.setDiscoverCharacteristicsListener(new NobleWebSocketServer.DiscoverCharacteristicsListener(){
            @Override
            public void onDiscoverCharacteristics(int connIndex, String deviceAddress, List<String> characteristicUuids) {
                ScanResult scanResult = scanResultMap.get(deviceAddress);
                BluetoothGatt gatt = scanResult.getDevice().connectGatt(getApplicationContext(), true, new BluetoothGattCallback() {
                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);
                        Log.e(TAG, "" + gatt + characteristic + status);
                    }
                });
                for(String uuid : characteristicUuids) {
                    gatt.readCharacteristic(new BluetoothGattCharacteristic(UUID.fromString(uuid), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
                }
            }
        });

        webSocketServer.start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
