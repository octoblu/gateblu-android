package com.octoblu.gateblu;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;
import com.octoblu.sanejsonobject.SaneJSONObject;

import org.json.JSONObject;

public class WebViewDevice extends Emitter {
    private static final String TAG = "WebViewDevice";
    public static final String CONFIG = "config";
    public static final String SEND_LOG = "send_log";
    private final Context context;
    private WebView webView = null;
    private Device device;

    public WebViewDevice(SaneJSONObject deviceJSONObject, Context context) {
        this.device = new Device(deviceJSONObject);
        this.context = context;
        this.webView = null;
    }

    public String getUuid() {
        return this.device.getUuid();
    }

    public Device toDevice(){
        return this.device;
    }

    private void setConfig(String jsonData) {
        SaneJSONObject json = SaneJSONObject.fromString(jsonData);
        this.device.fromJSONObject(json);
        emit(CONFIG);
    }

    private void sendLogMessage(String jsonData) {
        SaneJSONObject json = SaneJSONObject.fromString(jsonData);
        emit(SEND_LOG, json);
    }

    public void start() {
        Log.i(TAG, "start: " + this.device.getUuid());
        stop();
        this.webView = generateWebView();
    }

    public void stop(){
        if(this.webView == null){
            return;
        }

        this.webView.destroy();
    }

    private WebView generateWebView() {
        WebView webView = new WebView(context);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);

        webView.addJavascriptInterface(new ConnectorEventEmitter() {
            @Override
            @JavascriptInterface
            public void on(String event, String jsonData) {
                Log.i(TAG, "on(" + event + "): " + jsonData);
                if (event.equals("config")) {
                    setConfig(jsonData);
                }
                if (event.equals("send_log")) {
                    sendLogMessage(jsonData);
                }
            }
        }, "ConnectorEventListener");
        webView.clearCache(true);
        webView.loadUrl("file:///android_asset/www/device.html");
        webView.evaluateJavascript("window.connectorName = \"" + this.device.getConnector() + "\"", new Util.IgnoreReturnValue());
        webView.evaluateJavascript("window.meshbluJSON = {uuid: \"" + this.device.getUuid() + "\", token: \"" + this.device.getToken() + "\"};", new Util.IgnoreReturnValue());
        
        return webView;
    }

    public static WebViewDevice fromJSONObject(JSONObject data, Context context) {
        return new WebViewDevice(SaneJSONObject.fromJSONObject(data), context);
    }

    public interface ConnectorEventEmitter {
        @JavascriptInterface
        void on(String event, String jsonData);
    }
}
