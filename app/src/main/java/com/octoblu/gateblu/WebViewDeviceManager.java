package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
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

                SaneJSONObject saneData = SaneJSONObject.fromJSONObject(data);
                String connectorName = saneData.getStringOrNull("connector");
                String uuid = saneData.getStringOrNull("uuid");
                String token = saneData.getStringOrNull("token");

                if (connectorName == null || uuid == null || token == null) {
                    Log.e(TAG, "addDevice error, missing connectorName, uuid, or token");
                    return;
                }

                WebView webView = generateWebView(connectorName, uuid, token);
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

    @Override
    public void stopAll() {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for(WebView device : devices) {
                    device.destroy();
                }

                devices.clear();
            }
        });
    }

    private WebView generateWebView(String connectorName, String uuid, String token) {
        WebView webView = new WebView(context);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);

        webView.addJavascriptInterface(new ConnectorEventEmitter() {
            @Override
            public void on(String event, String jsonData) {

            }
        }, "ConnectorEventListener");
        webView.loadUrl("file:///android_asset/www/device.html");
        webView.evaluateJavascript("window.connectorName = \"" + connectorName + "\"", new Util.IgnoreReturnValue());
        webView.evaluateJavascript("window.meshbluJSON = {uuid: \"" + uuid + "\", token: \"" + token + "\"};", new Util.IgnoreReturnValue());
        return webView;
    }

    private String getStringOrNull(JSONObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    public interface ConnectorEventEmitter {
        @JavascriptInterface
        void on(String event, String jsonData);
    }
}
