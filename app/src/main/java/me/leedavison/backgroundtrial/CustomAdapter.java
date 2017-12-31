package me.leedavison.backgroundtrial;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.graphics.ColorUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Leeps on 31/12/2017.
 */

public class CustomAdapter extends ArrayAdapter<DayListModel> implements View.OnClickListener {

    Context mContext;
    private ArrayList<DayListModel> dataSet;
    private int lastPosition = -1;

    public CustomAdapter(ArrayList<DayListModel> data, Context context) {
        super(context, R.layout.history_row_item, data);
        this.dataSet = data;
        this.mContext = context;

    }

    @Override
    public void onClick(View v) {

        int position = (Integer) v.getTag();
        Object object = getItem(position);
        DayListModel DayListModel = (DayListModel) object;


    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        DayListModel DayListModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.history_row_item, parent, false);
            viewHolder.txtDate = (TextView) convertView.findViewById(R.id.date);
            viewHolder.txtExposure = (TextView) convertView.findViewById(R.id.exposure_number);
            viewHolder.exposureBar = (ProgressBar) convertView.findViewById(R.id.history_progressBar);

            result = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        result.startAnimation(animation);
        lastPosition = position;

        viewHolder.txtDate.setText(String.valueOf(DayListModel.getDay()));
        viewHolder.exposureBar.setProgress((int) Math.round(DayListModel.getExposure()));
        viewHolder.txtExposure.setText(String.valueOf(Math.round(DayListModel.getExposure())) + "%");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (DayListModel.getExposure() < 100) {
                viewHolder.exposureBar.setProgressTintList(ColorStateList.valueOf(ColorUtils.blendARGB(Color.argb(255, 0, 255, 0), Color.argb(255, 255, 0, 0), (float) (DayListModel.getExposure() / 100))));
            } else {
                viewHolder.exposureBar.setProgressTintList(ColorStateList.valueOf(Color.argb(255, 255, 0, 0)));
            }
//            viewHolder.exposureBar.setProgressTintMode();
        }
        // Return the completed view to render on screen
        return convertView;
    }

    // View lookup cache
    private static class ViewHolder {
        TextView txtDate;
        TextView txtExposure;
        ProgressBar exposureBar;
    }
}