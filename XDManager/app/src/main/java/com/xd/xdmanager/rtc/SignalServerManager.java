package com.xd.xdmanager.rtc;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.xd.xdmanager.ChatActivity;
import com.xd.xdmanager.model.ListStatus;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SignalServerManager implements ISignalCallBack {
    private static final String TAG = "SignalServerManager";
    private Context mContext;

    private SignalServer mSignalServer;

    //ims回调
    private List<ImsCallback> mImsCallback = new ArrayList<>();
    //保存连接成功的客户端信息connect和对应的唯一标识（目前使用ip）
    private final Map<WebSocket, String> mConnectMaps = new HashMap<>();

    private IMSRetryHandler imsRetryHandler;
    public static volatile SignalServerManager INSTANCE;

    public static SignalServerManager INSTANCE(Context context) {
        if (INSTANCE == null) {
            synchronized (SignalServerManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SignalServerManager(context);
                }
            }
        }
        return INSTANCE;
    }

    private SignalServerManager(Context context) {
        mContext = context;
        HandlerThread imsRetry = new HandlerThread("ims_retry");
        imsRetry.start();
        imsRetryHandler = new IMSRetryHandler(imsRetry.getLooper(), this);
    }

    public void registerImsCallback(ImsCallback imsCallback) {
        synchronized (mImsCallback) {
            if (!mImsCallback.contains(imsCallback)) {
                mImsCallback.add(imsCallback);
                Log.e("ZFL", "registerImsCallback -->  " + mImsCallback.size() + "");
            }
        }
    }

    /**
     * 注销ims连接状态callback
     *
     * @param imsCallback
     */
    public void unRegisterImsConnectCallBack(ImsCallback imsCallback) {
        synchronized (mImsCallback) {
            if (mImsCallback.contains(imsCallback)) {
                mImsCallback.remove(imsCallback);
                Log.e("ZFL", "unRegisterImsConn -->  " + mImsCallback.size() + "");
            }
        }
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        unregister(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject jsonMessage = new JSONObject(message);
            String type = jsonMessage.getString("type");
            if (type.equals(MessageType.REGISTER.getId())) {
                registerUser(conn, message);
            } else if (type.equals(MessageType.OFFER.getId())) {
                onRemoteOfferReceived(jsonMessage);
            } else if (type.equals(MessageType.ANSWER.getId())) {
                onRemoteAnswerReceived(jsonMessage);
            } else if (type.equals(MessageType.ICE_CANDIDATE.getId())) {
                onRemoteCandidateReceived(jsonMessage);
            } else if (type.equals(MessageType.HANGUP.getId())) {
                String reason = jsonMessage.getString("reason");
                onHangup(reason);
            } else {
                Log.w("SignalServer", "the type is invalid: " + type);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (ex instanceof BindException) {
            Log.d("SignalServer", "bind port failed");
            Message message = imsRetryHandler.obtainMessage();
            imsRetryHandler.sendMessageDelayed(message, 5000);

        }
    }


//    public void stop() {
//        Log.d("SignalServer", "=== stop*************************" + this);
//        for (Map.Entry<WebSocket, String> entry : mConnectMaps.entrySet()) {
//            WebSocket conn = entry.getKey();
//            conn.close();
//        }
//        if (mSignalServer != null) {
//            try {
//                mSignalServer.stop();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void stop() {
        Log.d("SignalServer", "=== stop*************************" + this);
        // 关闭所有客户端连接
        for (Map.Entry<WebSocket, String> entry : mConnectMaps.entrySet()) {
            WebSocket conn = entry.getKey();
            conn.close(1000, "Service shutdown");
        }
        mConnectMaps.clear();

        // 停止服务器
        if (mSignalServer != null) {
            try {
                mSignalServer.stop(1000); // 添加超时时间
                mSignalServer = null; // 置空帮助GC
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 取消重试任务
        if (imsRetryHandler != null) {
            imsRetryHandler.removeCallbacksAndMessages(null);
        }
    }
//
//    public void start() {
//        Log.d("SignalServer", "=== start*************************" + this);
//        if (mSignalServer != null) {
//            try {
//                mSignalServer.stop();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        mSignalServer = new SignalServer(this);
//        mSignalServer.start();
//    }

    public void start() {
        Log.d("SignalServer", "=== start*************************" + this);
        try {
            // 先彻底停止旧服务
            stop();
            killPortOccupancyProcess();

            // 检查端口是否可用
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(8887), 1);
            ss.close();

            // 启动新服务
            mSignalServer = new SignalServer(this);
            mSignalServer.start();
        } catch (IOException e) {
            Log.w(TAG, "端口仍被占用，延迟重试");
            Message message = imsRetryHandler.obtainMessage();
            imsRetryHandler.sendMessageDelayed(message, 5000);
        }
    }

    // 在启动服务前调用此方法
    private void killPortOccupancyProcess() {
        // 1. 获取当前进程ID
        int currentPid = android.os.Process.myPid();

        // 2. 获取本应用所有运行中的进程
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();

        // 3. 遍历并终止非当前进程的实例
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.processName.equals(mContext.getPackageName()) && process.pid != currentPid) {
                android.os.Process.killProcess(process.pid);
                Log.d(TAG, "Killed residual process: " + process.pid);
            }
        }
    }

    /**
     * 获取所有当前连接的客户端列表
     * ip
     *
     * @return
     */
    public List<String> getClientList() {
        return new ArrayList<>(mConnectMaps.values());
    }

    /**
     * 通过clientid获取连接
     *
     * @param clientId
     * @return
     */
    public WebSocket getClientById(String clientId) {
        for (Map.Entry<WebSocket, String> entry : mConnectMaps.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 发送消息给学生端
     *
     * @param clientId
     * @param msg
     */
    public void sendMessage(String clientId, String msg) {
        WebSocket connect = getClientById(clientId);
        if (connect != null) {
            connect.send(msg);
        } else {
            Log.e("SignalServer", "sendMessage failed");
        }
    }

    /**
     * ims 重连handler
     */
    class IMSRetryHandler extends Handler {
        SignalServerManager mSignalServer;

        public IMSRetryHandler(Looper looper, SignalServerManager signalServerManager) {
            super(looper);
            mSignalServer = signalServerManager;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.w(TAG, "retry start ims server");
            mSignalServer.start();
        }
    }


    /**
     * 用户连接上信令服务
     *
     * @param conn
     * @param message
     * @throws JSONException
     */
    private void registerUser(WebSocket conn, String message) throws JSONException {
        if (mConnectMaps.containsKey(conn)) {
            mConnectMaps.remove(conn);
        }
        JSONObject jsonMessage = new JSONObject(message);
        String id = jsonMessage.getString("id");
        mConnectMaps.put(conn, id);
        //让ui刷新client list
        synchronized (mImsCallback) {
            for (ImsCallback callBack : mImsCallback) {
                callBack.refeshClent();
            }
        }
    }

    /**
     * client 断开与信令服务的连接
     *
     * @param conn
     */
    private void unregister(WebSocket conn) {
        if (mConnectMaps.containsKey(conn)) {
            mConnectMaps.remove(conn);
        }
        //让ui刷新client list
        synchronized (mImsCallback) {
            for (ImsCallback callBack : mImsCallback) {
                callBack.refeshClent();
            }
        }
    }

    // 发送方，收到学生端的answer
    private void onRemoteAnswerReceived(JSONObject message) {
        synchronized (mImsCallback) {
            for (ImsCallback callBack : mImsCallback) {
                callBack.onRemoteAnswerReceived(message);
            }
        }
    }

    private void onRemoteCandidateReceived(JSONObject message) {
        synchronized (mImsCallback) {
            for (ImsCallback callBack : mImsCallback) {
                callBack.onRemoteCandidateReceived(message);
            }
        }
    }

    private void onRemoteOfferReceived(JSONObject message) {
        try {
            String description = message.getString("sdp");
            String callFrom = message.getString("id");
            if (ListStatus.getInstance(mContext).getInCall()) {
                Logger.w("=== SignalServer onRemoteOfferReceived 接收到" + callFrom + "打来的电话，老师正在通话中。。。。。。");
                WebSocket connect = getClientById(callFrom);
                if (connect != null) {
                    JSONObject json_message = new JSONObject();
                    try {
                        json_message.put("type", MessageType.HANGUP.getId());
                        json_message.put("reason", "incalling");
                        connect.send(message.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
            ChatActivity.openActivity(mContext, false, callFrom, "laoshi", description, 0);
        } catch (JSONException e) {
            Logger.d("=== SignalServer onRemoteOfferReceived() e=" + e.getMessage());
        }
    }

    private void onHangup(String reason) {
        synchronized (mImsCallback) {
            for (ImsCallback callBack : mImsCallback) {
                callBack.onHangup(reason);
            }
        }
    }
}
