package de.uni_bremen.comnets.jd.energymonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
import java.util.Date;
import java.util.List;

/**
 * Created by user on 17.11.15.
 */
public final class EnergyDbAdapter {

    public static final String LOG_TAG = "EnergyDbAdapter";

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
    private static SQLiteDatabase mDb = null;

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


    public EnergyDbAdapter(Context context) {
        this.context = context;
    }

    public EnergyDbAdapter open() throws SQLException {
        if (mDb == null) {
            mDbHelper = new DatabaseHelper(context);
            mDb = mDbHelper.getWritableDatabase();
        }

        return this;
    }

    public void close() {
//        if (mDbHelper != null) {
//            mDbHelper.close();
 //       }
    }

    public void flushDb() {
        synchronized (mDb) {
            mDb.execSQL(SQL_DELETE_ENTRIES);
            mDbHelper.onCreate(mDb);
        }
    }

    public long appendData(ContentValues values) {
        long row;
        synchronized (mDb) {
            row = mDb.insert(TABLE_NAME, null, values);
        }
        return row;
    }

    private Cursor getGroupData() {
        Cursor mCursor;
        mCursor = mDb.query(TABLE_NAME, new String[]{
                COLUMN_NAME_ID,
                COLUMN_NAME_TIMESTAMP
        }, null, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

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

    public List<String> getChildDataById(int id) {
        List<String> items = new ArrayList<String>();

        synchronized (mDb) {
            Cursor mCursor = getDataById(id);

            for (int i = 0; i < mCursor.getColumnCount(); i++) {
                String item = mCursor.getColumnName(i) + ": " + mCursor.getString(i);
                items.add(item);
            }
        }
        return items;
    }

    public String getGroupHeadingById(int id) {
        String heading;

        synchronized (mDb) {
            Cursor mCursor = getDataById(id);
            long timestamp = mCursor.getLong(mCursor.getColumnIndex(COLUMN_NAME_TIMESTAMP));
            int columnId = mCursor.getInt(mCursor.getColumnIndex(COLUMN_NAME_ID));
            heading = String.valueOf(columnId) + ": " + DateFormat.getDateTimeInstance().format(new Date(timestamp));
        }
        return heading;
    }


    public int getSize() {
        int count;
        synchronized (mDb) {
            count = getGroupData().getCount();
        }
        return count;
    }

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

    public int writeToFile(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter fosw = new OutputStreamWriter(fos);

            synchronized (mDb) {

                Cursor cur = getAllData();

                String row = "";
                for (int j = 0; j < cur.getColumnCount(); j++) {
                    row += cur.getColumnName(j) + ",";
                }
                row += "\n";
                fosw.write(row);

                cur.moveToFirst();
                while (cur.isAfterLast() == false) {
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
            }
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
