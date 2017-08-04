// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package me.leedavison.backgroundtrial;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;

import static me.leedavison.backgroundtrial.MainActivity.getContext;


public class LevelMeterActivity extends Activity implements
        MicrophoneInputListener {

    private static final String TAG = "LevelMeterActivity";
    MicrophoneInput micInput;  // The micInput object provides real time audio.
    TextView mdBTextView;
    TextView mdBFractionTextView;
    BarLevelDrawable mBarLevel;
    String[] perms = {"android.permission.RECORD_AUDIO"};
    SharedPreferences prefs;
    String offsetKey = "OFFSET_VALUE_KEY";
    String profileKey = "PROFILE_KEY_LEVEL_";
    DecimalFormat df = new DecimalFormat("##.# dB");

    Float dBLevel = 0f;
    AudioManager am;


    MediaPlayer signalPlayer;


    double mOffsetdB = 10;  // Offset for bar, i.e. 0 lit LEDs at 10 dB.
    // The Google ASR input requirements state that audio input sensitivity
    // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
    // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
    double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    // For displaying error in calibration.
    double mDifferenceFromNominal = 0.0;
    double mRmsSmoothed;  // Temporally filtered version of RMS.
    double mAlpha = 0.9;  // Coefficient of IIR smoothing filter for RMS.
    private TextView mGainTextView;
    private int mSampleRate;  // The audio sampling rate to use.
    private int mAudioSource;  // The audio source to use.
    // Variables to monitor UI update and check for slow updates.
    private volatile boolean mDrawing;
    private volatile int mDrawingCollided;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        prefs = this.getSharedPreferences(getPackageName(), MODE_PRIVATE);

        signalPlayer = MediaPlayer.create(getApplicationContext(), getResources().getIdentifier("fpn", "raw", getPackageName()));
        signalPlayer.setLooping(true);

        // Here the micInput object is created for audio capture.
        // It is set up to call this object to handle real time audio frames of
        // PCM samples. The incoming frames will be handled by the
        // processAudioFrame method below.
        micInput = new MicrophoneInput(this);

        // Read the layout and construct.
        setContentView(R.layout.level_meter_main);

        // Get a handle that will be used in async thread post to update the
        // display.
        mBarLevel = (BarLevelDrawable) findViewById(R.id.bar_level_drawable_view);
        mdBTextView = (TextView) findViewById(R.id.dBTextView);
        mdBFractionTextView = (TextView) findViewById(R.id.dBFractionTextView);
        mGainTextView = (TextView) findViewById(R.id.gain);


        //If the calibration offset exists, set it as the
        if (prefs.contains(offsetKey)) {
            mDifferenceFromNominal = prefs.getFloat(offsetKey, 0);
            mGainTextView.setText(df.format(mDifferenceFromNominal));
        }


        // Toggle Button handler.

        final ToggleButton onOffButton = (ToggleButton) findViewById(
                R.id.on_off_toggle_button);
        final ToggleButton signalOnOffButton = (ToggleButton) findViewById(
                R.id.signal_source_toggle);
        final Button calibrateButton = (Button) findViewById(
                R.id.calibrate_button);
        final Button profileButton = (Button) findViewById(
                R.id.profile_button);

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LevelMeterActivity.this);
                builder.setMessage("The device will now be profiled. Ensure the headphones are on the fixture")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                new MyTask(LevelMeterActivity.this).execute();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

            }
        });

        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                prefs.edit().putFloat(offsetKey, (float) mDifferenceFromNominal).commit();

                Log.i("offset is stored as: ", String.valueOf(prefs.getFloat(offsetKey, 0)));
            }
        });


        CompoundButton.OnCheckedChangeListener changeChecker = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.on_off_toggle_button:
                        if (onOffButton.isChecked()) {
                            readPreferences();
                            micInput.setSampleRate(mSampleRate);
                            micInput.setAudioSource(mAudioSource);
                            micInput.start();
                        } else {
                            micInput.stop();
                        }

                    case R.id.signal_source_toggle:
                        if (signalOnOffButton.isChecked()) {
                            signalPlayer.start();
                        } else {
                            if (signalPlayer.getCurrentPosition() > 1)
                                signalPlayer.pause();
                            signalPlayer.seekTo(0);
                        }
                }
            }
        };

        onOffButton.setOnCheckedChangeListener(changeChecker);
        signalOnOffButton.setOnCheckedChangeListener(changeChecker);
        // Sort out perms

        if (Build.VERSION.SDK_INT >= 23) {
            int res = getContext().checkCallingOrSelfPermission(perms[0]);
            Log.i("permission state: ", String.valueOf(res));
            if (res != 0) {
                requestPermissions(perms, 200);
            }
        }

