package de.uni_bremen.comnets.jd.energymonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.database.DatabaseUtils.queryNumEntries;

/**
 * Adapter to use the SQLite DB for storing the energy measurements
 *
 * @author Jens Dede, jd@comnets.uni-bremen.de
 */
public final class EnergyDbAdapter {

    // Tag for logging
    public static final String LOG_TAG = "EnergyDbAdapter";

    // General db related names and versions
    public static final String DATABASE_NAME = "energy.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "batterystatus";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_PERCENTAGE = "percentage";
    public static final String COLUMN_NAME_CHARGING = "is_charging";
    public static final String COLUMN_NAME_CHG_USB = "chg_usb";
    public static final String COLUMN_NAME_CHG_AC = "chg_ac";

    // db datatypes
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String NUMERIC_TYPE = " NUMERIC";
    private static final String COMMA_SEP = ",";

    // Command to create the db
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_TIMESTAMP + NUMERIC_TYPE + COMMA_SEP +
                    COLUMN_NAME_PERCENTAGE + INTEGER_TYPE + COMMA_SEP +
                    COLUMN_NAME_CHARGING + INTEGER_TYPE + COMMA_SEP +
                    COLUMN_NAME_CHG_USB + INTEGER_TYPE + COMMA_SEP +
                    COLUMN_NAME_CHG_AC + INTEGER_TYPE +
                    " )";

    // Cached headings to reduce database traffic
    private Map<Integer, String> cachedHeadings = null;

    // Remove everything from the db
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private final Context context;
    private static DatabaseHelper mDbHelper;   // Helper object to handle db
    private static SQLiteDatabase mDb = null;  // The (writable) database

    /**
     * Database helper with some predefined transition methods
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }


        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    /**
     * Standard constructor
     *
     * @param context Calling context
     */
    public EnergyDbAdapter(Context context) {
        this.context = context;
        this.cachedHeadings = Collections.synchronizedMap(new HashMap<Integer, String>());
    }

    /**
     * Open the writable database
     *
     * @return The DB Adapter object
     * @throws SQLException If db cannot be opened
     */
    public synchronized EnergyDbAdapter open() throws SQLException {
        if (mDb == null) {
            mDbHelper = new DatabaseHelper(context);
            mDb = mDbHelper.getWritableDatabase();
        }

        return this;
    }

    /**
     * Close the database
     */
    public synchronized void close() {
        if (mDb != null) {
            mDb.close();
        }
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }

    /**
     * Remove everything from the database and create a new, empty one
     */
    public synchronized void flushDb() {
        mDb.execSQL(SQL_DELETE_ENTRIES);
        mDbHelper.onCreate(mDb);
    }

    /**
     * Add new data to the database
     *
     * @param values pairs of ContentValues
     * @return RowId of the inserted data
     */
    public synchronized long appendData(ContentValues values) {
        return mDb.insert(TABLE_NAME, null, values);
    }


    /**
     * Get a cursor pointing to data with a given id
     *
     * @param id id of the data to get
     * @return Cursor
     */
    private Cursor getDataById(int id) {
        Cursor mCursor;
        mCursor = mDb.query(TABLE_NAME, new String[]{
                COLUMN_NAME_ID,
                COLUMN_NAME_TIMESTAMP,
                COLUMN_NAME_PERCENTAGE,
                COLUMN_NAME_CHARGING,
                COLUMN_NAME_CHG_USB,
                COLUMN_NAME_CHG_AC
        }, COLUMN_NAME_ID + "=" + String.valueOf(id + 1), null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Returns a string list of the child data for a given id
     *
     * @param id id to get the child data for
     * @return String list of the data
     */
    public synchronized List<String> getChildDataById(int id) {
        List<String> items = new ArrayList<String>();

        Cursor mCursor = getDataById(id);
        for (int i = 0; i < mCursor.getColumnCount(); i++) {
            String item = mCursor.getColumnName(i) + ": " + mCursor.getString(i);
            items.add(item);
        }
        return items;
    }

    /**
     * Get the heading for a group in an expandable list view
     *
     * @param id id of group to get
     * @return String for the heading
     */
    public synchronized String getGroupHeadingById(int id) {
        String heading;
        Cursor mCursor = getDataById(id);
        long timestamp = mCursor.getLong(mCursor.getColumnIndex(COLUMN_NAME_TIMESTAMP));
        int columnId = mCursor.getInt(mCursor.getColumnIndex(COLUMN_NAME_ID));
        int percentage = mCursor.getInt(mCursor.getColumnIndex(COLUMN_NAME_PERCENTAGE));
        heading = String.valueOf(columnId) + ": " + DateFormat.getDateTimeInstance().format(new Date(timestamp)) + " (" + String.valueOf(percentage) + "%)" ;
        return heading;
    }

    /**
     * Cached Version of getGroupHeadingById to reduce db traffic
     * @param id    id of group to get
     * @return      String of the heading
     */
    public synchronized  String getCachedGroupHeadingById(int id) {
        if (!cachedHeadings.containsKey(id)){
            cachedHeadings.put(id, getGroupHeadingById(id));
        }
        return cachedHeadings.get(id);
    }


    /**
     * Get total number of datasets in the database
     *
     * @return number of entries
     */
    public synchronized long getSize() {
        return queryNumEntries(mDb, TABLE_NAME);
    }

    /**
     * Return a cursor to get all data in db. Used for example for the export
     *
     * @return Cursor to all data in db
     */
    private Cursor getAllData() {
        Cursor mCursor = mDb.query(TABLE_NAME, new String[]{
                        COLUMN_NAME_ID,
                        COLUMN_NAME_TIMESTAMP,
                        COLUMN_NAME_PERCENTAGE,
                        COLUMN_NAME_CHARGING,
                        COLUMN_NAME_CHG_USB,
                        COLUMN_NAME_CHG_AC
                },
                null, null, null, null, null, null
        );
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Remove all entries from the cache. Maybe useful when app is in the background to save some RAM
     */
    public void tidyup(){
        cachedHeadings.clear();
    }


    /**
     * Write complete db to a file
     *
     * @param file File object where to write the data to
     * @return 0 if successful
     * -1 otherwise
     */
    public synchronized int writeToFile(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter fosw = new OutputStreamWriter(fos);

            Cursor cur = getAllData();

            String row = "";
            for (int j = 0; j < cur.getColumnCount(); j++) {
                row += cur.getColumnName(j) + ",";
            }
            row += "\n";
            fosw.write(row);

            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                row = "";
                for (int j = 0; j < cur.getColumnCount(); j++) {
                    row += cur.getString(j) + ",";
                }
                row += "\n";
                fosw.write(row);
                cur.moveToNext();
            }

            fosw.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }


}
