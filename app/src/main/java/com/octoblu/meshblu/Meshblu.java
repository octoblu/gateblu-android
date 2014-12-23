package com.octoblu.meshblu;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Meshblu extends Emitter {
    public static final String DEFAULT_MESHBLU_HOST = "meshblu.octoblu.com";
    public static final int DEFAULT_MESHBLU_PORT = 80;
    public static final String TAG = "meshblu:Meshblu";
    public static final String EVENT_IDENTIFY = "identify";
    public static final String EVENT_REGISTER = "register";
    public static final String EVENT_IDENTITY = "identity";
    /**
     * Emitted with no arguments
     */
    public static final String EVENT_READY = "ready";

    private final Socket socket;

    private String uuid, token;

    public Meshblu() throws URISyntaxException {
        this(null, null);
    }

    public Meshblu(String uuid, String token) throws URISyntaxException {
        this(uuid, token, DEFAULT_MESHBLU_HOST, DEFAULT_MESHBLU_PORT);
    }

    public Meshblu(String uuid, String token, String host, int port) throws URISyntaxException {
        this.uuid = uuid;
        this.token = token;
        String scheme = (port == 443) ? "https" : "http";
        URI uri = new URI(scheme, null, host, port, null, null, null);

        socket = IO.socket(uri.toString());
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "EVENT_CONNECT");
            }
        });

        socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Exception error = (Exception) args[0];
                Log.e(TAG, "EVENT_ERROR", error);
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String reason = (String) args[0];
                Log.d(TAG, "EVENT_DISCONNECT: " + reason);
            }
        });

        socket.on(EVENT_IDENTIFY, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "EVENT_IDENTIFY");
                authorize();
            }
        });

        socket.on(EVENT_READY, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "EVENT_READY");
                emit(EVENT_READY, args);
            }
        });
    }

    private void authorize(){
        if(uuid == null || token == null){
            emitRegister();
            return;
        }
        emitIdentity();
    }

    public void connect() {
        socket.connect();
    }

    private void emitIdentity() {
        JSONObject identityJSON = new JSONObject();
        try {
            identityJSON.put("uuid", uuid);
            identityJSON.put("token", token);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting identity", e);
            return;
        }
        socket.emit(EVENT_IDENTITY, identityJSON);
    }

    private void emitRegister() {
        JSONObject registerJSON = new JSONObject();
        try {
            registerJSON.put("type", "device:gateblu:android");
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting register", e);
            return;
        }

        socket.emit(EVENT_REGISTER, new JSONObject[]{registerJSON}, new Ack() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                Log.d(TAG, "EVENT_REGISTER: " + data.toString());

                try {
                    setUuid(data.getString("uuid"));
                    setToken(data.getString("token"));
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing registration data", e);
                    return;
                }

                emitIdentity();
            }
        });
    }

    public String getUuid() {
        return uuid;
    }

    public String getToken() {
        return token;
    }

    private void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private void setToken(String token) {
        this.token = token;
    }
}
