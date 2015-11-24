/*
*    Light Controller, to Control wifi LED Lighting
*    Copyright (C) 2014  Eliot Stocker
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package tv.piratemedia.lightcontroler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class UDPConnection {
    private static final String TAG = "UDPConnection";
    public static String CONTROLLERIP = "";
    public static int CONTROLLERPORT = 0;
    public static int CONTROLLERADMINPORT = 48899;
    private AsyncTask<Void, Void, Void> mPostMessageTask;
    private utils Utils;
    private UDP_Server server = null;
    private SharedPreferences prefs;
    private static Context mCtx;
    private static Handler mHandler;
    private String NetworkBroadCast;

    private LinkedList<PostMessage> mMessageQueue = new LinkedList<>();
    private boolean onlineMode = false;
    private long mLastMessageSent = 0;
    private boolean mRunSender = true;

    private DatagramSocket mDatagramSocket;

    public UDPConnection(Context context, Handler handler) {
        mCtx = context;
        mHandler = handler;
        Utils = new utils(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        NetworkBroadCast = "192.168.0.255";
        try {
            NetworkBroadCast = Utils.getWifiIP(utils.BROADCAST_ADDRESS);
        } catch (ConnectionException e) {
            e.printStackTrace();
            return;
        }

        mPostMessageTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                while (mRunSender) {
                    if (mMessageQueue.size() > 0) {
                        PostMessage message = mMessageQueue.removeFirst();
                        sendMessageNow(message.getData());
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                return null;
            }
        };
        if (Build.VERSION.SDK_INT >= 11) {
            mPostMessageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            mPostMessageTask.execute();
        }
    }

    public void setOnlineMode(boolean online) {
        onlineMode = online;
    }

    public void sendMessage(final byte[] bytes) throws IOException {
        Log.v(TAG, "queuing message...");
        mMessageQueue.add(new PostMessage(bytes));
    }


    private void sendMessageNow(byte[] bytes) {

        CONTROLLERIP = prefs.getString("pref_light_controller_ip", NetworkBroadCast);
        CONTROLLERPORT = Integer.parseInt(prefs.getString("pref_light_controller_port", "8899"));

        try {
            InetAddress controller = InetAddress.getByName(CONTROLLERIP);
            DatagramPacket p = new DatagramPacket(bytes, 3, controller, CONTROLLERPORT);
            getDatagramSocket().send(p);
            Log.v(TAG, "sent light command: " + String.valueOf(bytes));
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramSocket getDatagramSocket() {
        if (mDatagramSocket == null) {
            try {
                mDatagramSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return mDatagramSocket;
    }

    public void sendAdminMessage(byte[] Bytes) throws IOException {
        sendAdminMessage(Bytes, false);
    }

    public void sendAdminMessage(byte[] Bytes, Boolean Device) throws IOException {
        if (server == null) {
            server = new UDP_Server();
            server.runUdpServer();
        } else if (!server.Server_aktiv) {
            server.runUdpServer();
        }

        String NetworkBroadCast = null;
        if (Device) {
            CONTROLLERIP = prefs.getString("pref_light_controller_ip", "192.168.0.255");
            NetworkBroadCast = CONTROLLERIP;
        } else {
            try {
                NetworkBroadCast = Utils.getWifiIP(utils.BROADCAST_ADDRESS);
            } catch (ConnectionException e) {
                e.printStackTrace();
                return;
            }
        }
        DatagramSocket s = new DatagramSocket();
        InetAddress controller = InetAddress.getByName(NetworkBroadCast);
        DatagramPacket p = new DatagramPacket(Bytes, Bytes.length, controller, CONTROLLERADMINPORT);
        s.setBroadcast(true);
        s.send(p);
    }

    public void destroyUDPC() {
        Log.d("controller", "destroy");
        if (server != null) {
            server.stop_UDP_Server();
        }
        if (mPostMessageTask != null) {
            mPostMessageTask.cancel(true);
        }
    }

    class UDP_Server {
        private AsyncTask<Void, Void, Void> async;
        public boolean Server_aktiv = true;

        @SuppressLint("NewApi")
        public void runUdpServer() {
            Server_aktiv = true;
            async = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    byte[] lMsg = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
                    DatagramSocket ds = null;

                    try {
                        ds = new DatagramSocket(UDPConnection.CONTROLLERADMINPORT);
                        ds.setSoTimeout(1000);
                        while (Server_aktiv) {
                            try {
                                ds.receive(dp);
                                String Data = new String(dp.getData());
                                if (Data.startsWith("+ok")) {
                                    if (Data.startsWith("+ok=")) {
                                        Message m = new Message();
                                        m.what = LightsController.LIST_WIFI_NETWORKS;
                                        m.obj = Data;
                                        mHandler.sendMessage(m);
                                        Server_aktiv = false;
                                    } else {
                                        Message m = new Message();
                                        m.what = LightsController.COMMAND_SUCCESS;
                                        mHandler.sendMessage(m);
                                        Server_aktiv = false;
                                    }
                                } else {
                                    String[] parts = Data.split(",");
                                    if (parts.length > 1) {
                                        if (Utils.validIP(parts[0]) && Utils.validMac(parts[1])) {
                                            Message m = new Message();
                                            m.what = LightsController.DISCOVERED_DEVICE;
                                            m.obj = parts;
                                            mHandler.sendMessage(m);
                                            Server_aktiv = false;
                                        }
                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                //no problem
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (ds != null) {
                            ds.close();
                        }
                    }

                    return null;
                }
            };

            if (Build.VERSION.SDK_INT >= 11)
                async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else async.execute();
        }

        public void stop_UDP_Server() {
            Server_aktiv = false;
        }
    }

    public class PostMessage {
        byte[] mData;

        public PostMessage(byte[] bytes) {
            this.mData = bytes;
        }

        public byte[] getData() {
            return mData;
        }
    }
}
