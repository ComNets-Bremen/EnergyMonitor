package de.uni_bremen.comnets.jd.energymonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by user on 17.11.15.
 */
public final class EnergyDbAdaper {

    public static final String LOG_TAG ="EnergyDbAdapter";

    public static final String DATABASE_NAME = "energy.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "batterystatus";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_PERCENTAGE = "percentage";
    public static final String COLUMN_NAME_CHARGING = "is_charging";
    public static final String COLUMN_NAME_CHG_USB = "chg_usb";
    public static final String COLUMN_NAME_CHG_AC = "chg_ac";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String NUMERIC_TYPE = " NUMERIC";

    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_TIMESTAMP + NUMERIC_TYPE + COMMA_SEP +
                    COLUMN_NAME_PERCENTAGE + INTEGER_TYPE + COMMA_SEP +
                    COLUMN_NAME_CHARGING + INTEGER_TYPE + COMMA_SEP +
                    COLUMN_NAME_CHG_USB + INTEGER_TYPE + COMMA_SEP +
                    COLUMN_NAME_CHG_AC + INTEGER_TYPE +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private final Context context;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

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


    public EnergyDbAdaper(Context context){
        this.context = context;
    }

    public EnergyDbAdaper open() throws SQLException {
        mDbHelper = new DatabaseHelper(context);
            mDb = mDbHelper.getWritableDatabase();

        return this;
    }

    public void close(){
        if (mDbHelper != null){
            mDbHelper.close();
        }
    }

    public void flushDb() {
        synchronized (mDb) {
            mDb.execSQL(SQL_DELETE_ENTRIES);
            mDbHelper.onCreate(mDb);
        }
    }

    public long appendData(ContentValues values){
        long res;
        synchronized (mDb){
            res = mDb.insert(TABLE_NAME, null, values);
        }
        return res;
    }

    public Cursor getGroupData(){
        Cursor mCursor;
        synchronized (mDb) {
            mCursor = mDb.query(TABLE_NAME, new String[]{
                    COLUMN_NAME_ID,
                    COLUMN_NAME_TIMESTAMP
            }, null, null, null, null, null, null);
            if (mCursor != null) {
                mCursor.moveToFirst();
            }
        }
        return mCursor;
    }

    public Cursor getDataById(int id){
        Cursor mCursor;
        synchronized (mDb) {
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
        }
        return mCursor;
    }

    public List<String> getChildDataById(int id){
        Cursor mCursor = getDataById(id);

        List<String> items = new ArrayList<String>();

        for (int i = 0; i < mCursor.getColumnCount(); i++) {
            String item = mCursor.getColumnName(i) + ": " + mCursor.getString(i);
            items.add(item);
        }
        return items;
    }

    public String getGroupHeadingById(int id){
        Cursor mCursor = getDataById(id);
        long timestamp = mCursor.getLong(mCursor.getColumnIndex(COLUMN_NAME_TIMESTAMP));
        int columnId = mCursor.getInt(mCursor.getColumnIndex(COLUMN_NAME_ID));
        return String.valueOf(columnId) + ": " + DateFormat.getDateTimeInstance().format(new Date(timestamp));
    }



    public int getSize(){
        return getGroupData().getCount();
    }

    public Cursor getAllData() {
        Cursor mCursor;
        synchronized (mDb) {
            mCursor = mDb.query(TABLE_NAME, new String[]{
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
        }
        return mCursor;
    }
}
