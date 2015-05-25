package net.microtrash.wisperingtree.bluetooth.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import net.microtrash.wisperingtree.bus.ServerConnectionFail;
import net.microtrash.wisperingtree.bus.ServerConnectionSuccess;

import net.microtrash.wisperingtree.bus.FileSentToClient;
import net.microtrash.wisperingtree.bus.FileSentToClientFail;
import net.microtrash.wisperingtree.bus.ProgressStatusChange;
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

            EventBus.getDefault().post(new ServerConnectionSuccess(mClientAddress));

            while (mRunning) {
                if (mFilesToSend.size() > 0) {
                    File nextFile = mFilesToSend.getFirst();
                    sendFileNow(nextFile);
                    EventBus.getDefault().post(new FileSentToClient(nextFile));
                    mFilesToSend.removeFirst();
                }
                Thread.sleep(500);
            }
        } catch (IOException e) {
            mRunning = false;
            mLogger.log("ERROR: " + e.getMessage());
            EventBus.getDefault().post(new ServerConnectionFail(mClientAddress));
            EventBus.getDefault().post(new FileSentToClientFail());
        } catch (InterruptedException e) {
            mLogger.log("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendFileNow(File file) throws IOException, InterruptedException {
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

        EventBus bus = EventBus.getDefault();

        outputStream = mSocket.getOutputStream();
        long totalLength = file.length();
        String filename = file.getName();
        String command = Protocol.COMMAND_START + Protocol.COMMAND_SEND_FILE + Protocol.SEPARATOR + filename + Protocol.SEPARATOR + totalLength + Protocol.COMMAND_END;
        outputStream.write(command.getBytes());
        outputStream.flush();

        mLogger.log("sent command", command);

        long bytesWritten = 0;
        while ((c = is.read(buffer, 0, buffer.length)) > 0) {
            outputStream.write(buffer, 0, c);
            outputStream.flush();
            bytesWritten += c;
            bus.post(new ProgressStatusChange((float) bytesWritten / (float) totalLength, filename + " / " + bytesWritten + " bytes"));
            Thread.sleep(Protocol.TRANSFER_DELAY_MS);
        }

        mLogger.log("sent total of bytes", bytesWritten + "");
        is.close();
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
