package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;

import net.microtrash.wisperingtree.AudioRecorder;
import net.microtrash.wisperingtree.bus.AudioLevelChanged;
import net.microtrash.wisperingtree.bus.AudioPeakDetectionChanged;
import net.microtrash.wisperingtree.bus.SamplingStart;
import net.microtrash.wisperingtree.bus.SamplingStop;
import net.microtrash.wisperingtree.util.Logger;
import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Tools;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.greenrobot.event.EventBus;

public class RecordService extends Service {

    private LoggerInterface mLogger;

    private int mRecNum;
    private Long mLastTimeAboveMin = null;
    private long mSampleStartTime;
    private boolean mSampling = false;
    private Handler mHandler;
    private static final String LOG_TAG = "AudioRecordTest";
    private AudioRecorder mRecorder = null;

    private final static int[][] sampleRates =
            {
                    {22050, 44100, 11025, 8000},
                    {22050, 11025, 8000, 44100},
                    {11025, 8000, 22050, 44100},
                    {8000, 11025, 22050, 44100}
            };
    private int mMinLevel;
    private int mMaxLevel;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLogger = Logger.getInstance();

        EventBus.getDefault().register(this);
        mHandler = new Handler();

        mMinLevel = Tools.getPreferenceInt(getBaseContext(), Static.KEY_MIN_NOISE_VALUE);
        mMaxLevel = Tools.getPreferenceInt(getBaseContext(), Static.KEY_MAX_NOISE_VALUE);
        mRecNum = Tools.getPreferenceInt(getBaseContext(), Static.KEY_LAST_REC_NUM);

        initRecorder(getNextFilename());
        mRecorder.start();
        observeAudio();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Tools.putPreference(getBaseContext(), Static.KEY_LAST_REC_NUM, mRecNum);

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        EventBus.getDefault().unregister(this);
    }

    public void onEvent(AudioPeakDetectionChanged event) {
        mMinLevel = event.getMin();
        mMaxLevel = event.getMax();
    }

    private void initRecorder(String filename) {
        int i = 0;

        do {
            mLogger.log("recorder initializing with: " + sampleRates[0][i]);
            mRecorder = new AudioRecorder(true, MediaRecorder.AudioSource.MIC, sampleRates[0][i], AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

        }
        while ((++i < sampleRates.length) & !(mRecorder.getState() == AudioRecorder.State.INITIALIZING));

        mRecorder.setOutputFile(filename);
        mRecorder.prepare();
    }


    private void observeAudio() {

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mRecorder != null) {
                    int amp = mRecorder.getMaxAmplitude();
                    //Log.v("RecordService", "vol: " + amp + " state: " + mRecorder.getState());

                    //mAudioLevelBar.getAbsoluteMaxValue(30000);
                    float normalizedLevel = (float) amp / (float) 15000;

                    if (amp > mMaxLevel && !mSampling) {
                        //Log.v(TAG, "vol: " + amp + " normalized: " + normalizedLevel);
                        startSampling();
                        mSampling = true;
                    }
                    long now = System.currentTimeMillis();
                    if (amp > mMinLevel) {
                        mLastTimeAboveMin = now;
                    }
                    if (mSampling && mLastTimeAboveMin != null) {
                        long timeDiff = now - mLastTimeAboveMin;
                        // if recording was started AND level has stayed below min level for at least a second -> stop
                        if (timeDiff > 1000 && amp < mMinLevel) {
                            stopSampling();
                            mSampling = false;
                            mLastTimeAboveMin = null;
                        }
                    }
                    // make sure recordings don't exceed the max recording length
                    if (mSampling && now - mSampleStartTime > 10000) {
                        stopSampling();
                        mSampling = false;
                        mLastTimeAboveMin = null;
                    }
                    EventBus.getDefault().post(new AudioLevelChanged(amp));
                    observeAudio();
                }
            }
        }, 50);

    }

    private void startSampling() {
        mSampleStartTime = System.currentTimeMillis();
        mLogger.log("start Sampling");
        mRecorder.setAddSamples(true);
        EventBus.getDefault().post(new SamplingStart());
    }

    private void stopSampling() {
        mLogger.log("StopSampling. Filelength: " + (float) (System.currentTimeMillis() - mSampleStartTime) / 1000f + " sec.");
        final File outputFile = new File(mRecorder.getOutputFile());
        if (mRecorder != null && mLastTimeAboveMin != null) {
            try {
                mRecorder.stop();
                mRecorder.release();

                // delay to make sure file is written completely
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        File newFile = new File(outputFile.getPath().replace(".tmp", ".wav"));
                        outputFile.renameTo(newFile);
                        mLogger.log("new File created", newFile.getName());
                        backupFile(newFile);
                    }
                }, 600);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                initRecorder(getNextFilename());
                mRecorder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        EventBus.getDefault().post(new SamplingStop());
    }

    private void backupFile(final File newFile) {
        final String backupDirPath = Utils.getAppRootDir() + File.separatorChar + Static.BACKUP_DIR_NAME;
        File backupDir = new File(backupDirPath);
        if (!backupDir.exists()) {
            backupDir.mkdir();
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                File backupFile = new File(backupDirPath + File.separatorChar + "recording_" + System.currentTimeMillis() + ".wav");
                try {
                    copy(newFile, backupFile);
                } catch (Exception e) {
                    mLogger.log(e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private String getNextFilename() {
        String dirPath = Utils.getAppRootDir();
        String nextfilename = dirPath + "/audiorecordtest" + mRecNum % Static.MAX_FILES + ".tmp";
        mRecNum++;
        return nextfilename;
    }


}
