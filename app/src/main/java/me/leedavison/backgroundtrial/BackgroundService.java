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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

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


        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("This is a notification!")
                .setContentText("Background service is totally running, and unkillable.")
                .setLights(Color.MAGENTA, 50, 50)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(1, notification);

        final NotificationCompat.Builder musicPlayingNotification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Thanks for helping my research!")
                .setContentText("No music is playing")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setUsesChronometer(false);
        mNM.notify(1, musicPlayingNotification.build());
        //Setup the scheduled task

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {



//                NotificationCompat.Builder musicPlayingNotification = new NotificationCompat.Builder(getApplicationContext())
//                        .setContentTitle("Music is not playing!")
//                        .setContentText("Start some music to log")
//                        .setSmallIcon(R.mipmap.ic_launcher)
//                        .setUsesChronometer(false)
//                        .build();
//                mNM.notify(1,musicPlayingNotification);


                if(manager.isMusicActive())
                {
                    volumeLevel = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

                    musicPlayingNotification.setContentTitle("Music is playing")
                            .setContentText("Logging time and volume of music: " + volumeLevel);

                    mNM.notify(1, musicPlayingNotification.build());


//                    Notification musicPlayingNotification = new Notification.Builder(getApplicationContext())
//                            .setContentTitle("Music is playing!")
//                            .setContentText("Logging playing music at volume level" + volumeLevel)
//                            .setSmallIcon(R.mipmap.ic_launcher)
//                            .setUsesChronometer(true)
//                            .build();
//                    mNM.notify(1,musicPlayingNotification);

                }else{

                    musicPlayingNotification.setContentTitle("No music is playing")
                            .setContentText("Thanks for helping with my research!")
                            .setUsesChronometer(false);

                    mNM.notify(1, musicPlayingNotification.build());

//                    Notification musicPlayingNotification = new Notification.Builder(getApplicationContext())
//                            .setContentTitle("Music is not playing!")
//                            .setContentText("Start some music to log")
//                            .setSmallIcon(R.mipmap.ic_launcher)
//                            .setUsesChronometer(false)
//                            .build();
//                    mNM.notify(1,musicPlayingNotification);
                }

            }
        }, 0, 5, TimeUnit.SECONDS);


        newtext = "BackGroundApp Service Running";

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    public void onDestroy() {
        mNM.cancel(R.string.local_service_started);
        stopSelf();
    }
//    private void showNotification() {
//        CharSequence text = getText(R.string.local_service_started);
//
//        Notification notification = new Notification(R.mipmap.ic_launcher, text, System.currentTimeMillis());
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,new Intent(this, MainActivity.class), 0);
////        notification.setLatestEventInfo(this, "BackgroundAppExample",newtext, contentIntent);
//        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        mNM.notify(R.string.local_service_started, notification);
//    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}



