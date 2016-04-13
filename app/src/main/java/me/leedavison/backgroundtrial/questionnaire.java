package me.leedavison.backgroundtrial;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Toast;

import com.heinrichreimersoftware.singleinputform.SingleInputFormActivity;
import com.heinrichreimersoftware.singleinputform.steps.Step;
import com.heinrichreimersoftware.singleinputform.steps.TextStep;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class questionnaire extends SingleInputFormActivity {
    private static final String DATA_KEY_NAME = "name";
    private static final String DATA_KEY_EMAIL = "email";
    private static final String DATA_KEY_AGE = "age";
    private static final String DATA_KEY_SCHOOL = "school";

    Context appContext = MainActivity.getContext();

    SharedPreferences prefs= appContext.getSharedPreferences(
            "me.leedavison.backgroundtrial", Context.MODE_PRIVATE);


    @Override
    protected List<Step> getSteps(Context context) {
        List<Step> steps = new ArrayList<Step>();

        steps.add(
                new TextStep(context, DATA_KEY_NAME, InputType.TYPE_CLASS_TEXT, R.string.q_name, R.string.example_error, R.string.q_name_details)
        );

        steps.add(
                new TextStep(context, DATA_KEY_EMAIL, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, R.string.q_email, R.string.q_email_error, R.string.q_email_details, new TextStep.StepChecker() {
                    @Override
                    public boolean check(String s) {
                        return Patterns.EMAIL_ADDRESS.matcher(s).matches();
                    }
                })
        );

        steps.add(
                new TextStep(context, DATA_KEY_AGE, InputType.TYPE_CLASS_NUMBER, R.string.q_age, R.string.example_error, R.string.q_age_details)
        );


        steps.add(new TextStep(context, DATA_KEY_SCHOOL, InputType.TYPE_CLASS_TEXT, R.string.q_school, R.string.example_error, R.string.q_school_details)
        );

//        steps.add(
//                new DateStep(context, DATA_KEY_DOB, InputType.TYPE_CLASS_DATETIME, R.string.example, R.string.example_error, new DateStep.StepChecker() {
//                    @Override
//                    public boolean check(int i, int i1, int i2) {
//                        return false;
//                    }
//                })
//        );


        //Add more steps here...

        return steps;
    }

    public void initilizeCSV(){

            String fileName = "Headphone_Log.csv";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            String time = sdf.format(new Date());
            String entry = "Time , Volume level" + "\n" +
                    prefs.getString(DATA_KEY_NAME,null) +
                    prefs.getString(DATA_KEY_EMAIL,null) +
                    prefs.getString(DATA_KEY_AGE,null) +
                    prefs.getString(DATA_KEY_SCHOOL,null) + "\n"
                    + time + " , Start of log " + "\n";

            try {
                FileOutputStream out = openFileOutput(fileName, MODE_PRIVATE);
                out.write(entry.getBytes());
                out.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }



    @Override
    protected void onFormFinished(Bundle data) {
        //Get the form data

        prefs.edit().putString(DATA_KEY_NAME,TextStep.text(data,DATA_KEY_NAME)).apply();
        prefs.edit().putString(DATA_KEY_EMAIL,TextStep.text(data,DATA_KEY_EMAIL)).apply();
        prefs.edit().putString(DATA_KEY_AGE,TextStep.text(data,DATA_KEY_AGE)).apply();
        prefs.edit().putString(DATA_KEY_SCHOOL,TextStep.text(data,DATA_KEY_SCHOOL)).apply();

        initilizeCSV();

//        String text = TextStep.text(data, DATA_KEY_EXAMPLE);
        Toast.makeText(questionnaire.this, TextStep.text(data, DATA_KEY_NAME),Toast.LENGTH_SHORT).show();



    }
}