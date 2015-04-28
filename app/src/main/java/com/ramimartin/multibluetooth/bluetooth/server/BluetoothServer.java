package com.ramimartin.multibluetooth.bluetooth.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.ramimartin.multibluetooth.bus.BluetoothCommunicator;
import com.ramimartin.multibluetooth.bus.ServeurConnectionFail;
import com.ramimartin.multibluetooth.bus.ServeurConnectionSuccess;

import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
 * Created by Rami MARTIN on 13/04/2014.
 */
public class BluetoothServer implements Runnable {

    private static final String TAG = "BluetoothServer";
    private final LoggerInterface mLogger;
    private boolean mRunning = true;

    private UUID mUUID;
    public String mClientAddress;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStreamWriter mOutputStreamWriter;
    private boolean mReceiveFile = false;
    private String mReceiveFilename;

    public BluetoothServer(BluetoothAdapter bluetoothAdapter, String clientAddress, LoggerInterface logger) {
        mBluetoothAdapter = bluetoothAdapter;
        mClientAddress = clientAddress;
        mUUID = UUID.fromString("e0917680-d427-11e4-8830-" + mClientAddress.replace(":", ""));
        mLogger = logger;
    }

    @Override
    public void run() {
        try {
            mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BLTServer", mUUID);
            mSocket = mServerSocket.accept();
            mInputStream = mSocket.getInputStream();
            mOutputStreamWriter = new OutputStreamWriter(mSocket.getOutputStream());

            int bufferSize = 1024;
            int bytesRead = -1;
            byte[] buffer = new byte[bufferSize];

            EventBus.getDefault().post(new ServeurConnectionSuccess(mClientAddress));

            while (mRunning) {
                final StringBuilder sb = new StringBuilder();
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

                EventBus.getDefault().post(new BluetoothCommunicator(sb.toString()));

            }
        } catch (IOException e) {
            Log.e("", "ERROR : " + e.getMessage());
            EventBus.getDefault().post(new ServeurConnectionFail(mClientAddress));
        }
    }

    public void write(String message) {
        try {
            if (mOutputStreamWriter != null) {
                mOutputStreamWriter.write(message);
                mOutputStreamWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile(File file) {
        int bufferSize = 1024;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] buffer = new byte[bufferSize];
        OutputStream outputStream = null;
        int c = 0;

        try {
            outputStream = mSocket.getOutputStream();
            long totalLength = file.length();
            String command = Protocol.COMMAND_SEND_FILE + Protocol.SEPARATOR + file.getName() + Protocol.SEPARATOR + totalLength;
            outputStream.write(command.getBytes());
            outputStream.flush();

            long bytesWritten = 0;
            while ((c = is.read(buffer, 0, buffer.length)) > 0) {
                outputStream.write(buffer, 0, c);
                outputStream.flush();
                if(bytesWritten < 10000 || bytesWritten + 10000 > totalLength){
                    mLogger.log(new String(buffer));
                }
                bytesWritten += c;
            }
            mLogger.log("sent total of bytes", bytesWritten+"");
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientAddress() {
        return mClientAddress;
    }

    public void closeConnection() {
        if (mSocket != null) {
            try {
                mInputStream.close();
                mInputStream = null;
                mOutputStreamWriter.close();
                mOutputStreamWriter = null;
                mSocket.close();
                mSocket = null;
                mServerSocket.close();
                mServerSocket = null;
                mRunning = false;
            } catch (Exception e) {
            }
            mRunning = false;
        }
    }
}
