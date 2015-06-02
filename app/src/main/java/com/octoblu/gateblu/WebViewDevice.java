package com.octoblu.gateblu;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONObject;

public class WebViewDevice {
    private static final String TAG = "WebViewDevice";
    private final String connectorName, uuid, token;
    private final Context context;
    private WebView webView;

    public WebViewDevice(String connectorName, String uuid, String token, Context context) {
        this.connectorName = connectorName;
        this.uuid = uuid;
        this.token = token;
        this.context = context;
        this.webView = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void start() {
        Log.i(TAG, "start: " + uuid);
        stop();
        this.webView = generateWebView();
    }

    public void stop(){
        if(this.webView == null){
            return;
        }

        this.webView.destroy();
    }

    public static WebViewDevice fromJSONObject(JSONObject data, Context context) {
        SaneJSONObject saneData = SaneJSONObject.fromJSONObject(data);
        String connectorName = saneData.getStringOrThrow("connector");
        String uuid = saneData.getStringOrThrow("uuid");
        String token = saneData.getStringOrThrow("token");

        return new WebViewDevice(connectorName, uuid, token, context);
    }

    private WebView generateWebView() {
        WebView webView = new WebView(context);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);

        webView.addJavascriptInterface(new ConnectorEventEmitter() {
            @Override
            @JavascriptInterface
            public void on(String event, String jsonData) {
                Log.i(TAG, "on("+event+"): " + jsonData);
            }
        }, "ConnectorEventListener");
        webView.clearCache(true);
        webView.loadUrl("file:///android_asset/www/device.html");
        webView.evaluateJavascript("window.connectorName = \"" + connectorName + "\"", new Util.IgnoreReturnValue());
        webView.evaluateJavascript("window.meshbluJSON = {uuid: \"" + uuid + "\", token: \"" + token + "\"};", new Util.IgnoreReturnValue());
        return webView;
    }

    public interface ConnectorEventEmitter {
        @JavascriptInterface
        void on(String event, String jsonData);
    }
}
