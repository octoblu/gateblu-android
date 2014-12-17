package com.octoblu.gateblu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
        Device device = getItem(position);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.device_grid_item, parent, false);

        TextView textView = (TextView) view.findViewById(R.id.device_grid_item_name);
        textView.setText(device.getName());

        int imageID = context.getResources().getIdentifier(device.getImageName(), "drawable",  context.getPackageName());
        ImageView imageView = (ImageView) view.findViewById(R.id.device_grid_item_image);
        imageView.setImageResource(imageID);

        return view;
    }
}
