package me.leedavison.backgroundtrial;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;

import com.heinrichreimersoftware.singleinputform.SingleInputFormActivity;
import com.heinrichreimersoftware.singleinputform.steps.Step;
import com.heinrichreimersoftware.singleinputform.steps.TextStep;

import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.FmtNumber;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class questionnaire extends SingleInputFormActivity {

    //String entries for the questionnaire input
    private static final String DATA_KEY_NAME = "name";
    private static final String DATA_KEY_EMAIL = "email";
    private static final String DATA_KEY_AGE = "age";
    private static final String DATA_KEY_SCHOOL = "school";
    final CellProcessor[] listeningLogProcessor = new CellProcessor[]{
            new NotNull(),
            new FmtDate("dd/MM/yyyy hh:mm:ss"),
            new FmtNumber("00"),
            new FmtNumber("00.0")
    };
    // This is a processor for the layout of the listening log
    String LISTENING_LOG_CSV_HEADER[] = {"Event", "Time", "Volume Level", "dB Equivalent"};
    HashMap<String, Object> listeningLogEntry = new HashMap<String, Object>();
    // This is the setup for the header, entry, and layout (processor) for the user data
    String USER_DATA_CSV_HEADER[] = {"Name", "Email", "Age", "School"};
    HashMap<String, Object> userDataEntry = new HashMap<String, Object>();
    CellProcessor[] userDataProcessor = new CellProcessor[]{
            new NotNull(),
            new NotNull(),
            new NotNull(),
            new NotNull()
    };

    Context appContext = MainActivity.getContext();

    // Get a reference to the sharedprefs of the app
    SharedPreferences prefs = appContext.getSharedPreferences(
            "me.leedavison.backgroundtrial", Context.MODE_PRIVATE);


    @Override
    protected List<Step> getSteps(Context context) {
        List<Step> steps = new ArrayList<Step>();

        steps.add(new TextStep(context, DATA_KEY_NAME, InputType.TYPE_CLASS_TEXT, R.string.q_name, R.string.example_error, R.string.q_name_details));

        steps.add(new TextStep(context, DATA_KEY_EMAIL, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, R.string.q_email, R.string.q_email_error, R.string.q_email_details, new TextStep.StepChecker() {
                    @Override
                    public boolean check(String s) {
                        return Patterns.EMAIL_ADDRESS.matcher(s).matches();
                    }
                })
        );

        steps.add(new TextStep(context, DATA_KEY_AGE, InputType.TYPE_CLASS_NUMBER, R.string.q_age, R.string.example_error, R.string.q_age_details));

        steps.add(new TextStep(context, DATA_KEY_SCHOOL, InputType.TYPE_CLASS_TEXT, R.string.q_school, R.string.example_error, R.string.q_school_details));

        // More steps if needed...

        return steps;


    }


    public void initilizeCSV() {

        //Context wrapper to get the file location for the csv
        ContextWrapper c = new ContextWrapper(getApplicationContext());

//        Log.i("Simpedateformat: ", SimpleDateFormat.getDateTimeInstance().toString());

        //Get strings of the sharedprefs from the questions
        String name = prefs.getString(DATA_KEY_NAME, null);
        String email = prefs.getString(DATA_KEY_EMAIL, null);
        String age = prefs.getString(DATA_KEY_AGE, null);
        String schoolclass = prefs.getString(DATA_KEY_SCHOOL, null);

        //Put strings into the hashmap to get written to the csv. Use the header as keys for the key value pairs
        userDataEntry.put(USER_DATA_CSV_HEADER[0], name);
        userDataEntry.put(USER_DATA_CSV_HEADER[1], email);
        userDataEntry.put(USER_DATA_CSV_HEADER[2], age);
        userDataEntry.put(USER_DATA_CSV_HEADER[3], schoolclass);

        //Create the User data entry CSV
        ICsvMapWriter mapWriter = null;
        try {
            //Get a writer for csv and make it write to the files directory for the app
            mapWriter = new CsvMapWriter(new FileWriter(c.getFilesDir() + "/User_Data.csv", true),
                    CsvPreference.STANDARD_PREFERENCE);

            // Write the header as defined above.
            mapWriter.writeHeader(USER_DATA_CSV_HEADER);

            // Write the user entry into the CSV under the headers
            mapWriter.write(userDataEntry, USER_DATA_CSV_HEADER, userDataProcessor);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mapWriter != null) {
                try {
                    mapWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        // Null the writer, and then reinitialize it to write the Listening Log CSV
        mapWriter = null;

        // This writes the header as before, and then a LOG STARTED entry, so that it can be seen when it started.
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[0], "Log created");
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[1], new Date());
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[2], 1);
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[3], 1.0);

        try {
            mapWriter = new CsvMapWriter(new FileWriter(c.getFilesDir() + "/Headphone_Log.csv", true),
                    CsvPreference.STANDARD_PREFERENCE);
            mapWriter.writeHeader(LISTENING_LOG_CSV_HEADER);
            mapWriter.write(listeningLogEntry, LISTENING_LOG_CSV_HEADER, listeningLogProcessor);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mapWriter != null) {
                try {
                    mapWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onFormFinished(Bundle data) {

        //Get the form data and put it in sharedprefs. commit is used because I need it there now, not in background.
        prefs.edit().putString(DATA_KEY_NAME, TextStep.text(data, DATA_KEY_NAME)).commit();
        prefs.edit().putString(DATA_KEY_EMAIL, TextStep.text(data, DATA_KEY_EMAIL)).commit();
        prefs.edit().putString(DATA_KEY_AGE, TextStep.text(data, DATA_KEY_AGE)).commit();
        prefs.edit().putString(DATA_KEY_SCHOOL, TextStep.text(data, DATA_KEY_SCHOOL)).commit();

        initilizeCSV();
    }
}