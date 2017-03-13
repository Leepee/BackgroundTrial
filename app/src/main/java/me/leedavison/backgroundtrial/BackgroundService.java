package me.leedavison.backgroundtrial;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.service.notification.NotificationListenerService;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BackgroundService extends Service {
    private NotificationManager mNM;
    private final IBinder mBinder = new LocalBinder();
    private String newtext;
    int volumeLevel;
    int savedLevel;
    AudioManager manager;
    boolean isSaving;
    String state;


    public class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onCreate() {

        Intent intent = new Intent(this, MainActivity.class);
// use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);


        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        final Notification.Builder musicPlayingNotification = new Notification.Builder(getApplicationContext())
                .setContentTitle("Thanks for helping my research!")
                .setContentText("No audio is playing")
                .setSmallIcon(R.drawable.sleeping)
                .setOngoing(true)
                .setContentIntent(pIntent)
                .setPriority(Notification.PRIORITY_MIN)
                .setUsesChronometer(false);
        mNM.notify(1, musicPlayingNotification.build());

        //Setup the scheduled task
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override

            public void run() {

                if ((Integer.valueOf(android.os.Build.VERSION.SDK))>23) {
//                    NotificationManagerCompat.

//                            mNM.getActiveNotifications();

                }

                if (manager.isMusicActive() && (manager.isWiredHeadsetOn() || manager.isBluetoothA2dpOn())) {
                    if (manager.isBluetoothA2dpOn()) Log.i("Bluetooth Audio: ", "on");

                    volumeLevel = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

                    if (!isSaving) {
                        state = "Audio started";
                        saveResult(volumeLevel, state);
                        savedLevel = volumeLevel;
                        isSaving = true;

                        musicPlayingNotification.setContentTitle("Audio is playing")
                                .setContentText("Logging time and volume of audio: " + volumeLevel)
                                .setSmallIcon(R.drawable.headphones);
                        mNM.notify(1, musicPlayingNotification.build());
                    }

                    if (volumeLevel != savedLevel) {
                        state = "Volume changed";
                        saveResult(volumeLevel, state);

                        musicPlayingNotification.setContentText("Logging time and volume of audio: " + volumeLevel);
                        mNM.notify(1, musicPlayingNotification.build());

                        isSaving = true;
                        savedLevel = volumeLevel;
                    }

                } else {

                    if (isSaving) {
                        isSaving = false;
                        state = "Audio stopped";

                        saveResult(volumeLevel, state);

                        musicPlayingNotification.setContentTitle("No audio is playing")
                                .setContentText("Thanks for helping with my research!")
                                .setSmallIcon(R.drawable.sleeping)
                                .setUsesChronometer(false);
                        mNM.notify(1, musicPlayingNotification.build());
                    }
                }
            }
        }, 0, 10, TimeUnit.SECONDS);


        newtext = "BackGroundApp Service Running";

    }

    public void saveResult(int vol, String state) {


        String fileName = "Headphone_Log.csv";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String time = sdf.format(new Date());
        String entry = state + " , " + time + " , " + vol + "\n";

//        String playingApp;
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            MediaSessionManager mediaSessionManager = (MediaSessionManager) this.getSystemService(Context.MEDIA_SESSION_SERVICE);
//            List<MediaController> listController;
//
//        listController = mediaSessionManager.getActiveSessions(AudioManager.OnAudioFocusChangeListener);
//        MediaController controller = listController.get(0);
//
//            playingApp = controller.getPackageName();
//
//            entry = time + " , " + vol + " , " + playingApp + "\n";
//        }

        try {
            FileOutputStream out = openFileOutput(fileName, Context.MODE_APPEND);
            out.write(entry.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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



