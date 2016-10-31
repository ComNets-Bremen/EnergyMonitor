package de.uni_bremen.comnets.jd.energymonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
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
import java.io.IOException;
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

    private EnergyDbAdapter dbAdapter = null;

    // TODO: CHECK
    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    // TODO: End check

    BatteryReceiver batteryReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // DB Related stuff
        try {
            if (dbAdapter == null) {
                dbAdapter = new EnergyDbAdapter(this);
                dbAdapter.open();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dbAdapter = null;
            // Todo: Handle?
        }

        // List View start

        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableListView);

        listAdapter = new ExpandableListAdapter(this, dbAdapter);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        batteryReceiver = new BatteryReceiver(this, dbAdapter, listAdapter);

        // Enable measurements according to saved status
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean running = sharedPref.getBoolean(ConfigClass.CONFIG_VALUE_RUNNING, true);
        setMeasurementsEnable(running);

        // Start Background service
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        this.startService(serviceIntent);
    }

    /**
     * App is exited. Stopp all measurements, unregister everything
     */
    @Override
    protected void onDestroy() {
        setMeasurementsEnable(false);

        super.onDestroy();
    }

    /**
     * App is in the background. Remove unneeded (cached) data from the memory
     */
    @Override
    public void onStop() {
        dbAdapter.tidyup();
        super.onStop();
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
                    new ExportDb().execute();

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
    public void myRequestPermissions(String[] permissions, int requestCode) {
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
            new ExportDb().execute();
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
                dbAdapter.flushDb();
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
            if (batteryReceiver.getRegister()) {
                batteryReceiver.register(false);
                // Make settings persistent
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putBoolean(ConfigClass.CONFIG_VALUE_RUNNING, false);
                editor.apply();
                notifyUser(R.string.measurements_stopped);
            } else {
                notifyUser(R.string.measurements_already_stopped);
            }
        } else {
            if (!batteryReceiver.getRegister()) {
                batteryReceiver.register(true);
                // Make settings persistent
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putBoolean(ConfigClass.CONFIG_VALUE_RUNNING, true);
                editor.apply();
                notifyUser(R.string.measurements_started);
            } else {
                notifyUser(R.string.measurements_already_running);
            }
        }

        CheckBox checkBox = (CheckBox) findViewById(R.id.measurementStatusCheckBox);
        if (batteryReceiver.getRegister()) {
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


    public class ExportDb extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... voids) {
            String lastFilename = "";

            SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.output_file_format));

            if (!isExternalStorageWritable()) {
                Log.e(LOG_TAG, "Directory not accessible");
                return null;
            }


            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

            if (!(dir.exists() && dir.isDirectory())) {
                dir.mkdirs();
            }

            File output = new File(dir, "Energy-Table_" + sdf.format(new Date()) + ".csv");

            if (output == null) {
                Log.e(LOG_TAG, "Cannot create output file.");
                return null;
            }

            if (!output.exists()) {
                Log.e(LOG_TAG, "File does not exists, will try to create it: " + output.toString());
                try {
                    output.createNewFile();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot create new file!");
                    e.printStackTrace();
                }

            }


            if (dbAdapter.writeToFile(output) != 0) {
                return null;
            }

            return output.getAbsolutePath().toString();
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
