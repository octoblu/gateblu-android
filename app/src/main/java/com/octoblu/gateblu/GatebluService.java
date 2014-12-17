package com.octoblu.gateblu;

import android.app.IntentService;
import android.content.Intent;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Created by redaphid on 12/17/14.
 */
public class GatebluService extends IntentService {
    private WebSocketServer webSocketServer;


    public GatebluService() {
        super("GatebluService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        webSocketServer = new GatebluWebSocketServer(new InetSocketAddress(0xB1E));
        webSocketServer.start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