//        ToggleButton.OnClickListener tbListener =
//                new ToggleButton.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (onOffButton.isChecked()) {
//                            readPreferences();
//                            micInput.setSampleRate(mSampleRate);
//                            micInput.setAudioSource(mAudioSource);
//                            micInput.start();
//                        } else {
//                            micInput.stop();
//                        }
//                    }
//                };
//        onOffButton.setOnClickListener(tbListener);

//        ToggleButton.OnClickListener signalTBListener =
//                new ToggleButton.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (signalOnOffButton.isChecked()) {
//
////                            try {
////                                signalPlayer.prepare();
////                            } catch (IOException e) {
////                                e.printStackTrace();
////                            }
//                            signalPlayer.start();
//
//
////                            readPreferences();
////                            micInput.setSampleRate(mSampleRate);
////                            micInput.setAudioSource(mAudioSource);
////                            micInput.start();
//                        } else {
//                            signalPlayer.stop();
//                        }
//                    }
//                };
//        onOffButton.setOnClickListener(signalTBListener);

        // Level adjustment buttons.

        // Minus 5 dB button event handler.
        Button minus5dbButton = (Button) findViewById(R.id.minus_5_db_button);
        DbClickListener minus5dBButtonListener = new DbClickListener(-5.0);
        minus5dbButton.setOnClickListener(minus5dBButtonListener);

        // Minus 1 dB button event handler.
        Button minus1dbButton = (Button) findViewById(R.id.minus_1_db_button);
        DbClickListener minus1dBButtonListener = new DbClickListener(-1.0);
        minus1dbButton.setOnClickListener(minus1dBButtonListener);

        // Minus 0.1 dB button event handler.
        Button minuspoint1dbButton = (Button) findViewById(R.id.minus_point1_db_button);
        DbClickListener minuspoint1dBButtonListener = new DbClickListener(-0.1);
        minuspoint1dbButton.setOnClickListener(minuspoint1dBButtonListener);

        // Plus 0.1 dB button event handler.
        Button pluspoint1dbButton = (Button) findViewById(R.id.plus_point1_db_button);
        DbClickListener pluspoint1dBButtonListener = new DbClickListener(0.1);
        pluspoint1dbButton.setOnClickListener(pluspoint1dBButtonListener);

        // Plus 1 dB button event handler.
        Button plus1dbButton = (Button) findViewById(R.id.plus_1_db_button);
        DbClickListener plus1dBButtonListener = new DbClickListener(1.0);
        plus1dbButton.setOnClickListener(plus1dBButtonListener);

        // Plus 5 dB button event handler.
        Button plus5dbButton = (Button) findViewById(R.id.plus_5_db_button);
        DbClickListener plus5dBButtonListener = new DbClickListener(5.0);
        plus5dbButton.setOnClickListener(plus5dBButtonListener);


        // Settings button, launches the settings dialog.

        Button settingsButton = (Button) findViewById(R.id.settingsButton);
        Button.OnClickListener settingsBtnListener =
                new Button.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        final ToggleButton onOffButton = (ToggleButton) findViewById(
                                R.id.on_off_toggle_button);
                        onOffButton.setChecked(false);
                        LevelMeterActivity.this.micInput.stop();

                        Intent settingsIntent = new Intent(LevelMeterActivity.this,
                                Settings.class);
                        LevelMeterActivity.this.startActivity(settingsIntent);
                    }
                };
        settingsButton.setOnClickListener(settingsBtnListener);
    }

    /**
     * Method to read the sample rate and audio source preferences.
     */
    private void readPreferences() {
        SharedPreferences preferences = getSharedPreferences("LevelMeter",
                MODE_PRIVATE);
        mSampleRate = preferences.getInt("SampleRate", 8000);
        mAudioSource = preferences.getInt("AudioSource",
                MediaRecorder.AudioSource.MIC);
    }

    /**
     * This method gets called by the micInput object owned by this activity.
     * It first computes the RMS value and then it sets up a bit of
     * code/closure that runs on the UI thread that does the actual drawing.
     */
    @Override
    public void processAudioFrame(short[] audioFrame) {
        if (!mDrawing) {
            mDrawing = true;
            // Compute the RMS value. (Note that this does not remove DC).
            double rms = 0;
            for (int i = 0; i < audioFrame.length; i++) {
                rms += audioFrame[i] * audioFrame[i];
            }
            rms = Math.sqrt(rms / audioFrame.length);

            // Compute a smoothed version for less flickering of the display.
            mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
            final double rmsdB = 20.0 * Math.log10(mGain * mRmsSmoothed);

            // Set up a method that runs on the UI thread to update of the LED bar
            // and numerical display.
            mBarLevel.post(new Runnable() {
                @Override
                public void run() {
                    // The bar has an input range of [0.0 ; 1.0] and 10 segments.
                    // Each LED corresponds to 6 dB.
                    mBarLevel.setLevel((mOffsetdB + rmsdB) / 60);

                    DecimalFormat df = new DecimalFormat("##");
                    mdBTextView.setText(df.format(20 + rmsdB));

                    DecimalFormat df_fraction = new DecimalFormat("#");
                    int one_decimal = (int) (Math.round(Math.abs(rmsdB * 10))) % 10;
                    mdBFractionTextView.setText(Integer.toString(one_decimal));

//                    dBLevel = Float.parseFloat((df.format(20 + rmsdB) + "." + one_decimal ));
                    dBLevel = Float.parseFloat(String.valueOf((df.format(20 + rmsdB)) + "." + String.valueOf(one_decimal)));
//                    Log.i("Spl: ", String.valueOf(dBLevel));


                    mDrawing = false;
                }
            });
        } else {
            mDrawingCollided++;
            Log.v(TAG, "Level bar update collision, i.e. update took longer " +
                    "than 20ms. Collision count" + Double.toString(mDrawingCollided));
        }
    }

    /**
     * Inner class to handle press of gain adjustment buttons.
     */
    private class DbClickListener implements Button.OnClickListener {
        private double gainIncrement;

        public DbClickListener(double gainIncrement) {
            this.gainIncrement = gainIncrement;
        }

        @Override
        public void onClick(View v) {
            LevelMeterActivity.this.mGain *= Math.pow(10, gainIncrement / 20.0);
            mDifferenceFromNominal -= gainIncrement;
            mGainTextView.setText(df.format(mDifferenceFromNominal));
        }
    }

    public class MyTask extends AsyncTask<Void, Void, Void> {


        ProgressDialog pd;
        private Context mContext;

        public MyTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(LevelMeterActivity.this);
            pd.setMessage("One moment, profiling device volume levels!");
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            for (int i = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); i > 0; i--) {

                am.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
//                Log.i("Volume is ", String.valueOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)));
                signalPlayer.start();

                try {
                    Thread.sleep(200);
                    prefs.edit().putFloat(profileKey + String.valueOf(i), dBLevel).apply();
                    Thread.sleep(200);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                signalPlayer.pause();
                Log.i("Vol " + String.valueOf(i) + " contains: ",String.valueOf(prefs.getFloat(profileKey + String.valueOf(i),0)));

            }




//            Iterator iterator = prefs.getAll().entrySet().iterator();
//            while (iterator.hasNext()) {
//
//                Map.Entry pair = (Map.Entry) iterator.next();
//                Log.i("Prefs contains: ", String.valueOf(pair.getValue() + String.valueOf(pair.getValue())));
//                iterator.remove();
//            }




//                System.out.println(pair.getKey() + " = " + pair.getValue());
//                it.remove(); // avoids a ConcurrentModificationExceptionx


//            for (int i = prefs.getAll().size(); i>0; i--) {
//                Log.i("Prefs contains: ", prefs.getAll().get();
//            }
            //set volume to max


            //Set volume 1 notch below


//
//            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
//            signalPlayer.start();
//
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            signalPlayer.pause();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (pd != null) {
                pd.dismiss();
            }
        }
    }


}



