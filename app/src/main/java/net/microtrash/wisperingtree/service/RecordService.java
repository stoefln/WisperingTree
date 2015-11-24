package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;

import net.microtrash.wisperingtree.AudioRecorder;
import net.microtrash.wisperingtree.bus.AdaptiveThresholdChanged;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.LinkedList;

import de.greenrobot.event.EventBus;

public class RecordService extends Service {

    private static final int MAX_AMP_BUFFER_SIZE = 100;
    private LoggerInterface mLogger;

    private int mRecNum;
    private Long mLastTimeAboveMin = null;
    private long mSampleStartTime;
    private boolean mSampling = false;
    private Handler mHandler;
    private static final String LOG_TAG = "AudioRecordTest";
    private AudioRecorder mRecorder = null;
    private LinkedList<Integer> mMaxAmpBuffer = new LinkedList<>();
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

                    if (amp > mMaxLevel && !mSampling) {
                        //Log.v(TAG, "vol: " + amp + " normalized: " + normalizedLevel);
                        mMaxAmpBuffer.clear();
                        startSampling();
                        mSampling = true;
                    }

                    if (!mSampling) {
                        tuneAdaptiveThreshold(amp);
                    }

                    long now = System.currentTimeMillis();
                    if (amp > mMinLevel) {
                        mLastTimeAboveMin = now;
                    }
                    if (mSampling && mLastTimeAboveMin != null) {
                        long timeDiff = now - mLastTimeAboveMin;
                        // if recording was started AND level has stayed below min level for at least 2 seconds -> stop
                        if (timeDiff > 2000 && amp < mMinLevel) {
                            stopSampling();
                            mSampling = false;
                            mLastTimeAboveMin = null;
                        }
                    }
                    // make sure recordings don't exceed the max recording length
                    if (mSampling && now - mSampleStartTime > 25000) {
                        stopSampling();
                        mSampling = false;
                        mLastTimeAboveMin = null;
                        // threshold might be too low. increase it by 2000
                        mMinLevel += 2000;
                        setMaxLevelByMinLevel();
                        mLogger.log("Loooong sample! Adaptive threshold increased to " + mMinLevel + " \t" + mMaxLevel);
                        EventBus.getDefault().post(new AdaptiveThresholdChanged(mMinLevel, mMaxLevel));
                    }
                    EventBus.getDefault().post(new AudioLevelChanged(amp));
                    observeAudio();
                }
            }
        }, 50);

    }

    private void tuneAdaptiveThreshold(int amp) {
        // adaptive threshold:
        // if no recording happened in the last 20 seconds, check the last 20 seconds of maxAmplitudes and set the upper threshold 4 db above and the lower threshold 2 db above
        mMaxAmpBuffer.add(amp);

        if(mMaxAmpBuffer.size() > MAX_AMP_BUFFER_SIZE){
            int max = 0;
            for (Integer val : mMaxAmpBuffer) {
                if(max < val){
                    max = val;
                }
            }
            // bsp: 10 000 - 5000 = 5000
            int diff = mMinLevel - max;
            mMinLevel = mMinLevel - diff / 3 + 3000;
            setMaxLevelByMinLevel();

            mLogger.log("set adaptive threshold to " + mMinLevel + " \t" + mMaxLevel);
            EventBus.getDefault().post(new AdaptiveThresholdChanged(mMinLevel, mMaxLevel));
            for (int i = 0; i<= 20; i++) {
                mMaxAmpBuffer.removeFirst();
            }
        }
    }

    private void setMaxLevelByMinLevel() {
        mMaxLevel = (int) (mMinLevel * 1.3 + 2000);
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
        File backupFile = new File(backupDirPath + File.separatorChar + "recording_" + System.currentTimeMillis() + ".wav");
        try {
            copy(newFile, backupFile);
        } catch (Exception e) {
            mLogger.log(e.getMessage());
        }
    }

    // in order to find a bug where we have many duplicates in the backed up files, we will store some information and show error log messages in case duplicates are detected:
    Hashtable<Long, String> mFileSizes = new Hashtable<>();

    public void copy(final File src, final File dst) {


        InputStream in = null;
        try {
            in = new FileInputStream(src);

            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            long bytesWritten = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                bytesWritten += len;
            }
            if (mFileSizes.get(bytesWritten) == null) {
                mFileSizes.put(bytesWritten, dst.getName());
                mLogger.log("Duplicated " + bytesWritten + " bytes");
            } else {
                mLogger.log("POTENTIAL BUG: FILESIZE DUPLICATE! These two files have the same size of (" + bytesWritten + " bytes): " + mFileSizes.get(bytesWritten) + "  " + dst.getName());
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mLogger.log(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            mLogger.log(e.getMessage());
        }
    }

    private String getNextFilename() {
        String dirPath = Utils.getAppRootDir();
        String nextfilename = dirPath + "/audiorecordtest" + mRecNum % Static.MAX_FILES + ".tmp";
        mRecNum++;
        return nextfilename;
    }


}
