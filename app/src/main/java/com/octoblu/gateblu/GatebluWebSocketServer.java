package com.octoblu.gateblu;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Created by redaphid on 12/17/14.
 */
public class GatebluWebSocketServer extends WebSocketServer {

    public GatebluWebSocketServer(InetSocketAddress address) {
        super(address);
        Log.d("GatebluWebsocket", "Instantiated Server");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d("GatebluWebsocket", "something connected!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d("GatebluWebsocket", "something sent us a message!");
        Log.d("GatebluWebsocket", message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }
}
