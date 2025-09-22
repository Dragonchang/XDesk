package com.xd.xdcontrol.net;

import android.os.Handler;
import android.os.Looper;
import android.serialport.SerialPort;
import android.util.Log;

import com.xd.xdcontrol.frame.Frame;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//测距传感器
public class RS485Controller {
    private static final int TIMEOUT = 100; // 单位ms
    private static final byte END_MARKER = 0x3B;

    private SerialPort serialPort;
    private OutputStream out;
    private InputStream in;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
    private volatile boolean isRunning = true;

    // 增加滤波相关成员变量
    private static final int DISTANCE_WINDOW_SIZE = 7;
    private final int[] distanceWindow = new int[DISTANCE_WINDOW_SIZE];
    private int distanceIndex = 0;
    private int validDataCount = 0;
    private float smoothedAmplitude = 0f;
    private static final float AMPLITUDE_SMOOTH_FACTOR = 0.3f;

    private static volatile RS485Controller instance;

    private RS485Controller() {
    }

    public static synchronized RS485Controller getInstance() {
        if (instance == null) {
            synchronized (RS485Controller.class) {
                if (instance == null) {
                    instance = new RS485Controller();
                }
            }
        }
        return instance;
    }

    private volatile boolean isConnected = false;

    public boolean isConnected() {
        return isConnected;
    }

    public void init() throws IOException {
        try {
            setSerialPortPermissions();

            serialPort = new SerialPort(new File("/dev/ttyS5"), 115200, 8, 1, 0);
            out = serialPort.getOutputStream();
            in = serialPort.getInputStream();
            isConnected = true;
            startReceiveThread();
            Log.i("RS485Controller", "测距传感器串口已连接");
        } catch (IOException e) {
            isConnected = false;
            Log.e("RS485Controller", "测距传感器串口连接失败: " + e.getMessage());
            throw e;
        }
    }

    private void startReceiveThread() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int len;
            ByteArrayOutputStream cache = new ByteArrayOutputStream();

            try {
                while (!Thread.interrupted()) {
                    if ((len = in.read(buffer)) > 0) {
                        cache.write(buffer, 0, len);
                        processRawData(cache.toByteArray(), len);
                        cache.reset();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private StringBuilder dataBuffer = new StringBuilder();

    private void processRawData(byte[] buffer, int length) {
        String newData = new String(buffer, 0, length);
        dataBuffer.append(newData);

        // 处理所有完整数据帧（以\r\n分隔）
        while (true) {
            int endIndex = dataBuffer.indexOf("\r\n");
            if (endIndex == -1) break;

            String frame = dataBuffer.substring(0, endIndex);
            dataBuffer.delete(0, endIndex + 2); // 移除已处理数据

            parseSensorData(frame);
        }
    }

    private void parseSensorData(String frame) {
        if ("0".equals(frame)) {
            return;
        }
        Pattern pattern = Pattern.compile("Range:\\s*(\\d+).*Amplitude:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(frame);

        if (matcher.find()) {
            try {
                int distance = Integer.parseInt(matcher.group(1));
                int amplitude = Integer.parseInt(matcher.group(2));

                // 距离滑动平均滤波
                int filteredDistance = smoothDistance(distance);

                // 幅度简单指数滤波
                int filteredAmplitude = smoothAmplitude(amplitude);

                // 使用滤波后的数据（示例：记录日志）
                Log.d("Filtered", "Distance: " + filteredDistance + "mm (" + distance + "), " + "Amplitude: " + filteredAmplitude + " (" + amplitude + ")");

                // 主线程回调示例
                if (shouldTriggerStop(filteredDistance, filteredAmplitude)) {
                    Frame.HANDLES.sentAll(10004, 1);
                }
            } catch (NumberFormatException e) {
                Log.w("TAG", "数据格式异常: " + frame);
            }
        }
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public void close() {
        try {
            if (serialPort != null) {
                serialPort.close();
            }
        } catch (Exception e) {
            Log.e("Sensor", "释放资源异常: " + e.getMessage());
        }
    }

    // 距离滑动平均实现
    private int smoothDistance(int newDistance) {
        // 更新数据窗口
        distanceWindow[distanceIndex % DISTANCE_WINDOW_SIZE] = newDistance;
        distanceIndex++;
        validDataCount = Math.min(validDataCount + 1, DISTANCE_WINDOW_SIZE);

        // 计算窗口平均值
        int sum = 0;
        for (int i = 0; i < validDataCount; i++) {
            sum += distanceWindow[i];
        }
        return sum / validDataCount;
    }

    // 幅度指数平滑滤波
    private int smoothAmplitude(int newAmplitude) {
        if (smoothedAmplitude == 0) {
            smoothedAmplitude = newAmplitude;
        } else {
            smoothedAmplitude = AMPLITUDE_SMOOTH_FACTOR * newAmplitude + (1 - AMPLITUDE_SMOOTH_FACTOR) * smoothedAmplitude;
        }
        return (int) smoothedAmplitude;
    }

    // 触发条件判断（示例）
    private boolean shouldTriggerStop(int distance, int amplitude) {
        // 简单条件：距离<200mm 且振幅>500
        return distance < 200 && amplitude > 500;
    }

    private void setSerialPortPermissions() {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
//            process = Runtime.getRuntime().exec("/system/bin/su");
            os = new DataOutputStream(process.getOutputStream());

            os.writeBytes("sh -c 'chmod 777 /dev/ttyS*'\n");
            os.writeBytes("exit\n");
            os.flush();

//            os.writeBytes("su\n");
//            os.writeBytes("chmod 777 /dev/ttyS*\n");
//            os.writeBytes("exit\n");
//            os.flush();

            int exitCode = process.waitFor();
            Log.d("Root", "Exit code: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Root", "Permission setting failed: " + e.getMessage());
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}