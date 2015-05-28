package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WebViewDeviceManager implements DeviceManager {

    public static final String TAG = "WebViewDeviceManager";
    private final Context context;
    private final ArrayList<WebView> devices;
    private final Handler uiThreadHandler;

    public WebViewDeviceManager(Context context, Handler uiThreadHandler) {
        this.context = context;
        this.uiThreadHandler = uiThreadHandler;
        this.devices = new ArrayList<>();
    }


    @Override
    public void addDevice(final JSONObject data) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "addDevice: " + data.toString());
                WebView webView = new WebView(context);
                WebSettings settings = webView.getSettings();

                String connectorName,uuid,token;
                try {
                    connectorName = data.getString("connector");
                    uuid= data.getString("uuid");
                    token= data.getString("token");
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }

                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setAllowFileAccess(true);
                settings.setAllowFileAccessFromFileURLs(true);

                webView.loadUrl("file:///android_asset/www/device.html");
                webView.evaluateJavascript("window.connectorName = \"" + connectorName + "\"", new Util.IgnoreReturnValue());
                webView.evaluateJavascript("window.meshbluJSON = {uuid: \"" + uuid + "\", token: \"" + token + "\"};", new Util.IgnoreReturnValue());

                devices.add(webView);
            }
        });
    }

    @Override
    public void removeDevice(JSONObject data) {
        Log.i(TAG, "removeDevice");
    }

    @Override
    public void startDevice(JSONObject data) {
        Log.i(TAG, "startDevice");
    }

    @Override
    public void stopDevice(JSONObject data) {
        Log.i(TAG, "stopDevice");
    }
}
