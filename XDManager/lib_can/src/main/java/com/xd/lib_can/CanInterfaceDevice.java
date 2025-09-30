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
            UpCanInterface();
        }
        mSendThread.start();
        mReceiveThread.start();
        mRunning = true;
    }

    /***
     * 启动can interface
     */
    public void UpCanInterface()
    {
        String[] canConfigCmd = {"ip link set can0 type can bitrate 500000 dbitrate 2000000 fd on"};
        String result = ShellUtil.execCommand(canConfigCmd, true, true);
        Log.i("CanInterfaceDevice", "UpCanInterface result: " + result);
        String[] canUpCmd = {"ip link set can0 up"};
        result = ShellUtil.execCommand(canUpCmd, true, true);
        Log.i("CanInterfaceDevice", "UpCanInterface result: " + result);
    }

    public InterfaceStatus getInterfaceStatus()
    {
        String[] canInfoCmd = {"ip link show | grep can | grep \"state\""};
        String canInfo = ShellUtil.execCommand(canInfoCmd, true, true);
        if(canInfo.isEmpty())
        {
            return InterfaceStatus.INTERFACE_STATUS_NONE;
        }
        Log.i("CanInterfaceDevice", "getInterfaceStatus canInfo: " + canInfo);
        String canStatusCmd = "echo \"" +canInfo +"\" | awk '{for(i=1;i<=NF;i++) if($i==\"state\") print $(i+1)}'";
        Log.i("", "canStatusCmd: " + canStatusCmd);
        String[] canStatusCmds = {canStatusCmd};
        String canStatus = ShellUtil.execCommand(canStatusCmds, true, true);
        if(canStatus.isEmpty())
        {
            return InterfaceStatus.INTERFACE_STATUS_NONE;
        }
        Log.i("CanInterfaceDevice", "getInterfaceStatus canStatus: " + canStatus);
        if(canStatus.equals("UP")){
            return InterfaceStatus.INTERFACE_STATUS_UP;
        } else if(canStatus.equals("DOWN")){
            return InterfaceStatus.INTERFACE_STATUS_DOWN;
        } else {
            return InterfaceStatus.INTERFACE_STATUS_NONE;
        }
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

    public void registerReceiveCallBack(ICanMessageReceiveCallBack callBack) {
        synchronized (mMessageCallbacks) {
            if (callBack != null && !mMessageCallbacks.contains(callBack)) {
                mMessageCallbacks.add(callBack);
                Log.i("CanInterfaceDevice", "registerReceiveCallBack callBack: " + callBack+ " size: " + mMessageCallbacks.size());
            }
        }
    }

    public void unregisterReceiveCallback(ICanMessageReceiveCallBack callBack) {
        if (callBack == null) return;
        synchronized (mMessageCallbacks) {
            mMessageCallbacks.remove(callBack);
            Log.i("CanInterfaceDevice", "unregisterReceiveCallback callBack: " + callBack+ " size: " + mMessageCallbacks.size());
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
        int ret = m_AndroidSocketcan.socketcanSend(m_FD, msg.m_canid, msg.m_eff, msg.m_rtr, msg.m_len, msg.m_data);
        Log.i("CanInterfaceDevice", "sendMessageImp with ret: " + ret);
    }

    class ReceiveThread extends Thread {
        @Override
        public void run() {
            byte[] data = new byte[8];
            while (mRunning) {
                byte[] ret = m_AndroidSocketcan.socketcanReceive(m_FD);
                if(ret != null && ret.length == 12 ) {
                    System.arraycopy(ret, 4, data, 0, 8);
                    CanMessage msg = new CanMessage(Util.byteToLong(ret, 0, 4), data);
                    Log.i("CanInterfaceDevice", "ReceiveThread with msg: "+ msg.toString());
                    synchronized (mMessageCallbacks) {
                        for (ICanMessageReceiveCallBack callback : mMessageCallbacks) {
                            callback.onMessage(msg);
                        }
                    }
                } else {
                    if(ret == null) {
                        Log.i("CanInterfaceDevice", "ReceiveThread with null data");
                    } else {
                        Log.i("CanInterfaceDevice", "ReceiveThread with error data length: "+ret.length);
                    }
                }
            }
        }
    }
}
