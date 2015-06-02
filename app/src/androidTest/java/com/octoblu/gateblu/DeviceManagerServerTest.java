package com.octoblu.gateblu;

import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;

@RunWith(AndroidJUnit4.class)
public class DeviceManagerServerTest extends AndroidTestCase {

    public static final String WS_LOCALHOST_D00D = "ws://localhost:53261";
    public static final int TIMEOUT = 5000;
    private static String TAG = "DeviceManagerServerTest";
    private DeviceManagerServer deviceManagerServer;
    private GatebluWebsocketClient client;
    private MockDeviceManager deviceManager;


    @Before
    public void setUp() throws Exception {
        deviceManager = new MockDeviceManager();

        deviceManagerServer = new DeviceManagerServer(deviceManager);
        deviceManagerServer.start();

        client = new GatebluWebsocketClient(new URI(WS_LOCALHOST_D00D));
        client.connectBlocking();

    }

    @Test
    public void testAddDeviceSendsResponse() throws Exception {
        client.send("{\"action\": \"addDevice\", \"id\": \"some-uuid\"}");

        for (int waitTime = 0; waitTime < TIMEOUT; waitTime += 100) {
            if(client.lastMessage != null) {
                return; // Test passed
            }
            Thread.sleep(100);
        }

        throw new Exception("never received a response");
    }

    @Test
    public void testAddDeviceCallsDeviceManagerAddDevice() throws Exception {
        client.send("{\"action\": \"addDevice\", \"id\": \"some-uuid\"}");

        for (int waitTime = 0; waitTime < TIMEOUT; waitTime += 100) {
            if(deviceManager.addDeviceCalled) {
                return; // Test passed
            }
            Thread.sleep(100);
        }

        throw new Exception("deviceManager.addDevice was never called");
    }






    private class GatebluWebsocketClient extends WebSocketClient {
        public Exception lastException = null;
        public String lastMessage = null;
        private boolean isOpen = false;

        public GatebluWebsocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            this.isOpen = true;
        }

        @Override
        public void onMessage(String message) {
            this.lastMessage = message;
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

        }

        @Override
        public void onError(Exception lastException) {
            this.lastException = lastException;
        }
    }

    private class MockDeviceManager implements DeviceManager {
        public boolean addDeviceCalled = false;

        @Override
        public void addDevice(JSONObject data) {
            this.addDeviceCalled = true;
        }

        @Override
        public void removeDevice(JSONObject data) {

        }

        @Override
        public void startDevice(JSONObject data) {

        }

        @Override
        public void stopDevice(JSONObject data) {

        }

        @Override
        public void stopAll() {

        }

    }
}
