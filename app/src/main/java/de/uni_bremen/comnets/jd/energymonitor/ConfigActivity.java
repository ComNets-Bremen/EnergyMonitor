package de.uni_bremen.comnets.jd.energymonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.Manifest;

public class ConfigActivity extends Activity {
    public static final String LOG_TAG = "ConfigActivity";

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private BroadcastReceiver batteryReceiver = null;
    private boolean batteryReceiverRegistered = false;
    private IntentFilter batteryReceiverFilter = null;

    private EnergyDbAdaper dbHelper;
    private SimpleCursorAdapter dataAdapter;

// TODO: CHECK
    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
// TODO: End check

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // DB Related stuff
        try{
            dbHelper = new EnergyDbAdaper(this);
            dbHelper.open();
        } catch (SQLException e){
            e.printStackTrace();
            dbHelper = null;
            // Todo: Handle?
        }

        // List View start

        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableListView);

        listAdapter = new ExpandableListAdapter(this, dbHelper);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // List View end


        if (batteryReceiver == null) {
            batteryReceiver = new BroadcastReceiver() {
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
                    values.put(EnergyDbAdaper.COLUMN_NAME_CHARGING, isCharging);
                    values.put(EnergyDbAdaper.COLUMN_NAME_TIMESTAMP, currentTime);
                    values.put(EnergyDbAdaper.COLUMN_NAME_CHG_AC, acCharge);
                    values.put(EnergyDbAdaper.COLUMN_NAME_CHG_USB, usbCharge);
                    values.put(EnergyDbAdaper.COLUMN_NAME_PERCENTAGE, level);

                    dbHelper.appendData(values);
                    listAdapter.notifyDataSetChanged();

                }
            };

            if (batteryReceiverFilter == null) {
                batteryReceiverFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            }
        }

        // Enable measurements on startup
        setMeasurementsEnable(true);
    }

    @Override
    protected void onDestroy() {
        setMeasurementsEnable(false);

        super.onDestroy();
    }

    /**
     * Executed when a certain permission is requested
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new ExportDb().execute(dbHelper.getAllData());

                } else {
                    notifyUser(R.string.no_write_access);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Used to request permissions out of an onClick handler
     *
     * @param permissions
     * @param requestCode
     */
    public void myRequestPermissions(String[] permissions, int requestCode){
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }


    /**
     * Export the database
     *
     * @param view
     */
    public void exportDbClickHandler(View view) {
    Log.e(LOG_TAG, "export");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.e(LOG_TAG, "request");

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.write_access_message)
                        .setTitle(R.string.write_access_title);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        myRequestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                Log.e(LOG_TAG, "requested");
                myRequestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            new ExportDb().execute(dbHelper.getAllData());
        }
    }

    /**
     * Remove everything from the database
     *
     * @param view
     */
    public void flushDbClickHandler(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.flush_db_message)
                .setTitle(R.string.flush_db_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.flushDb();
                listAdapter.notifyDataSetChanged();
                notifyUser(R.string.db_flushed);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                notifyUser(R.string.aborted);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void toggleCheckBox(View view) {
        CheckBox checkBox = (CheckBox) findViewById(R.id.measurementStatusCheckBox);
        setMeasurementsEnable(checkBox.isChecked());
    }

    /**
     * Enable / disable measurements
     *
     * @param measurementsEnable
     */
    private void setMeasurementsEnable(boolean measurementsEnable) {
        if (!measurementsEnable) {
            if (batteryReceiverRegistered) {
                unregisterReceiver(batteryReceiver);
                batteryReceiverRegistered = false;
                notifyUser(R.string.measurements_stopped);
            } else {
                notifyUser(R.string.measurements_already_stopped);
            }
        } else {
            if (!batteryReceiverRegistered) {
                registerReceiver(batteryReceiver, batteryReceiverFilter);
                batteryReceiverRegistered = true;
                notifyUser(R.string.measurements_started);
            } else {
                notifyUser(R.string.measurements_already_running);
            }
        }

        CheckBox checkBox = (CheckBox) findViewById(R.id.measurementStatusCheckBox);
        if (batteryReceiverRegistered) {
            checkBox.setText(R.string.measurement_status_running);
            checkBox.setChecked(true);
        } else {
            checkBox.setText(R.string.measurement_status_stopped);
            checkBox.setChecked(false);
        }
    }


    /**
     * Debug output: Output given text in text view and as a toast
     *
     * @param text The Text itself
     */
    public void notifyUser(String text) {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Wrapper for notifyUser: Accepts id from string resources
     *
     * @param id The string id
     */
    public void notifyUser(int id) {
        notifyUser(getResources().getString(id));
    }

    /**
     * Add a new item to the expandable list view
     *
     * @param title Title of new item
     * @param items The corresponding List including all items
     */
    private void addNewDataToListView(String title, List<String> items) {
        listDataHeader.add(title);
        listDataChild.put(title, items);
        listAdapter.notifyDataSetChanged();
    }






    public class ExportDb extends AsyncTask<Cursor, Void, String> {

        protected String doInBackground(Cursor... cursors) {
            int count = cursors.length;
            String lastFilename = "";

            SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.output_file_format));

            if (!isExternalStorageWritable()) {
                Log.e(LOG_TAG, "Directory not accessible");
                return null;
            }
            for (int i = 0; i < count; i++) {

                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

                if (!(dir.exists() && dir.isDirectory())) {
                    dir.mkdirs();
                }

                File output = new File(dir, "Energy-Table_" + sdf.format(new Date()) + ".csv");

                if (output == null) {
                    Log.e(LOG_TAG, "Cannot create output file.");
                    return null;
                }

                try {

                    if (!output.exists()) {
                        Log.e(LOG_TAG, "File does not exists, will try to create it: " + output.toString());
                        try {
                            output.createNewFile();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Cannot create new file!");
                            e.printStackTrace();
                        }

                    }

                    FileOutputStream fos = new FileOutputStream(output);
                    OutputStreamWriter fosw = new OutputStreamWriter(fos);

                    Cursor res = cursors[i];
                    String row = "";
                    for (int j = 0; j < res.getColumnCount(); j++) {
                        row += res.getColumnName(j) + ",";
                    }
                    row += "\n";
                    fosw.write(row);

                    res.moveToFirst();
                    while (res.isAfterLast() == false) {
                        row = "";
                        for (int j = 0; j < res.getColumnCount(); j++) {
                            row += res.getString(j) + ",";
                        }
                        row += "\n";
                        fosw.write(row);
                        res.moveToNext();
                    }
                    fosw.close();
                    fos.close();

                    lastFilename = output.getAbsolutePath().toString();

                } catch (IOException e) {
                    Log.e(LOG_TAG, "IOException!");
                    e.printStackTrace();
                    return null;
                }
            }
            return lastFilename;
        }

        protected void onPostExecute(String lastFile) {
            if (lastFile != null) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(lastFile)));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_csv_to)));
            } else {
                notifyUser(R.string.no_write_access);
                Log.e(LOG_TAG, "lastFile is null.");
            }
            return;
        }

        /**
         * Check if the external storage is mounted and writable
         *
         * @return true if writable
         */
        private boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }

    }

}
