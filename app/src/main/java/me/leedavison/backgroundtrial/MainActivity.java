package me.leedavison.backgroundtrial;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.internal.NavigationMenu;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;
import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.EdgeDetail;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.charts.SeriesLabel;
import com.hookedonplay.decoviewlib.events.DecoEvent;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;

public class MainActivity extends AppCompatActivity {

    private static final String DATA_KEY_NAME = "name";
    private static final String DATA_KEY_EMAIL = "email";
    private static final String DATA_KEY_AGE = "age";
    private static final String DATA_KEY_SCHOOL = "school";
    public static boolean isService = false;
    public static Context appContext;
    public static boolean trackMusic = false;
    private static CustomAdapter adapter;
    final CellProcessor[] listeningLogProcessor = new CellProcessor[]{
            new Optional(),
            new ParseDate("dd/MM/yyyy hh:mm:ss"),
            new ParseInt(),
            new ParseDouble()
    };
    boolean firstBoot;
    String feedbackText = "No feedback!";
    String userName;
    String userEmail;
    String userAge;
    String userSchool;
    TextView welcomeText;
    String userShortName[];
    // Set up the reader, and the processors
    String LISTENING_LOG_CSV_HEADER[] = {"Event", "Time", "Volume Level", "dB Equivalent"};
    HashMap<String, Object> listeningLogEntry[];
    // a Map to output from the csv
    Map<String, Object> exposureMap;
    //SimpleDateFormat for making day-date comparisons by string
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    //SimpleDateFormat for creating attractive human readable date in the history listview
    SimpleDateFormat attractiveDate = new SimpleDateFormat("EEE dd/MM/yyyy");
    // Some variables for comparing date and audio levels
    Date csvDate;
    Date previousCsvDate;
    Double audioLevel = 0.0;
    Date audioTime;
    Double todaysExposure = 0.0;
    ArrayList<Double> exposurePoints = new ArrayList<>();
    ArrayList<DayListModel> historyDayList = new ArrayList<>();
    ListView historyListView;
    ICsvMapReader mapReader = null;


    FabSpeedDial fabSpeedDial;

    Intent serviceIntent;

    public static Context getContext() {
        return appContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();

        fabSpeedDial = (FabSpeedDial) findViewById(R.id.fab_speed_dial);
        setupFAB();


        serviceIntent = new Intent(MainActivity.this, BackgroundService.class);

        SharedPreferences prefs = this.getSharedPreferences(
                "me.leedavison.backgroundtrial", Context.MODE_PRIVATE);


        if (!userDetailsExist()) {
            trackMusic = false;
            firstBoot = true;
            Intent myIntent = new Intent(MainActivity.this, questionnaire.class);
            MainActivity.this.startActivity(myIntent);
        } else {
            trackMusic = true;
            firstBoot = false;
        }

        userName = prefs.getString(DATA_KEY_NAME, "welcome to my music tracker!");
        userEmail = prefs.getString(DATA_KEY_EMAIL, null);
        userAge = prefs.getString(DATA_KEY_AGE, null);
        userSchool = prefs.getString(DATA_KEY_SCHOOL, null);
        welcomeText = (TextView) findViewById(R.id.welcome_message);

        userShortName = userName != null ? userName.split(" ") : new String[0];
        if (welcomeText != null) {
            welcomeText.setText("Hi, " + userShortName[0] + "!");
        }

        welcomeText.setText("Lol rehgsikdekjfbnskdjgfnb");

//
//        ImageButton startserviceButton = (ImageButton) findViewById(R.id.Button1);
//        ImageButton stopserviceButton = (ImageButton) findViewById(R.id.Button2);
//
//
//        if (startserviceButton != null) {
//            startserviceButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    startService(serviceIntent);
//
//                    Intent startMain = new Intent(Intent.ACTION_MAIN);
//                    startMain.addCategory(Intent.CATEGORY_HOME);
//                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(startMain);
//                    isService = true;
//                }
//            });
//        }
//
//        if (stopserviceButton != null) {
//            stopserviceButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    trackMusic = false;
//                    Log.i("Trackmusic set: ", "False");
//
//                    Intent startMain = new Intent(Intent.ACTION_MAIN);
//                    startMain.addCategory(Intent.CATEGORY_HOME);
//                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(startMain);
//
//                }
//            });
//        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.headphonesicon2);


        historyListView = (ListView) findViewById(R.id.history_view);

        readCSV();

        sumPoints();
        Log.i(csvDate.toString() + " exposure: ", String.valueOf(todaysExposure));

        // Check if date is today - need to add this somewhere else!
        if (sdf.format(csvDate).equals(sdf.format(new Date()))) {
            createTodayExposureView();
            Log.i("Today's exposure is: ", String.valueOf(todaysExposure));
        }

        createBarList();

//        arcView.addEvent(new DecoEvent.Builder(10)
//                .setIndex(series1Index)
//                .setDelay(6000)
//                .setColor(Color.parseColor("#FFFF0000"))
//                .build());


//        arcView.addEvent(new DecoEvent.Builder(25).setIndex(series1Index).setDelay(4000).build());
//        arcView.addEvent(new DecoEvent.Builder(10).setIndex(series1Index).setDelay(12000).build());

    }

