package com.octoblu.gateblu;

import android.os.ParcelUuid;

import java.util.UUID;

/**
 * Created by octoblu on 6/9/15.
 */
public class ShortableParcelUuid {
    public static ParcelUuid fromString(String uuid) {
        if(uuid == null){
            return ParcelUuid.fromString(uuid);
        }

        if(uuid.length() == 4){
            return ParcelUuid.fromString("0000" + uuid + "-0000-1000-8000-00805F9B34FB");
        }

        return ParcelUuid.fromString(uuid);
    }
}
