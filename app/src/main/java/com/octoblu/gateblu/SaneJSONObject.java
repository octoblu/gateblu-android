package com.octoblu.gateblu;

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.MissingResourceException;

public class SaneJSONObject extends JSONObject {
    public SaneJSONObject() {
        super();
    }

    public SaneJSONObject(String jsonData) throws JSONException {
        super(jsonData);
    }

    public String getStringOrNull(@Nullable String name) {
        try {
            return this.getString(name);
        } catch (JSONException e) {
            return null;
        }
    }

    public String getStringOrThrow(@Nullable String name) {
        String value = getStringOrNull(name);
        if (value == null) {
            throw new MissingResourceException("Missing " + name, "WebViewDevice", name);
        }
        return value;
    }

    public static SaneJSONObject fromJSONObject(JSONObject data) {
        return fromString(data.toString());
    }

    public static SaneJSONObject fromString(String data) {
        try {
            return new SaneJSONObject(data);
        } catch (JSONException e) {
            return new SaneJSONObject();
        }
    }
}
