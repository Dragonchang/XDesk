package com.xd.lib_can;


import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CanInterfaceDevice {
    private static String m_InterfaceName = "can0";
    private static long m_Bitrate = 500000;

    //can接口文件描述符
    private int m_FD;
    private android_socketcan m_AndroidSocketcan = new android_socketcan();
    private InterfaceStatus m_InterfaceStatus = InterfaceStatus.INTERFACE_STATUS_DOWN;

    private BlockingQueue<CanMessage> mQueue = new LinkedBlockingQueue<>();
    private SendThread mSendThread = new SendThread();
    private ReceiveThread mReceiveThread;
    private Boolean mRunning = false;

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
        return -1;
    }

    public int WriteData() {
        return -1;
    }
    public void sendMessage(CanMessage msg) {
        if (msg != null) {
            mQueue.offer(msg);
        } else {
            Log.e("CanInterfaceDevice", "sendMessage failed:" + msg.toString());
        }
    }
    public long[] ReadData()
    {
        return new long[10];
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

    }

    class ReceiveThread extends Thread {
        @Override
        public void run() {
            while (mRunning) {

            }
        }
    }
}
