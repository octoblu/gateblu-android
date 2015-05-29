package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.net.UnknownHostException;

public class Gateblu {
    public static final String TAG = "Gateblu";
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

    public void restart(){
        stop();
        start();
    }

    private void start() {
        try {
            webView = buildWebView(context);
            server = new DeviceManagerServer(deviceManager);
            server.start();
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        webView.loadUrl("file:///android_asset/www/gateblu.html");
        webView.evaluateJavascript("window.meshbluJSON = {uuid: \"" + uuid + "\", token: \"" + token + "\"};", new Util.IgnoreReturnValue());
        webView.evaluateJavascript("localStorage.setItem('debug', '*')", new Util.IgnoreReturnValue());
    }

    public void stop() {
        if(webView != null) {
            webView.destroy();
        }

        if(server != null) {
            server.stop();
        }
    }
}