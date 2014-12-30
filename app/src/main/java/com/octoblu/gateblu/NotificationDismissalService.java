package com.octoblu.gateblu;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NotificationDismissalService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}
