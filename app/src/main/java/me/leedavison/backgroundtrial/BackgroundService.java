package me.leedavison.backgroundtrial;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BackgroundService extends Service {
    private NotificationManager mNM;
    Bundle b;
    Intent notificationIntent;
    private final IBinder mBinder = new LocalBinder();
    private String newtext;
    int volumeLevel;
    AudioManager manager;


    public class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onCreate() {

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        final NotificationCompat.Builder musicPlayingNotification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Thanks for helping my research!")
                .setContentText("No music is playing")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(Color.GRAY)
                .setUsesChronometer(false);
        mNM.notify(1, musicPlayingNotification.build());

        //Setup the scheduled task
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {


                if(manager.isMusicActive())
                {
                    volumeLevel = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

                    musicPlayingNotification.setContentTitle("Music is playing")
                            .setContentText("Logging time and volume of music: " + volumeLevel);

                    mNM.notify(1, musicPlayingNotification.build());

                    saveResult(volumeLevel);

                }else{

                    musicPlayingNotification.setContentTitle("No music is playing")
                            .setContentText("Thanks for helping with my research!")
                            .setUsesChronometer(false);

                    mNM.notify(1, musicPlayingNotification.build());

                }

            }
        }, 0, 5, TimeUnit.SECONDS);


        newtext = "BackGroundApp Service Running";

    }

    public boolean saveResult(int vol){


        String fileName = "Headphone_Log.csv";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String time = sdf.format(new Date());
        String entry = time + " , " + vol +"\n";

        try {
            FileOutputStream out = openFileOutput(fileName, Context.MODE_APPEND);
            out.write(entry.getBytes());
            out.close();
        }catch (Exception e){
            e.printStackTrace();
        }



        return true;
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    public void onDestroy() {
        mNM.cancel(R.string.local_service_started);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}



