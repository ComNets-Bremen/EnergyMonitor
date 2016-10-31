package de.uni_bremen.comnets.jd.energymonitor;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Created by jd on 27.10.16.
 */

public class BatteryReceiver {
    public final static String LOG_TAG = "BatteryReceiver";

    private BroadcastReceiver mBatteryReceiver = null;
    private EnergyDbAdapter mDbAdapter = null;
    private ExpandableListAdapter mExpandableListAdapter = null;
    private boolean mIsRegistered = false;
    Context mContext = null;


    BatteryReceiver(Context context, EnergyDbAdapter db, final ExpandableListAdapter expandableListAdapter){
        mDbAdapter = db;
        mExpandableListAdapter = expandableListAdapter;
        mContext = context;

        mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {


                long currentTime = System.currentTimeMillis();

                int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scaling = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int level = -1;
                if (currentLevel >= 0 && scaling > 0) {
                    level = (currentLevel * 100) / scaling;
                }
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

                int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;


                ContentValues values = new ContentValues();
                values.put(EnergyDbAdapter.COLUMN_NAME_CHARGING, isCharging);
                values.put(EnergyDbAdapter.COLUMN_NAME_TIMESTAMP, currentTime);
                values.put(EnergyDbAdapter.COLUMN_NAME_CHG_AC, acCharge);
                values.put(EnergyDbAdapter.COLUMN_NAME_CHG_USB, usbCharge);
                values.put(EnergyDbAdapter.COLUMN_NAME_PERCENTAGE, level);

                if (mDbAdapter.appendData(values) == -1){
                    Log.e(LOG_TAG, "Cannot insert data into database!");
                }
                //mExpandableListAdapter.notifyDataSetChanged();

            }
        };

    }

    public BroadcastReceiver getBroadcastReceiver(){
        return mBatteryReceiver;
    }

    public IntentFilter getIntentFilter(){
        return new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    }

    public boolean register(boolean reg){
        if (!mIsRegistered && reg){
            mContext.registerReceiver(this.getBroadcastReceiver(), this.getIntentFilter());
            mIsRegistered = true;
        } else if (mIsRegistered && !reg){
            mContext.unregisterReceiver(mBatteryReceiver);
            mIsRegistered = false;
        }
        return mIsRegistered;
    }

    public boolean getRegister(){
        return mIsRegistered;
    }
}
