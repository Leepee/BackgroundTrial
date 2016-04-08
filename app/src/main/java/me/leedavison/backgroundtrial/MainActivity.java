package me.leedavison.backgroundtrial;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.internal.NavigationMenu;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;

import au.com.bytecode.opencsv.CSV;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;

public class MainActivity extends AppCompatActivity {

    public static boolean isService = false;
    String feedbackText = "No feedback!";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton startserviceButton = (ImageButton) findViewById(R.id.Button1);
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

    @Override
    protected void onResume() {
        super.onResume();
        stopService(new Intent(MainActivity.this,
                BackgroundService.class));
        if (isService) {
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.setText("Click the button to resume service");
            isService = false;
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FabSpeedDial fabSpeedDial = (FabSpeedDial) findViewById(R.id.fab_speed_dial);
        fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
            @Override
            public boolean onPrepareMenu(NavigationMenu navigationMenu) {
                // TODO: Do something with yout menu items, or return false if you don't want to show them
                return true;

            }

            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.action_mail:
                            sendEmail(feedbackText);
                        return true;
                    case R.id.action_bug:
                        final EditText edittext = new EditText(getApplicationContext());
                        AlertDialog.Builder feedbackForm = new AlertDialog.Builder(MainActivity.this);
                        feedbackForm.setView(edittext)
                                .setMessage("What's the problem?")
                                .setTitle("Sorry there's an issue!")
                                .setPositiveButton("Send Feedback", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        feedbackText = String.valueOf(edittext.getText());
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
                                .setMessage("This will delete the log so far... Are you sure you want to do that? \\n What's the password?")
                                .setTitle("Whoah there...")
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        if (password.equals(String.valueOf(passwordText.getText()))){
                                            resetFile();
                                        }else{
                                            Toast.makeText(MainActivity.this, "Incorrect password!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();

                        resetFile();
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void sendEmail(String feedback){

        ContextWrapper c = new ContextWrapper(getApplicationContext());


            BackgroundMail.newBuilder(getApplicationContext())
                    .withUsername("SolentHearingHealth@gmail.com")
                    .withPassword("anechoic3745")
                    .withMailto("lee.davison@solent.ac.uk")
                    .withSubject("Data submission from XXX")
                    .withBody(feedback)
                    .withAttachments(c.getFilesDir() + "/Headphone_Log.csv")
                    .withOnSuccessCallback(new BackgroundMail.OnSuccessCallback() {
                        @Override
                        public void onSuccess() {
                            Log.e("Email status: ", "worked");
//                        Toast.makeText(MainActivity.this, "email sent!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .withOnFailCallback(new BackgroundMail.OnFailCallback() {
                        @Override
                        public void onFail() {
                            Log.e("Email status: ", "worked");
//                        Toast.makeText(MainActivity.this, "Email failed.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .send();

    }

    public void resetFile(){

        String fileName = "Headphone_Log.csv";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String time = sdf.format(new Date());
        String entry = "Time , Volume level" + "\n" + time + " , Start of log " + "\n" ;

        try {
            FileOutputStream out = openFileOutput(fileName, MODE_PRIVATE);
            out.write(entry.getBytes());
            out.close();
        }catch (Exception e){
            e.printStackTrace();
        }
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
