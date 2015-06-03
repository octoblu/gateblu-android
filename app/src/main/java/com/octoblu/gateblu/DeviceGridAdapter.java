package com.octoblu.gateblu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.octoblu.gateblu.models.Device;

import java.util.List;

public class DeviceGridAdapter extends BaseAdapter {
    private static final String TAG = "Gateblu:DeviceGridAdapter";
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

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String name = device.getName();
        viewHolder.name.setText(name);
        viewHolder.image.loadUrl(device.getLogo());
        viewHolder.image.setBackgroundColor(context.getResources().getColor(R.color.background_material_light));

        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    private static class ViewHolder {
        TextView name;
        WebView image;
    }
}
