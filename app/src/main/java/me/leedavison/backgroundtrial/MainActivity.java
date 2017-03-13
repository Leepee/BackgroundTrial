package me.leedavison.backgroundtrial;


import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.internal.NavigationMenu;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;

public class MainActivity extends AppCompatActivity {

    private static final String DATA_KEY_NAME = "name";
    private static final String DATA_KEY_EMAIL = "email";
    private static final String DATA_KEY_AGE = "age";
    private static final String DATA_KEY_SCHOOL = "school";
    public static boolean isService = false;
    public static Context appContext;
    public boolean trackMusic = false;
    boolean firstBoot;
    String feedbackText = "No feedback!";
    String userName;
    String userEmail;
    String userAge;
    String userSchool;
    TextView welcomeText;
    String userShortName[];

    public static Context getContext() {
        return appContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        appContext = getApplicationContext();

        SharedPreferences prefs = this.getSharedPreferences(
                "me.leedavison.backgroundtrial", Context.MODE_PRIVATE);

        userName = prefs.getString(DATA_KEY_NAME, "welcome to my music tracker!");
        userEmail = prefs.getString(DATA_KEY_EMAIL, null);
        userAge = prefs.getString(DATA_KEY_AGE, null);
        userSchool = prefs.getString(DATA_KEY_SCHOOL, null);


        userShortName = userName != null ? userName.split(" ") : new String[0];


        if (!userDetailsExist()) {
            trackMusic = false;
            firstBoot = true;
            Intent myIntent = new Intent(MainActivity.this, questionnaire.class);
            MainActivity.this.startActivity(myIntent);
        } else {
            trackMusic = true;
            firstBoot = false;
        }

        welcomeText = (TextView) findViewById(R.id.welcome_message);

        if (welcomeText != null) {
            welcomeText.setText("Hi, " + userShortName[0] + "!");
        }


        ImageButton startserviceButton = (ImageButton) findViewById(R.id.Button1);
        ImageButton stopserviceButton = (ImageButton) findViewById(R.id.Button2);


        if (startserviceButton != null) {
            startserviceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startService(new Intent(MainActivity.this, BackgroundService.class));
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                    isService = true;
                }
            });
        }

        if (stopserviceButton != null) {
            stopserviceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    trackMusic = false;

                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                }
            });
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.headphonesicon2);


        FabSpeedDial fabSpeedDial = (FabSpeedDial) findViewById(R.id.fab_speed_dial);
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

    @Override
    protected void onPause() {
        super.onPause();

        if (trackMusic && !firstBoot) {
            startService(new Intent(MainActivity.this, BackgroundService.class));
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        } else if (!firstBoot) {
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

        stopService(new Intent(MainActivity.this,
                BackgroundService.class));
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
                .withMailto("lee.davison@solent.ac.uk")
                .withSubject("Data submission from " + userName)
                .withBody(feedback)
                .withAttachments(c.getFilesDir() + "/Headphone_Log.csv")
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

        return super.onOptionsItemSelected(item);
    }
}
