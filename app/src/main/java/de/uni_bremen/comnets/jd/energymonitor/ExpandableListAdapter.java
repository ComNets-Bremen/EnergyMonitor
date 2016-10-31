package de.uni_bremen.comnets.jd.energymonitor;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * @author Jens Dede, jd@comnets.uni-bremen.de
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
    // Log tag for error logging
    public static final String LOG_TAG = "ExpandableListAdapter";

    private Context context;            // The calling context
    private EnergyDbAdapter dbAdapter;  // Adapter to access the db with the measurements

    /**
     * Default constructor
     *
     * @param context       The calling context
     * @param dbAdapter     An adapter to access the db with the data
     */
    public ExpandableListAdapter(Context context, EnergyDbAdapter dbAdapter) {
        this.context = context;
        this.dbAdapter = dbAdapter;
        Log.e(LOG_TAG, "dbAdapter id: " + dbAdapter.toString());
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return dbAdapter.getChildDataById(groupPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String) getChild(groupPosition, childPosition).toString();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return dbAdapter.getCachedGroupHeadingById(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return (int) dbAdapter.getSize();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
