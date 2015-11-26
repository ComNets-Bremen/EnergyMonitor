package de.uni_bremen.comnets.jd.energymonitor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by user on 17.11.15.
 */
public final class EnergyMonitorDbHelper extends SQLiteOpenHelper{

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

    public EnergyMonitorDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void flushDb(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }
}
