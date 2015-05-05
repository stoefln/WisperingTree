package com.ramimartin.multibluetooth.bluetooth.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.ramimartin.multibluetooth.bus.ServeurConnectionFail;
import com.ramimartin.multibluetooth.bus.ServeurConnectionSuccess;

import net.microtrash.wisperingtree.bus.FileSentToClient;
import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
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
    private LinkedList<File> mFilesToSend = new LinkedList<>();

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
                if(mFilesToSend.size() > 0) {
                    File nextFile = mFilesToSend.getFirst();
                    sendFileNow(nextFile);
                    mFilesToSend.removeFirst();
                }
                Thread.sleep(500);
            }
        } catch (IOException e) {
            mLogger.log("ERROR : " + e.getMessage());
            EventBus.getDefault().post(new ServeurConnectionFail(mClientAddress));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendFileNow(File file) {
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
            String command = Protocol.COMMAND_SEND_FILE + Protocol.SEPARATOR + file.getName() + Protocol.SEPARATOR + totalLength + Protocol.COMMAND_END;
            outputStream.write(command.getBytes());
            outputStream.flush();

            mLogger.log("sent command", command);

            long bytesWritten = 0;
            while ((c = is.read(buffer, 0, buffer.length)) > 0) {
                outputStream.write(buffer, 0, c);
                outputStream.flush();
                bytesWritten += c;
                mLogger.log("sent "+bytesWritten+ " bytes");
                Thread.sleep(1000);
            }

            mLogger.log("sent total of bytes", bytesWritten + "");
            is.close();
            EventBus.getDefault().post(new FileSentToClient(file));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        mFilesToSend.add(file);
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
