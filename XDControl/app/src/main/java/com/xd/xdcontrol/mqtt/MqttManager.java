package com.xd.xdcontrol.mqtt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.xd.xdcontrol.config.Const;
import com.xd.xdcontrol.frame.Frame;
import com.xd.xdcontrol.model.MasterOrder;
import com.xd.xdcontrol.net.StatusParser;
import com.xd.xdcontrol.net.StatusProtocol;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MqttManager {
    private static final String TAG = "MqttManager";
    private static MqttManager instance;

    // MQTT配置参数
    private String serverUri = "tcp://" + Const.DEFAULT_IP + ":1883";
    private String clientId;
    private String username = "username";
    private String password = "password";
    private int keepAlive = 60;
    private int connectionTimeout = 30;

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private MqttAndroidClient mqttClient;
    private MqttConnectOptions connectOptions;
    private final List<String> subscriptionTopics = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isConnected = false;
    private int reportInterval = 2000; // 默认2秒上报

    // 状态上报Runnable
    private final Runnable statusReportRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnected) {
                publishStatus();
                handler.postDelayed(this, reportInterval);
            }
        }
    };

    // 连接状态监听
    public interface ConnectionListener {
        void onConnected();

        void onDisconnected();

        void onError(Throwable error);
    }

    // 消息接收监听
    public interface MessageListener {
        void onMessageReceived(String topic, MqttMessage message);
    }

    //    private ConnectionListener connectionListener;
//    private MessageListener messageListener;

    private MqttManager(Context context, String clientid_, String username_) {
//        clientId = UUID.randomUUID().toString();
        this.username = username_;
        this.clientId = clientid_;
        initializeClient(context);
    }

    public static synchronized MqttManager getInstance(Context context, String clientid_, String username_) {
        if (instance == null) {
            instance = new MqttManager(context.getApplicationContext(), clientid_, username_);
        }
        return instance;
    }

    private void initializeClient(Context context) {
        mqttClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d(TAG, "Connection " + (reconnect ? "reestablished" : "established"));
                isConnected = true;
                Frame.HANDLES.sentAll(90004, 1);
                // 重新订阅主题
                resubscribeToTopics();
                // 启动状态上报
                startStatusReporting();
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.w(TAG, "Connection lost", cause);
                isConnected = false;
                Frame.HANDLES.sentAll(90004, 0);
                stopStatusReporting();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
//                String payload = new String(message.getPayload());
                Log.d(TAG, "Message received: " + topic + " - " + message.toString());
                handleCommand(message.getPayload());
//                if (messageListener != null) {
//                    handler.post(() -> messageListener.onMessageReceived(topic, message));
//                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 消息投递完成处理
            }
        });

        setupConnectOptions();
    }

    private void setupConnectOptions() {
        connectOptions = new MqttConnectOptions();
        connectOptions.setUserName(username);
        connectOptions.setPassword(password.toCharArray());
        connectOptions.setCleanSession(false);
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setConnectionTimeout(connectionTimeout);
        connectOptions.setKeepAliveInterval(keepAlive);
    }

    public void connect() {
        if (isConnected) return;

        try {
            mqttClient.connect(connectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Connect success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connect failed", exception);
                    Frame.HANDLES.sentAll(90004, 3);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Connect exception", e);
        }
    }

    public void disconnect() {
        try {
            mqttClient.disconnect().setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    isConnected = false;
                    Log.d(TAG, "Disconnect success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Disconnect failed", exception);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Disconnect exception", e);
        }
    }

    private void resubscribeToTopics() {
        for (String topic : subscriptionTopics) {
            subscribe(topic);
        }
    }

    public void subscribe(String topic) {
        if (mqttClient == null || !isConnected) {
            Log.e(TAG, "Subscribe failed: client not ready");
            return;
        }
        if (!subscriptionTopics.contains(topic)) {
            subscriptionTopics.add(topic);
        }

        try {
            mqttClient.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Subscribe failed: " + topic, exception);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Subscribe exception: " + topic, e);
        }
    }

    private void startStatusReporting() {
        stopStatusReporting();
        handler.postDelayed(statusReportRunnable, reportInterval);
    }

    private void stopStatusReporting() {
        handler.removeCallbacks(statusReportRunnable);
    }

    private void publishStatus() {
        publish("client/status", StatusProtocol.buildStatusPacket(StatusParser.getInstance()));
    }

    public void publish(String topic, byte[] data) {
        if (!isConnected) return;

        try {
            MqttMessage mqttMessage = new MqttMessage(data);
            mqttMessage.setQos(0);
            mqttMessage.setRetained(false);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            Log.e(TAG, "Publish failed: " + topic, e);
        }
    }

    public void publish(String topic, String message) {
        if (!isConnected) return;

        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttMessage.setRetained(false);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            Log.e(TAG, "Publish failed: " + topic, e);
        }
    }

    // 设置监听器
//    public void setConnectionListener(ConnectionListener listener) {
//        this.connectionListener = listener;
//    }

//    public void setMessageListener(MessageListener listener) {
//        this.messageListener = listener;
//    }

    // 配置方法
    public void setReportInterval(int intervalMillis) {
        this.reportInterval = intervalMillis;
    }

    public void setServerUri(String uri) {
        this.serverUri = uri;
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        setupConnectOptions();
    }

    private static int crc16CCITT(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc = (crc >>> 8) ^ Crc16Java.CRC16_TABLE[(crc ^ (b & 0xFF)) & 0xFF];
        }
        return crc & 0xFFFF;
    }

    private void handleCommand(byte[] data) {
        try {
            if (data.length != 8) {
                return;
            }

            int type = data[0] & 0xFF;
            int switchValue = data[1] & 0xFF;
            byte[] valueBytes = Arrays.copyOfRange(data, 2, 6);
            int receivedCrc = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);

            // CRC 校验
            byte[] crcData = Arrays.copyOfRange(data, 0, 6);
            int calculatedCrc = crc16CCITT(crcData);
            if (calculatedCrc != receivedCrc) {
                Log.e(TAG, "CRC check failed: " + calculatedCrc + " vs " + receivedCrc);
                return;
            }

            // 转换数值
            float value = ByteBuffer.wrap(valueBytes)
                    .order(ByteOrder.BIG_ENDIAN)
                    .getFloat();
            Log.d(TAG, "type: " + type + " switch: " + switchValue + " value: " + value);

            if (type < 1 || type > 40) return;
            if (switchValue < 0 || switchValue > 1) return;
            if (value < 0 || value > 50) return;

            Frame.HANDLES.sentAll(80000 + type, new Gson().toJson(new MasterOrder(switchValue, value)));
        } catch (Exception e) {
            Log.e(TAG, "Command processing failed", e);
        }
    }
}