    private void readCSV() {

        ContextWrapper c = new ContextWrapper(getApplicationContext());

        try {
            mapReader = new CsvMapReader(new FileReader(c.getFilesDir() + "/Headphone_Log.csv"), CsvPreference.STANDARD_PREFERENCE);

            String header[] = mapReader.getHeader(true);


            // Read out the csv
            while ((exposureMap = mapReader.read(header, listeningLogProcessor)) != null) {

                //Print to log all of the output of the csv for debugging
                Log.i("CSV Output: ", String.format("lineNo=%s, rowNo=%s, ListeningLog=%s", mapReader.getLineNumber(),
                        mapReader.getRowNumber(), exposureMap));

                // Get the date from the csv for this row
                csvDate = (Date) exposureMap.get("Time");

                // Check to make sure there was a previous csv date that was parsed
                if (previousCsvDate != null) {

                    // If the date is the same as the previous date, parse the events and add up the exposure
                    if (sdf.format(previousCsvDate).equals(sdf.format(csvDate))) {
                        parseEvents();
                    } else {
                        // If the date is different, sum the points, then log them out for now. Then set previous date to the new csv date, and restart the process of adding up exposure
                        sumPoints();

                        Log.i(previousCsvDate.toString() + " exposure: ", String.valueOf(todaysExposure));
                        previousCsvDate = csvDate;
                        parseEvents();
                    }
                } else {
                    parseEvents();
                    previousCsvDate = csvDate;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mapReader != null) {
                try {
                    mapReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createBarList() {

        //Reverse the list so that the dates are soonest-first
        Collections.reverse(historyDayList);

        //Remove today form, the history list
        historyDayList.remove(0);

        if (historyDayList.size() < 1) {
            historyDayList.add(new DayListModel("No history yet! come back tomorrow.", 0.0));
        }

        //Apply the history list to the adaptor to populate the listview
        adapter = new CustomAdapter(historyDayList, getApplicationContext());

        historyListView.setAdapter(adapter);
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DayListModel dayListModel = historyDayList.get(position);

                Snackbar.make(view, "You listened for " + String.valueOf(Math.round(dayListModel.getExposure())) + "% of your  daily dose that day", Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
            }
        });

    }

    public void parseEvents() {

        if (exposurePoints == null) {
            exposurePoints = new ArrayList<>();
        }

        //This block runs through the events for that date, and generates an exposure level
        if (exposureMap.get("Event").equals("Audio started")) {

            audioTime = (Date) exposureMap.get("Time");
            audioLevel = (Double) exposureMap.get("dB Equivalent");

        } else if (exposureMap.get("Event").equals("Volume changed")) {

            exposurePoints.add(calculatePoints((Date) exposureMap.get("Time"), audioTime, audioLevel));
            audioTime = (Date) exposureMap.get("Time");
            audioLevel = (Double) exposureMap.get("dB Equivalent");

        } else if (exposureMap.get("Event").equals("Audio stopped")) {

            exposurePoints.add(calculatePoints((Date) exposureMap.get("Time"), audioTime, audioLevel));
            audioTime = (Date) exposureMap.get("Time");
            audioLevel = 0.0;
        }
    }

    public Double calculatePoints(Date now, Date then, double soundLevel) {

        // Get difference in time between the two times, in seconds
        Double exposureLength = (double) ((now.getTime() - then.getTime()) / 1000);

        //ToDo scrap these logs once this is working
        Log.i("Now time: ", String.valueOf(now.getTime() / 1000));
        Log.i("Then time: ", String.valueOf(then.getTime() / 1000));
        Log.i("exposurelength: ", String.valueOf(exposureLength));

        // Return the exposure points according to the HSE formula (28800 is 8 Hrs)
        return Math.pow(10, ((soundLevel - 65) / 10)) * (exposureLength / 28800);


    }

    public void sumPoints() {

        todaysExposure = 0.0;
        // Adds up the points in the list once the list has been created
        if (exposurePoints.size() > 0) {
            for (int i = 0; i < exposurePoints.size(); i++) {
                System.out.println(exposurePoints.get(i));
                todaysExposure = todaysExposure + exposurePoints.get(i);
            }
        }

        // Create a pair of arraylists as we go, with an exposed date, and the number of points on that date, to populate a list of progressbars

        historyDayList.add(new DayListModel(attractiveDate.format(previousCsvDate), todaysExposure));
//        exposedDays.add(csvDate);
//        exposedDaysPoints.add(todaysExposure);

    }

    public void createTodayExposureView() {

        DecoView arcView = (DecoView) findViewById(R.id.dynamicArcView);

        // Create background track
        arcView.addSeries(new SeriesItem.Builder(Color.argb(255, 218, 218, 218))
                .setRange(0, 100, 100)
                .setInitialVisibility(false)
                .setLineWidth(32f)
                .build());

        //Create data series track
        SeriesItem seriesItem1 = new SeriesItem.Builder(Color.argb(255, 0, 255, 0))
                .setRange(0, 100, 0)
                .setSeriesLabel(new SeriesLabel.Builder("Exposure %.0f%%")
                        .setColorBack(Color.argb(218, 0, 0, 0))
                        .setColorText(Color.argb(255, 255, 255, 255))
                        .build())
                .addEdgeDetail(new EdgeDetail(EdgeDetail.EdgeType.EDGE_INNER, Color.parseColor("#22000000"), 0.4f))
                .setLineWidth(64f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .build();

        int series1Index = arcView.addSeries(seriesItem1);


        arcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true)
                .setDelay(1000)
                .setDuration(1000)
                .build());

//        arcView.setVisibility(View.VISIBLE);

        if (todaysExposure != null) {
            Float f = todaysExposure.floatValue();
            arcView.addEvent(new DecoEvent.Builder(f)
                    .setIndex(series1Index)
                    .setDelay(2000)
                    .setColor(ColorUtils.blendARGB(Color.argb(255, 0, 255, 0), Color.argb(255, 255, 0, 0), (float) (todaysExposure / 100)))
                    .build());
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        startService(new Intent(MainActivity.this, BackgroundService.class));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//When back is pressed, if trackmusic is on, enable the background service.
        if (trackMusic && !firstBoot) {
            startService(new Intent(MainActivity.this, BackgroundService.class));
            Toast.makeText(appContext, "Your music is being tracked", Toast.LENGTH_SHORT).show();
//If firstboot hasn't been completed, post a toast to warn user.
        } else if (!firstBoot) {
            stopService(serviceIntent);
            Toast.makeText(MainActivity.this, "Music is not being tracked.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (userDetailsExist()) {
            trackMusic = true;
        }

        welcomeText.setText("Hi, " + userShortName[0] + "!");

        stopService(serviceIntent);

        if (isService) {
            TextView tv = (TextView) findViewById(R.id.textView1);
            assert tv != null;
            tv.setText("Resume tracking");
            isService = false;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (userDetailsExist()) {
            trackMusic = true;
        }

    }

    private void sendEmail(String feedback) {

        ContextWrapper c = new ContextWrapper(getApplicationContext());

        BackgroundMail.newBuilder(this)
                .withUsername("SolentHearingHealth@gmail.com")
                .withPassword("anechoic3745")
                .withMailTo("lee.davison@solent.ac.uk")
                .withSubject("Data submission from " + userName)
                .withBody(feedback)
                .withAttachments(c.getFilesDir() + "/Headphone_Log.csv")
                .withAttachments(c.getFilesDir() + "/User_Data.csv")
                .withProcessVisibility(true)
                .withOnSuccessCallback(new BackgroundMail.OnSuccessCallback() {
                    @Override
                    public void onSuccess() {
//                        Log.e("Email status: ", "worked");
//                        Toast.makeText(MainActivity.this, "email sent!", Toast.LENGTH_SHORT).show();
                    }
                })
                .withOnFailCallback(new BackgroundMail.OnFailCallback() {
                    @Override
                    public void onFail() {
//                        Log.e("Email status: ", "worked");
//                        Toast.makeText(MainActivity.this, "Email failed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .send();

    }

    public boolean userDetailsExist() {

        SharedPreferences prefs = this.getSharedPreferences(
                "me.leedavison.backgroundtrial", Context.MODE_PRIVATE);
        return prefs.contains(DATA_KEY_NAME);
    }

    public void resetFile() {
        //ToDo: Check this to make sure it works!

        String fileName = "Headphone_Log.csv";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.ENGLISH);
        String time = sdf.format(new Date());
        String entry = "Time , Volume level" + "\n" + time + " , Start of log " + "\n";

        try {
            FileOutputStream out = openFileOutput(fileName, MODE_PRIVATE);
            out.write(entry.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(MainActivity.this, "CSV data file deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, LevelMeterActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_progress) {
            Intent ProgressIntent = new Intent(this, ProgressActivity.class);
            startActivity(ProgressIntent);
            return true;
        }

        if (id == R.id.action_stop_tracking) {
            trackMusic = false;
            Log.i("Trackmusic set: ", "False");

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    public void setupFAB() {
        if (fabSpeedDial != null) {
            fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
                @Override
                public boolean onPrepareMenu(NavigationMenu navigationMenu) {
                    // TODO: Do something with your menu items, or return false if you don't want to show them
                    return true;

                }

                @Override
                public boolean onMenuItemSelected(MenuItem menuItem) {

                    switch (menuItem.getItemId()) {
                        case R.id.action_mail:
                            sendEmail(feedbackText);
                            return true;

                        case R.id.action_bug:

                            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                            LayoutInflater inflater = getLayoutInflater();
                            View v = inflater.inflate(R.layout.alertdialog_edittext, null);
                            alert.setMessage("What's the problem?")
                                    .setTitle("Sorry there's an issue!")
                                    .setIcon(R.drawable.bug);
                            alert.setView(v);
                            alert.setIcon(R.drawable.bug);
                            final EditText edittext = (EditText) v.findViewById(R.id.alert_dialog_edit_text);

                            alert.setPositiveButton("Send Feedback", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (edittext != null) {
                                        feedbackText = edittext.getText().toString();
                                    }
                                    sendEmail(feedbackText);
                                    feedbackText = "No feedback!";
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                            return true;


                        case R.id.action_delete:

                            final EditText passwordText = new EditText(getApplicationContext());
                            final String password = "3745";

                            passwordText.setInputType(InputType.TYPE_CLASS_NUMBER);

                            AlertDialog.Builder fileDeleteDialog = new AlertDialog.Builder(MainActivity.this);
                            fileDeleteDialog.setView(passwordText)
                                    .setMessage("This will delete the log so far... Are you sure you want to do that? \n What's the password?")
                                    .setTitle("Whoah there...")
                                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if (password.equals(String.valueOf(passwordText.getText()))) {
                                                resetFile();
                                            } else {
                                                Toast.makeText(MainActivity.this, "Incorrect password!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                            return true;
                        default:
                            return true;
                    }
                }
            });
        }
    }
}
