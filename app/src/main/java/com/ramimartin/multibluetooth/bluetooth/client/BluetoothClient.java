package com.ramimartin.multibluetooth.bluetooth.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.ramimartin.multibluetooth.bus.BluetoothCommunicator;
import com.ramimartin.multibluetooth.bus.ClientConnectionFail;
import com.ramimartin.multibluetooth.bus.ClientConnectionSuccess;

import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Protocol;
import net.microtrash.wisperingtree.util.Utils;

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

    public void setLogger(LoggerInterface logger) {
        mLogger = logger;
    }

    public BluetoothClient(BluetoothAdapter bluetoothAdapter, String adressMac, LoggerInterface logger) {
        mBluetoothAdapter = bluetoothAdapter;
        mAdressMac = adressMac;
        mUuid = UUID.fromString("e0917680-d427-11e4-8830-" + bluetoothAdapter.getAddress().replace(":", ""));
        mLogger = logger;
    }

    @Override
    public void run() {

        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mAdressMac);
//        List<UUID> uuidCandidates = new ArrayList<UUID>();
//        uuidCandidates.add(mUuid);

        while (mInputStream == null) {
            mBluetoothConnector = new BluetoothConnector(mBluetoothDevice, true, mBluetoothAdapter, mUuid);

            try {
                mSocket = mBluetoothConnector.connect().getUnderlyingSocket();
                mInputStream = mSocket.getInputStream();
            } catch (IOException e1) {
                Log.e("", "===> mSocket IOException", e1);
                EventBus.getDefault().post(new ClientConnectionFail());
                e1.printStackTrace();
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
            byte[] buffer = new byte[bufferSize];

            EventBus.getDefault().post(new ClientConnectionSuccess());

            while (mRunning) {

                final StringBuilder sb = new StringBuilder();
                if (!mReceiveFile) {
                    bytesRead = mInputStream.read(buffer);
                    if (bytesRead != -1) {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize] != 0)) {
                            result = result + new String(buffer, 0, bytesRead);
                            bytesRead = mInputStream.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead);
                        sb.append(result);
                    }
                } else {
                    if (mLogger != null) {
                        mLogger.log("receiving file", mReceiveFilename);
                    }
                    int c = 0;
                    long bRead = 0;
                    OutputStream oos = new FileOutputStream(Utils.getAppRootDir() + "/" + mReceiveFilename);

                    while (bRead < mReceiveFileLength && (c = mInputStream.read(buffer, 0, buffer.length)) > 0) {
                        if ((bRead + bufferSize) >= mReceiveFileLength) {
                            c = (int) (mReceiveFileLength - bRead);
                            mLogger.log("rest bytes", "" + c);
                        }
                        if(bRead < 10000 || bRead + 10000 > mReceiveFileLength){
                            mLogger.log(new String(buffer));
                        }
                        oos.write(buffer, 0, c);
                        oos.flush();
                        bRead += c;
                        mLogger.log("read " + bRead + " of " + mReceiveFileLength + " bytes");
                    }
                    oos.close();


                    mLogger.log("saved file", mReceiveFilename);


                    mReceiveFile = false;
                    mReceiveFilename = null;

                }
                if (sb.toString().startsWith(Protocol.COMMAND_SEND_FILE)) {
                    // "SEND_FILE:filename.ext"
                    try {
                        String[] command = sb.toString().split(Protocol.SEPARATOR);
                        mReceiveFile = true;
                        mReceiveFilename = command[1];
                        mReceiveFileLength = Long.parseLong(command[2]);
                    } catch (Exception e) {
                        mLogger.log("Protocoll exception command could not be parsed:" + sb.toString());
                    }
                } else {
                    EventBus.getDefault().post(new BluetoothCommunicator(sb.toString()));
                }

            }
        } catch (IOException e) {
            Log.e("", "===> Client run");
            e.printStackTrace();
            EventBus.getDefault().post(new ClientConnectionFail());
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


}
