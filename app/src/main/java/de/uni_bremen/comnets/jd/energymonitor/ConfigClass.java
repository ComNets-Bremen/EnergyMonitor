package de.uni_bremen.comnets.jd.energymonitor;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * Handels the configurations and deals with the corresponding listeners etc.
 */

public class ConfigClass {
    public static final String TAG = "ConfigClass";
    public static final String CONFIG_VALUE_RUNNING = "de.uni_bremen.comnets.jd.energymonitor.isRunning";

    /*
    Service listener. Act when the preferences were changed
     */

    private static SharedPreferences.OnSharedPreferenceChangeListener  listener;

    ConfigClass(){
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener getConfigListener()
    {
        return new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                switch(key){
                    case CONFIG_VALUE_RUNNING:
                        // TODO: Write handle
                        if (prefs.getBoolean(key, true)){
                            Log.d(TAG, "Should now be running");
                        } else {
                            Log.d(TAG, "Should now be stopped");
                        }
                        break;

                    default:
                        Log.d(TAG, "Unhandled key: " +  key);
                }
            }
        };
    }

}
