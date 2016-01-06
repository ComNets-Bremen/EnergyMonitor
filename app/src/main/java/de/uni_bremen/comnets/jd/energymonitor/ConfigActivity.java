package de.uni_bremen.comnets.jd.energymonitor;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ConfigActivity extends AppCompatActivity {
    BroadcastReceiver batteryReceiver = null;
    boolean batteryReceiverRegistered = false;
    IntentFilter batteryReceiverFilter = null;

    EnergyMonitorDbHelper myEnergyDb = null;

    public static final String LOG_TAG = "ConfigActivity";
    public static final int SHOW_N_LAST_ROWS = 5;

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // List View start

        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableListView);

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // List View end

        if (myEnergyDb == null) {
            myEnergyDb = new EnergyMonitorDbHelper(getApplicationContext());
        }

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
                    values.put(EnergyMonitorDbHelper.COLUMN_NAME_CHARGING, isCharging);
                    values.put(EnergyMonitorDbHelper.COLUMN_NAME_TIMESTAMP, currentTime);
                    values.put(EnergyMonitorDbHelper.COLUMN_NAME_CHG_AC, acCharge);
                    values.put(EnergyMonitorDbHelper.COLUMN_NAME_CHG_USB, usbCharge);
                    values.put(EnergyMonitorDbHelper.COLUMN_NAME_PERCENTAGE, level);

                    // Toast.makeText(getApplicationContext(), "Values to be inserted into table: " + values.toString(), Toast.LENGTH_SHORT).show();

                    new InsertIntoDb().execute(values);

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
     * Export the database
     *
     * @param view
     */
    public void exportDb(View view) {
        new ExportDb().execute(EnergyMonitorDbHelper.TABLE_NAME);
    }

    /**
     * Remove everything from the database
     *
     * @param view
     */
    public void flushDb(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.flush_db_message)
                .setTitle(R.string.flush_db_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                myEnergyDb.flushDb();
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
     * @param text  The Text itself
     */
    public void notifyUser(String text) {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Wrapper for notifyUser: Accepts id from string resources
     * @param id    The string id
     */
    public void notifyUser(int id){
        notifyUser(getResources().getString(id));
    }


    /**
     * Async Task for exporting the db to a csv file
     */
    private class ExportDb extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... tables) {
            int count = tables.length;
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

                File output = new File(dir, Build.PRODUCT + "_" + tables[i] + "_" + sdf.format(new Date()) + ".csv");

                if (output == null) {
                    Log.e(LOG_TAG, "Cannot create output file.");
                    return null;
                }

                try {

                    if (!output.exists()) {
                        Log.e(LOG_TAG, "File does not exists: " + output.toString());
                        try {
                            output.createNewFile();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Cannot create new file!");
                            e.printStackTrace();
                        }

                    }

                    FileOutputStream fos = new FileOutputStream(output);
                    OutputStreamWriter fosw = new OutputStreamWriter(fos);

                    SQLiteDatabase db = myEnergyDb.getReadableDatabase();

                    Cursor res = db.rawQuery("select * from " + tables[0], null);
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
                    res.close();
                    db.close();
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
            //Toast.makeText(getApplicationContext(), "Insert done. Last file: " + file, Toast.LENGTH_SHORT).show();
            if (lastFile != null) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(lastFile)));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_csv_to)));
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_write_access), Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, "lastFile is null.");
            }
            return;
        }
    }


    /**
     * Async Task Insert Data into database
     */
    private class InsertIntoDb extends AsyncTask<ContentValues, Integer, Long> {
        protected Long doInBackground(ContentValues... values) {
            SQLiteDatabase db = myEnergyDb.getWritableDatabase();

            Long lastRowId = (long) -1;
            int count = values.length;
            for (int i = 0; i < count; i++) {

                lastRowId = db.insert(
                        EnergyMonitorDbHelper.TABLE_NAME,
                        null,
                        values[i]
                );
                if (isCancelled()) break;
            }
            db.close();
            myEnergyDb.close();


            return lastRowId;
        }

        protected void onPostExecute(Long rowId) {
            //Toast.makeText(getApplicationContext(), "Insert done. Last ID: " + rowId, Toast.LENGTH_SHORT).show();
            SQLiteDatabase db = myEnergyDb.getReadableDatabase();
            Cursor res = db.rawQuery("select * from " + EnergyMonitorDbHelper.TABLE_NAME + " where " + EnergyMonitorDbHelper.COLUMN_NAME_ID + "=" + rowId, null);
            res.moveToFirst();

            String title = "";
            List<String> items = new ArrayList<String>();

            for (int j = 0; j < res.getColumnCount(); j++) {
                String item = res.getColumnName(j) + ": " + res.getString(j);

                if (res.getColumnName(j).equals(EnergyMonitorDbHelper.COLUMN_NAME_TIMESTAMP)) {
                    Date d = new Date(res.getLong(j));
                    title = DateFormat.getDateTimeInstance().format(d);
                    item = item + " (" + title + ")";
                }
                items.add(item);
            }

            addNewDataToListView(title, items);

            res.close();
            db.close();
            myEnergyDb.close();
        }

    }


    /**
     * Check if the external storage is mounted and writable
     *
     * @return true if writable
     */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
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
}
