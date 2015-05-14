package net.microtrash.wisperingtree.bluetooth.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import net.microtrash.wisperingtree.bluetooth.mananger.BluetoothManager;
import net.microtrash.wisperingtree.bus.BluetoothCommunicator;
import net.microtrash.wisperingtree.bus.ClientConnectionFail;
import net.microtrash.wisperingtree.bus.ClientConnectionSuccess;
import net.microtrash.wisperingtree.bus.ProgressStatusChange;
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
    private boolean mReceiveCommand = false;

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

            EventBus.getDefault().post(new ClientConnectionSuccess());

            EventBus bus = EventBus.getDefault();
            char ch;

            while (mRunning) {

                String command = "";
                if (!mReceiveFile) {

                    while (mRunning) {
                        ch = (char) mInputStream.read();

                        if (ch == Protocol.COMMAND_START.charAt(0)) {
                            mReceiveCommand = true;
                        }
                        if (mReceiveCommand) {
                            //mLogger.log("receiving byte", ""+new String(stringBuffer, 0, bytesRead)+" command end:"+);
                            if (!checkCommand(command)) {
                                command = "";
                                mReceiveCommand = false;
                                mLogger.log("Invalid command: \"" + command + "\"");
                                EventBus.getDefault().post(new ClientConnectionFail());
                                return;
                            }

                            if (ch == Protocol.COMMAND_END) {
                                mReceiveCommand = false;
                                // parse command
                                mLogger.log("command complete: " + String.valueOf(ch));
                                break;
                            } else {
                                command += ch;
                            }

                        }
                    }

                } else {
                    mLogger.log("receiving file", mReceiveFilename);

                    int c = 0;
                    long bRead = 0;
                    final String filePath = Utils.getAppRootDir() + "/" + mReceiveFilename;
                    OutputStream oos = new FileOutputStream(filePath);

                    while (mRunning && bRead < mReceiveFileLength && (c = mInputStream.read(dataBuffer, 0, dataBuffer.length)) > 0) {
                        if ((bRead + bufferSize) >= mReceiveFileLength) {
                            c = (int) (mReceiveFileLength - bRead);
                        }
                        oos.write(dataBuffer, 0, c);
                        oos.flush();
                        bRead += c;
                        bus.post(new ProgressStatusChange((float) bRead / (float) mReceiveFileLength, mReceiveFilename + " / " + bRead + " bytes"));
                        Thread.sleep(Protocol.TRANSFER_DELAY_MS);
                    }
                    oos.close();
                    mLogger.log("read " + bRead + " bytes");
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
                mLogger.log("check command: " + command);
                if (command.startsWith(Protocol.COMMAND_START + Protocol.COMMAND_SEND_FILE)) {
                    try {
                        mLogger.log("command: ", command);
                        String[] commandArray = command.split(Protocol.SEPARATOR);
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

    private boolean checkCommand(String command) {
        if (command.length() > 500) {
            //mLogger.log("command too long: " + command.length());
            return false;
        }
        if (command.length() >= Protocol.COMMAND_START.length()){
            //mLogger.log("command long" + command.length());
            if(!command.startsWith(Protocol.COMMAND_START)) {
                //mLogger.log("command not valid: " + command);
                return false;
            }
        }
        return true;
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
