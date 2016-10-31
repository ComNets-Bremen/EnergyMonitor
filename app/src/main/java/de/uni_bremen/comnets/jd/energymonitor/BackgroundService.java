package de.uni_bremen.comnets.jd.energymonitor;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by jd on 23.10.16.
 */

public class BackgroundService extends Service {
    public static final String TAG = "BackgroundService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Start flag: " + flags);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(ConfigClass.getConfigListener());

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
        //TODO: We should stop the service using stopSelf() or stopService()?!
    }

    public void onDestroy() {
        // Unregister listener
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPref.unregisterOnSharedPreferenceChangeListener(ConfigClass.getConfigListener());
    }
}

