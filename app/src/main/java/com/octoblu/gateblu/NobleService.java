package com.octoblu.gateblu;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NobleService extends IntentService {
    public static final String TAG = "NobleService";
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final String ACTION_NO_BLUETOOTH_ADAPTER_FOUND = "noBluetoothAdapterFound";

    private NobleWebSocketServer webSocketServer;
    private Map<String, ScanResult> scanResultMap = new HashMap<>();
    private Map<String, BluetoothGatt> gattMap = new HashMap<>();
    private Map<Integer, BluetoothLeScanner> scannerMap = new HashMap<>();
    private LocalBroadcastManager localBroadcastManager;
    private boolean didItOnce = false;

    public NobleService() {
        super("NobleService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        webSocketServer = new NobleWebSocketServer(new InetSocketAddress(0xB1E));

        webSocketServer.setOnScanListener(new NobleWebSocketServer.OnScanListener() {
            @Override
            public void onScanListener(List<String> uuids, final int connId) {
                BluetoothAdapter adapter = bluetoothManager.getAdapter();

                if (adapter == null || !adapter.isEnabled()) {
                    Log.w(TAG, "No bluetooth adapter found, or bluetooth adapter is not enabled");
                    localBroadcastManager.sendBroadcast(new Intent(ACTION_NO_BLUETOOTH_ADAPTER_FOUND));
                    return;
                }
                BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();

                List<ScanFilter> scanFilters = new ArrayList<>();
                for(String uuid : uuids){
                    ParcelUuid parcelUuid = ShortableParcelUuid.fromString(uuid);
                    ScanFilter.Builder builder = new ScanFilter.Builder();
                    ScanFilter scanFilter = builder.setServiceUuid(parcelUuid).build();

                    scanFilters.add(scanFilter);
                }

                bluetoothLeScanner.startScan(scanFilters, new ScanSettings.Builder().build(), new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        Log.d(TAG, "Scanned a thing!: " + result.getDevice().getName());
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
            public void onConnect(final int connIndex, final String deviceAddress) {
                ScanResult scanResult = scanResultMap.get(deviceAddress);
                BluetoothGatt gatt = scanResult.getDevice().connectGatt(getApplicationContext(), true, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if(newState == BluetoothProfile.STATE_CONNECTED){
                            Log.d(TAG, "Connected to: " + deviceAddress);
                            webSocketServer.sendConnectedToDevice(connIndex, deviceAddress);
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);

                        for(BluetoothGattService service : gatt.getServices()) {
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                            webSocketServer.sendDiscoveredCharacteristics(connIndex, deviceAddress, service.getUuid().toString(), characteristics);
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicChanged(gatt, characteristic);
                        Log.d(TAG, "onCharacteristicChanged");

                        String serviceUuid = characteristic.getService().getUuid().toString();
                        String characteristicUuid = characteristic.getUuid().toString();
                        String data = bytesToHex(characteristic.getValue());

                        webSocketServer.sendRead(connIndex, deviceAddress, serviceUuid, characteristicUuid, data);
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);

                        Log.d(TAG, "onCharacteristicRead: success: " + (BluetoothGatt.GATT_SUCCESS == status));

                        String serviceUuid = characteristic.getService().getUuid().toString();
                        String characteristicUuid = characteristic.getUuid().toString();
                        String data = bytesToHex(characteristic.getValue());

                        webSocketServer.sendRead(connIndex, deviceAddress, serviceUuid, characteristicUuid, data);
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicWrite(gatt, characteristic, status);
                        Log.d(TAG, "onCharacteristicWrite");
                    }
                });
                gattMap.put(deviceAddress, gatt);
            }
        });

        webSocketServer.setDisconnectListener(new NobleWebSocketServer.DisconnectListener(){
            @Override
            public void onDisconnect() {
                for(BluetoothGatt gatt : gattMap.values()){
                    gatt.disconnect();
                }
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
            public void onDiscoverCharacteristics(final int connIndex, final String deviceAddress, final String serviceUuid, List<String> characteristicUuids) {
                ScanResult scanResult = scanResultMap.get(deviceAddress);
                BluetoothGatt gatt = gattMap.get(deviceAddress);
                gatt.discoverServices();
            }
        });

        webSocketServer.setWriteListener(new NobleWebSocketServer.WriteListener() {
            @Override
            public void write(int connIndex, String deviceAddress, String serviceUuid, String characteristicUuid, String data) {
                BluetoothGatt gatt = gattMap.get(deviceAddress);
                BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString(serviceUuid)).getCharacteristic(UUID.fromString(characteristicUuid));
                characteristic.setValue(hexStringToByteArray(data));
                gatt.writeCharacteristic(characteristic);
            }
        });

        webSocketServer.setNotifyListener(new NobleWebSocketServer.NotifyListener(){
            @Override
            public void notifyDevice(int connIndex, String deviceAddress, String serviceUuid, String characteristicUuid, boolean setNotify) {
                BluetoothGatt gatt = gattMap.get(deviceAddress);
                BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString(serviceUuid)).getCharacteristic(UUID.fromString(characteristicUuid));
                gatt.setCharacteristicNotification(characteristic, setNotify);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);

                webSocketServer.sendNotify(connIndex, deviceAddress, serviceUuid, characteristicUuid, setNotify);
            }
        });

        webSocketServer.start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
