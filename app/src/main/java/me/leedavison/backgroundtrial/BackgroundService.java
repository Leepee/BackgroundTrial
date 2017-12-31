package me.leedavison.backgroundtrial;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.FmtNumber;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BackgroundService extends Service {
    final CellProcessor[] listeningLogProcessor = new CellProcessor[]{
            new NotNull(),
            new FmtDate("dd/MM/yyyy hh:mm:ss"),
            new FmtNumber("00"),
            new FmtNumber("00.0")
    };
    private final IBinder mBinder = new LocalBinder();
    int volumeLevel;
    int savedLevel;
    AudioManager manager;
    boolean isSaving;
    String state;
    SharedPreferences prefs;

    // This is a processor for the layout of the listening log
    String LISTENING_LOG_CSV_HEADER[] = {"Event", "Time", "Volume Level", "dB Equivalent"};
    HashMap<String, Object> listeningLogEntry = new HashMap<String, Object>();
    ICsvMapWriter mapWriter = null;
    private NotificationManager mNM;

    @Override
    public void onCreate() {

        prefs = this.getSharedPreferences(getPackageName(), MODE_PRIVATE);

        Intent intent = new Intent(this, MainActivity.class);
// use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);


        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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

//if the app says not to log, kill all the resources.
                if (!MainActivity.trackMusic) {
                    Log.i("BS: ", "killed");
                    stopSelf();
                    mNM.cancel(1);
                    scheduler.shutdown();
                }

// Deprecation in the following method is ok - it is a purpose change.
                if (manager.isMusicActive()
//                        && (manager.isWiredHeadsetOn() || manager.isBluetoothA2dpOn())
                        ) {
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
                        Log.i("BS: ", "recording");
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


        Log.i("BS: ", "Running");

    }

    public void saveResult(int vol, String state) {

        ContextWrapper c = new ContextWrapper(getApplicationContext());

        //Get the dB value of the volume from the sharedprefs
        Float volDB = (prefs.getFloat("PROFILE_KEY_LEVEL_" + String.valueOf(volumeLevel), (float) 99.9));


        //Set up the map to write to the csv
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[0], state);
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[1], new Date());
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[2], vol);
        listeningLogEntry.put(LISTENING_LOG_CSV_HEADER[3], volDB);

        //Write the state change, time, vol level and dB exposure
        try {
            mapWriter = new CsvMapWriter(new FileWriter(c.getFilesDir() + "/Headphone_Log.csv", true),
                    CsvPreference.STANDARD_PREFERENCE);
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


//        String fileName = "Headphone_Log.csv";
//        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
//        String time = sdf.format(new Date());
//        String entry = state + " , " + time + " , " + vol + " , " + volDB + "\n";
//
//

//        String playingApp;

//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            MediaSessionManager mediaSessionManager = (MediaSessionManager) this.getSystemService(Context.MEDIA_SESSION_SERVICE);
//            List<MediaController> listController;
//
//
//        listController = mediaSessionManager.addOnActiveSessionsChangedListener(new );
//        MediaController controller = listController.get(0);
//
//            playingApp = controller.getPackageName();
////
//            entry = time + " , " + vol + " , " + playingApp + "\n";
//        }
//
//        try {
//            FileOutputStream out = openFileOutput(fileName, Context.MODE_APPEND);
//            out.write(entry.getBytes());
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();


    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }
}



