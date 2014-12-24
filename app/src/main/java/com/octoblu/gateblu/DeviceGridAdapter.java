package com.octoblu.gateblu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
            viewHolder.image = (ImageView) convertView.findViewById(R.id.device_grid_item_image);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }


        int imageID = context.getResources().getIdentifier(device.getImageName(), "drawable",  context.getPackageName());
        if(imageID == 0){
            imageID = R.drawable.generic;
        }

        viewHolder.name.setText(device.getName());
        viewHolder.image.setImageResource(imageID);
        viewHolder.image.setAlpha(device.isOnline() ? 1.0f : 0.5f);

        device.setOnOnlineChangedListener(new Device.OnlineChangedListener(){
            @Override
            public void onOnlineChanged(){
                viewHolder.image.setAlpha(device.isOnline() ? 1.0f : 0.5f);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView name;
        ImageView image;
    }
}
