package me.leedavison.backgroundtrial;

import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ListView;

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
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {

    // Set up the reader, and the processors

    private static CustomAdapter adapter;
    final CellProcessor[] listeningLogProcessor = new CellProcessor[]{
            new Optional(),
            new ParseDate("dd/MM/yyyy hh:mm:ss"),
            new ParseInt(),
            new ParseDouble()
    };
    String LISTENING_LOG_CSV_HEADER[] = {"Event", "Time", "Volume Level", "dB Equivalent"};
    HashMap<String, Object> listeningLogEntry[];
    Map<String, Object> exposureMap;
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    SimpleDateFormat attractiveDate = new SimpleDateFormat("EEE dd/MM/yyyy");
    Date csvDate;
    Date previousCsvDate;
    Double audioLevel = 0.0;
    Date audioTime;
    Double todaysExposure = 0.0;
    ArrayList<Double> exposurePoints = new ArrayList<>();
    // This is a pair of array lists for storing final results to populate the bars
    ArrayList<Date> exposedDays = new ArrayList<>();


    //Testing something...
    ArrayList<Double> exposedDaysPoints = new ArrayList<>();
    ArrayList<DayListModel> historyDayList = new ArrayList<>();
    ListView listView;
    ICsvMapReader mapReader = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        ContextWrapper c = new ContextWrapper(getApplicationContext());

        listView = (ListView) findViewById(R.id.history_view);

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

    private void createBarList() {

        //Reverse the list so that the dates are soonest-first
        Collections.reverse(historyDayList);
        adapter = new CustomAdapter(historyDayList, getApplicationContext());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DayListModel dayListModel = historyDayList.get(position);

                Snackbar.make(view, String.valueOf(dayListModel.getExposure()), Snackbar.LENGTH_LONG)
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


}
