package com.ramimartin.multibluetooth.bluetooth.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ramimartin.multibluetooth.bluetooth.mananger.BluetoothManager;
import com.ramimartin.multibluetooth.bus.BluetoothCommunicator;
import com.ramimartin.multibluetooth.bus.ClientConnectionFail;
import com.ramimartin.multibluetooth.bus.ClientConnectionSuccess;

import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Protocol;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
 * Created by Rami MARTIN on 13/04/2014.
 */
public class BluetoothClient implements Runnable {

    private boolean mRunning = true;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private UUID mUuid;
    private String mAdressMac;

    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStreamWriter mOutputStreamWriter;

    private BluetoothConnector mBluetoothConnector;
    private boolean mReceiveFile = false;
    private LoggerInterface mLogger;
    private String mReceiveFilename;
    private long mReceiveFileLength;
    private BluetoothManager.OnFileReceivedListener mOnFileReceivedListener;

    public void setLogger(LoggerInterface logger) {
        mLogger = logger;
    }

    public BluetoothClient(BluetoothAdapter bluetoothAdapter, String adressMac, LoggerInterface logger) {
        mBluetoothAdapter = bluetoothAdapter;
        mAdressMac = adressMac;
        String myAddress = bluetoothAdapter.getAddress();
        mUuid = UUID.fromString("e0917680-d427-11e4-8830-" + myAddress.replace(":", ""));
        mLogger = logger;
    }

    @Override
    public void run() {

        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mAdressMac);
//        List<UUID> uuidCandidates = new ArrayList<UUID>();
//        uuidCandidates.add(mUuid);

        int maxTries = 3;
        if (mInputStream == null) {
            mBluetoothConnector = new BluetoothConnector(mBluetoothDevice, true, mBluetoothAdapter, mUuid);


            try {
                mSocket = mBluetoothConnector.connect().getUnderlyingSocket();
                mInputStream = mSocket.getInputStream();
            } catch (IOException e1) {
                Log.e("", "===> mSocket IOException", e1);
                EventBus.getDefault().post(new ClientConnectionFail());
                e1.printStackTrace();
                return;
            }

        }

        if (mSocket == null) {
            Log.e("", "===> mSocket == Null");
            return;
        }

        try {

            mOutputStreamWriter = new OutputStreamWriter(mSocket.getOutputStream());

            int bufferSize = 1024;
            int bytesRead = -1;

            byte[] dataBuffer = new byte[bufferSize];
            byte[] stringBuffer = new byte[1];

            EventBus.getDefault().post(new ClientConnectionSuccess());
            byte commandEnd = Protocol.COMMAND_END.getBytes()[0];

            while (mRunning) {

                String command = "";
                if (!mReceiveFile) {
                    while ((bytesRead = mInputStream.read(stringBuffer, 0, stringBuffer.length)) > 0) {
                        //mLogger.log("receiving byte", ""+new String(stringBuffer, 0, bytesRead)+" command end:"+);
                        if(stringBuffer[0] == commandEnd){
                            break;
                        }else {
                            command += new String(stringBuffer, 0, bytesRead);
                        }
                    }

                } else {
                    mLogger.log("receiving file", mReceiveFilename);

                    int c = 0;
                    long bRead = 0;
                    final String filePath = Utils.getAppRootDir() + "/" + mReceiveFilename;
                    OutputStream oos = new FileOutputStream(filePath);

                    while (bRead < mReceiveFileLength && (c = mInputStream.read(dataBuffer, 0, dataBuffer.length)) > 0) {
                        if ((bRead + bufferSize) >= mReceiveFileLength) {
                            c = (int) (mReceiveFileLength - bRead);
                            mLogger.log("rest bytes", "" + c);
                        }
                        /*if (bRead < 10000 || bRead + 10000 > mReceiveFileLength) {
                            mLogger.log(new String(buffer));
                        }*/
                        oos.write(dataBuffer, 0, c);
                        oos.flush();
                        bRead += c;
                        mLogger.log("read " + bRead + " of " + mReceiveFileLength + " bytes");
                        Thread.sleep(1000);
                    }
                    oos.close();


                    mLogger.log("saved file", mReceiveFilename);

                    if (mOnFileReceivedListener != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mOnFileReceivedListener.onFileReceived(new File(filePath));
                            }
                        });

                    }
                    mReceiveFile = false;
                    mReceiveFilename = null;

                }
                if (command.startsWith(Protocol.COMMAND_SEND_FILE)) {
                    // "SEND_FILE:filename.ext"
                    try {
                        String[] commandArray = command.split(Protocol.SEPARATOR);
                        mLogger.log("New command:" + command);
                        mReceiveFile = true;
                        mReceiveFilename = commandArray[1];
                        mReceiveFileLength = Long.parseLong(commandArray[2]);
                    } catch (Exception e) {
                        mLogger.log("Protocoll exception command could not be parsed:" + command);
                    }
                } else {
                    EventBus.getDefault().post(new BluetoothCommunicator(command));
                }

            }
        } catch (IOException e) {
            Log.e("", "===> Client run");
            e.printStackTrace();
            EventBus.getDefault().post(new ClientConnectionFail());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void write(String message) {
        try {
            mOutputStreamWriter.write(message);
            mOutputStreamWriter.flush();
        } catch (IOException e) {
            Log.e("", "===> Client write");
            e.printStackTrace();
        }
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

    public void closeConnexion() {
        if (mSocket != null) {
            try {
                mInputStream.close();
                mInputStream = null;
                mOutputStreamWriter.close();
                mOutputStreamWriter = null;
                mSocket.close();
                mSocket = null;
                mBluetoothConnector.close();
            } catch (Exception e) {
                Log.e("", "===> Client closeConnexion");
            }
            mRunning = false;
        }
    }


    public void setOnFileReceivedListener(BluetoothManager.OnFileReceivedListener onFileReceivedListener) {
        mOnFileReceivedListener = onFileReceivedListener;
    }

    public BluetoothManager.OnFileReceivedListener getOnFileReceivedListener() {
        return mOnFileReceivedListener;
    }
}
