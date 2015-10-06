package com.octoblu.gateblu;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.octoblu.gateblu.models.Device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class DeviceGridAdapter extends BaseAdapter {
    private static final String TAG = "DeviceGridAdapter";
    private final Context context;
    private final List<Device> devices;

    public DeviceGridAdapter(Context context, List<Device> devices) {
        this.context = context;
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Device getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return devices.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Device device = getItem(position);

        final ViewHolder viewHolder;

        if(convertView == null){
            viewHolder = new ViewHolder();

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.device_grid_item, parent, false);
            viewHolder.name  = (TextView) convertView.findViewById(R.id.device_grid_item_name);
            viewHolder.image = (WebView) convertView.findViewById(R.id.device_grid_item_image_webview);
            viewHolder.image.setBackgroundColor(context.getResources().getColor(R.color.background_material_light));

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.name.setText(device.getName());
        String logoUrl = device.getLogo();
        if(logoUrl == null){
            logoUrl = "https://ds78apnml6was.cloudfront.net/device/generic.svg";
        }
        viewHolder.image.loadData(deviceImageHTML(logoUrl), "text/html", "utf8");
        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public String deviceImageHTML(String logoUrl) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open("www/device-image.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString().replace("{{logoUrl}}", logoUrl);
        } catch (IOException e) {
            Log.e(TAG, "Could not read device-image.html: " + e.getMessage(), e);
            return "";
        }
    }

    private static class ViewHolder {
        TextView name;
        WebView image;
    }
}
