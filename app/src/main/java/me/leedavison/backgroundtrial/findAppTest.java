package me.leedavison.backgroundtrial;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static me.leedavison.backgroundtrial.MainActivity.getContext;

public class findAppTest extends AppCompatActivity {

    TextView textView;

    String topPackageName = "none";
    String[] perms = {"android.permission.PACKAGE_USAGE_STATS"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_app_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textView = (TextView) findViewById(R.id.appNameText);


        if (Build.VERSION.SDK_INT >= 23) {
            int res = getContext().checkCallingOrSelfPermission(perms[0]);
            Log.i("permission state: ", String.valueOf(res));
            if (res == -1) {
                requestPermissions(perms, 200);
            } else if (res == -1) {
                Toast.makeText(this, "Permission no good m8", Toast.LENGTH_SHORT).show();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long currentTime = System.currentTimeMillis();
            // get usage stats for the last 10 seconds
            List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 10, currentTime);
            // search for app with most recent last used time
            if (stats != null) {
                long lastUsedAppTime = 0;
                for (UsageStats usageStats : stats) {
                    if (usageStats.getLastTimeUsed() > lastUsedAppTime) {
                        topPackageName = usageStats.getPackageName();
                        lastUsedAppTime = usageStats.getLastTimeUsed();
                        textView.setText(topPackageName);
                    }
                }
            }
        }
        textView.setText(topPackageName);

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            UsageStatsManager mUsageStatsManager = (UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);
//            long time = System.currentTimeMillis();
//            // We get usage stats for the last 10 seconds
//            List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000*100, time);
//            // Sort the stats by the last time used
//            if(stats != null) {
//                SortedMap<Long,UsageStats> mySortedMap = new TreeMap<Long,UsageStats>();
//                for (UsageStats usageStats : stats) {
//                    mySortedMap.put(usageStats.getLastTimeUsed(),usageStats);
//                }
//                if(!mySortedMap.isEmpty()) {
//                    topPackageName =  mySortedMap.get(mySortedMap.lastKey()).getPackageName();
////                    mySortedMap.get(2);
//                }
//            }
//        }


    }

}
