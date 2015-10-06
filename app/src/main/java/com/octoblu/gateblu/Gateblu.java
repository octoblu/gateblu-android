package com.octoblu.gateblu;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.octoblu.gateblu.models.Device;
import com.octoblu.meshblukit.Meshblu;
import com.octoblu.sanejsonobject.SaneJSONObject;
import com.octoblu.gateblu.models.Device;

import org.json.JSONArray;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;

public class Gateblu extends Emitter {
    public static final String TAG = "Gateblu";
    public static final String CONFIG = "config";
    public static final String REGISTER = "register";
    public static final String WHOAMI = "whoami";
    public static final String READY = "ready";
    public static final String UPDATE_DEVICE = "update_device";
    public static final String GENERATED_TOKEN = "generated_token";
    public static final String GATEBLU_LOGGER_UUID = "4dd6d1a8-0d11-49aa-a9da-d2687e8f9caf";
    private final WebViewDeviceManager deviceManager;
    private final Context context;
    private WebView webView;
    private DeviceManagerServer server;
    private String uuid, token, owner, name;
    private Meshblu meshblu;
    private HashMap<String, String> deploymentUuids = new HashMap<String, String>();

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
            public void call(Object... args) {
                Log.d(TAG, "Device Manager Ready, going to gateblu device");
                Gateblu.this.emit(READY, args);
            }
        });
        this.deviceManager.on(WebViewDeviceManager.SEND_LOG, new Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Sending log for device");
                SaneJSONObject log = (SaneJSONObject) args[0];
                String workflow = log.getStringOrNull("workflow");
                String state = log.getStringOrNull("state");
                String message = log.getStringOrNull("message");
                String uuid = log.getStringOrNull("uuid");
                Device device = Gateblu.this.deviceManager.getDevice(uuid);
                if(device == null){
                    Log.d(TAG, "Gateblu device not found, not sending log message");
                    return;
                }
                Gateblu.this.sendLogMessage(workflow, state, device, message);
            }
        });

        this.setupMeshblu();
    }

    private void setupMeshblu(){
        SaneJSONObject meshbluConfig = new SaneJSONObject();
        meshbluConfig.putOrIgnore("uuid", uuid);
        meshbluConfig.putOrIgnore("token", token);
        meshbluConfig.putOrIgnore("server", "meshblu.octoblu.com");
        meshbluConfig.putIntOrIgnore("port", 443);
        if(this.meshblu != null){
            this.meshblu.setCredentials(meshbluConfig);
            return;
        }
        this.meshblu = new Meshblu(meshbluConfig, context);
        this.meshblu.on(Meshblu.REGISTER, new Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Gateblu Device is registered");
                Gateblu.this.emit(REGISTER, args);
            }
        });
        this.meshblu.on(Meshblu.WHOAMI, new Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Retrieved Gateblu Device");
                Gateblu.this.emit(WHOAMI, args);
            }
        });
        this.meshblu.on(Meshblu.UPDATE_DEVICE, new Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Updated Gateblu Device");
                Gateblu.this.emit(UPDATE_DEVICE, args);
            }
        });
        this.meshblu.on(Meshblu.GENERATED_TOKEN, new Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Generated token for Gateblu Device");
                Gateblu.this.emit(GENERATED_TOKEN, args);
            }
        });
        this.whoami();
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

    public String getName() { return name;
    }

    public void setName(String newName) { name = newName; }

    public void restart(){
        stop();
        start();
    }

    private void start() {
        if(!this.meshblu.isRegistered()) {
            register();
            return;
        }

        this.updateDefaults();

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
        SaneJSONObject data = new SaneJSONObject();
        data.putOrIgnore("type", "device:gateblu");
        data.putOrIgnore("platform", "android");
        this.meshblu.register(data);
    }

    private void updateDefaults() {
        SaneJSONObject data = new SaneJSONObject();
        data.putOrIgnore("platform", "android");
        // THIS WILL ERROR because of no response body but if you are cool, you'll ignore it like me.
        this.meshblu.updateDevice(this.uuid, data);
    }

    public void generateToken(String uuid) {
        this.meshblu.generateToken(uuid);
    }

    public void whoami() {
        this.meshblu.whoami();
    }

    public void sendLogMessage(String workflow, String state, Device device, String message) {
        if(deploymentUuids.containsKey(device.getUuid()) == false){
            UUID uuid = java.util.UUID.randomUUID();
            deploymentUuids.put(device.getUuid(), uuid.toString());
        }
        String deploymentUuid = deploymentUuids.get(device.getUuid());
        String versionName = BuildConfig.VERSION_NAME;
        SaneJSONObject payload = new SaneJSONObject();
        payload.putOrIgnore("application", "gateblu-android");
        payload.putOrIgnore("deploymentUuid", deploymentUuid);
        payload.putOrIgnore("gatebluUuid", this.uuid);
        payload.putOrIgnore("deviceUuid", device.getUuid());
        payload.putOrIgnore("connector", device.getConnector());
        payload.putOrIgnore("state", state);
        payload.putOrIgnore("workflow", workflow);
        payload.putOrIgnore("message", message);
        payload.putOrIgnore("platform", "android");
        payload.putOrIgnore("version", versionName);
        SaneJSONObject meshbluMessage = new SaneJSONObject();
        JSONArray devices = new JSONArray();
        devices.put(GATEBLU_LOGGER_UUID);
        devices.put(this.uuid);
        meshbluMessage.putArrayOrIgnore("devices", devices);
        meshbluMessage.putJSONOrIgnore("payload", payload);
        meshbluMessage.putOrIgnore("topic", "gateblu_log");
        this.meshblu.message(meshbluMessage);

        if(state.equals("error")){
            CharSequence text = String.format("[%s] %s", device.getRealName(), message);
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }
}