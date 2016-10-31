package de.uni_bremen.comnets.jd.energymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Start App Automatically after reboot
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "BroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, BackgroundService.class);
        context.startService(serviceIntent);
    }
}
