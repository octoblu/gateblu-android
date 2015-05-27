package com.octoblu.gateblu;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.net.UnknownHostException;

public class Gateblu {
    public static final String TAG = "Gateblu";
    String uuid,token;
    Context context;

    public Gateblu(String uuid, String token, Context context) {
        this.uuid = uuid;
        this.token = token;
        this.context = context;
    }

    public void run() {
        try {
            DeviceManagerServer server = new DeviceManagerServer(new WebViewDeviceManager());
            server.start();
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        WebView webView = new WebView(context);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);

        webView.loadUrl("file:///android_asset/www/gateblu.html");
        webView.evaluateJavascript("window.meshbluJSON = {uuid: \"" + uuid + "\", token: \"" + token + "\"};", new Util.IgnoreReturnValue());
        webView.evaluateJavascript("localStorage.setItem('debug', '*')", new Util.IgnoreReturnValue());
    }
}
