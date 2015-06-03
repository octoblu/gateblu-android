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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;

import org.json.JSONObject;

import java.net.UnknownHostException;
import java.util.List;

public class Gateblu extends Emitter {
    public static final String TAG = "Gateblu";
    public static final String CONFIG = "config";
    public static final String REGISTER = "register";
    private final WebViewDeviceManager deviceManager;
    private final Context context;
    private WebView webView;
    private DeviceManagerServer server;
    private String uuid, token;

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
            server.stop();
        }
    }

    private void register() {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://meshblu.octoblu.com/devices";
        SaneJSONObject data = new SaneJSONObject();
        data.putOrIgnore("type", "device:gateblu");

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
}