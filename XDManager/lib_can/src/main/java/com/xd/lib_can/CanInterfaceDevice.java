package com.xd.lib_can;


import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CanInterfaceDevice {
    private static String m_InterfaceName = "can0";
    private static long m_Bitrate = 500000;

    //can接口文件描述符
    private int m_FD;
    private android_socketcan m_AndroidSocketcan = new android_socketcan();
    private InterfaceStatus m_InterfaceStatus = InterfaceStatus.INTERFACE_STATUS_NONE;

    private BlockingQueue<CanMessage> mQueue = new LinkedBlockingQueue<>();
    private SendThread mSendThread = new SendThread();
    private ReceiveThread mReceiveThread = new ReceiveThread();
    private Boolean mRunning = false;

    List<ICanMessageReceiveCallBack> mMessageCallbacks = new ArrayList<>();

    public static volatile CanInterfaceDevice INSTANCE;

    public static CanInterfaceDevice INSTANCE() {
        if (INSTANCE == null) {
            synchronized (CanInterfaceDevice.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CanInterfaceDevice();
                }
            }
        }
        return INSTANCE;
    }

    private CanInterfaceDevice() {
        int fd = OpenCanInterface();
        if(fd < 0) {
            Log.w("CanInterfaceDevice", "OpenCanInterface failed with fd: " + fd);

        }
        mSendThread.start();
        mReceiveThread.start();
        mRunning = true;
    }

    /***
     * 启动can interface
     */
    private void UpCanInterface()
    {

    }

    private void DownCanInterface()
    {

    }

    private InterfaceStatus getInterfaceStatus()
    {
        return InterfaceStatus.INTERFACE_STATUS_DOWN;
    }

    private int OpenCanInterface()
    {
        m_FD = m_AndroidSocketcan.socketcanOpen(m_InterfaceName);
        Log.i("CanInterfaceDevice", "OpenCanInterface fd: " + m_FD);
        return m_FD;
    }

    public void sendMessage(CanMessage msg) {
        if (msg != null) {
            mQueue.offer(msg);
        } else {
            Log.e("CanInterfaceDevice", "sendMessage failed:" + msg.toString());
        }
    }

    class SendThread extends Thread {
        @Override
        public void run() {
            while (mRunning) {
                try {
                    CanMessage queue = mQueue.take();
                    sendMessageImp(queue);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMessageImp(CanMessage msg) {
        int ret = m_AndroidSocketcan.socketcanWrite(m_FD, msg.canid, msg.eff, msg.rtr, msg.len, msg.data);
        Log.i("CanInterfaceDevice", "sendMessageImp with ret: " + ret);
    }

    class ReceiveThread extends Thread {
        @Override
        public void run() {
            while (mRunning) {

            }
        }
    }
}
