package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import android.webkit.WebSettings;
import android.webkit.WebView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;
import com.octoblu.meshblukit.Meshblu;
import com.octoblu.sanejsonobject.SaneJSONObject;

import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Gateblu extends Emitter {
    public static final String TAG = "Gateblu";
    public static final String CONFIG = "config";
    public static final String REGISTER = "register";
    public static final String WHOAMI = "whoami";
    public static final String READY = "ready";
    public static final String GENERATED_TOKEN = "generated_token";
    private final WebViewDeviceManager deviceManager;
    private final Context context;
    private WebView webView;
    private DeviceManagerServer server;
    private String uuid, token, owner, name;

    public Gateblu(String uuid, String token, Context context, Handler uiThreadHandler) {
        this.uuid = uuid;
        this.token = token;
        this.context = context;
        this.deviceManager = new WebViewDeviceManager(context, uiThreadHandler);
        this.deviceManager.on(DeviceManager.CONFIG, new Listener() {
            @Override
            public void call(Object... args) {
                Gateblu.this.emit(CONFIG, args);
            }
        });
        this.deviceManager.on(DeviceManager.READY, new Listener() {
            @Override
            public void call(Object... args){
                Log.d(TAG, "Device Manager Ready, going to gateblu device");
                Gateblu.this.emit(READY, args);
            }
        });
    }

    private WebView buildWebView(Context context) {
        WebView webView = new WebView(context);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        return webView;
    }

    public boolean isReady(){
        return deviceManager.isReady();
    }

    public boolean hasNoDevices() {
        return deviceManager.hasNoDevices();
    }

    public List<Device> getDevices(){
        return deviceManager.getDevices();
    }

    public Boolean hasOwner(){
        if(owner == null){
            return false;
        }
        return !owner.isEmpty();
    }

    public void setOwner(String newOwner){
        owner = newOwner;
    }

    public String getName() { return name; }
    public void setName(String newName) { name = newName; }

    public void restart(){
        stop();
        start();
    }

    private void start() {
        if(uuid == null || token == null) {
            register();
            return;
        }


        try {
            webView = buildWebView(context);
            server = new DeviceManagerServer(deviceManager);
            server.start();
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        webView.clearCache(true);
        webView.loadUrl("file:///android_asset/www/gateblu.html");
        webView.evaluateJavascript("window.meshbluJSON = {uuid: \"" + uuid + "\", token: \"" + token + "\"};", new Util.IgnoreReturnValue());
    }

    public void stop() {
        if(webView != null) {
            webView.destroy();
        }

        if(server != null) {
            try {
                server.stop(0);
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Error Stopping Server", e);
            }
        }
    }

    private void register() {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://meshblu.octoblu.com/devices";
        SaneJSONObject data = new SaneJSONObject();
        data.putOrIgnore("type", "device:gateblu");
        data.putOrIgnore("platform", "android");

        JsonObjectRequest registerRequest = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                emit(REGISTER, jsonObject);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });

        queue.add(registerRequest);
    }

    private void update() {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://meshblu.octoblu.com/devices";
        SaneJSONObject data = new SaneJSONObject();
        data.putOrIgnore("type", "device:gateblu");
        data.putOrIgnore("platform", "android");

        JsonObjectRequest registerRequest = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                emit(REGISTER, jsonObject);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });

        queue.add(registerRequest);
    }

    public void generateToken(String uuid) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = String.format("https://meshblu.octoblu.com/devices/%s/tokens", uuid);
        SaneJSONObject data = new SaneJSONObject();

        JsonObjectRequest generateTokenRequest = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                emit(GENERATED_TOKEN, jsonObject);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("meshblu_auth_uuid", Gateblu.this.uuid);
                params.put("meshblu_auth_token", Gateblu.this.token);
                return params;
            }
        };;

        queue.add(generateTokenRequest);
    }

    public void whoami() {
        Log.d(TAG, "Getting gateblu device");
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://meshblu.octoblu.com/v2/whoami";

        JsonObjectRequest whoamiRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) { emit(WHOAMI, jsonObject); }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) { Log.e(TAG, "Error getting device", volleyError); }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String>  params = new HashMap<String, String>();
                        params.put("meshblu_auth_uuid", Gateblu.this.uuid);
                        params.put("meshblu_auth_token", Gateblu.this.token);
                        return params;
                    }
                };

        queue.add(whoamiRequest);
    }
}