package com.xd.xdcontrol.net;

import android.os.Handler;
import android.os.Looper;
import android.serialport.SerialPort;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//电源板
public class PowerController {
    private static final int TIMEOUT = 100; // 单位ms
    private static final byte END_MARKER = 0x3B;

    private SerialPort serialPort;
    private OutputStream out;
    private InputStream in;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
    private volatile boolean isRunning = true;

    private static volatile PowerController instance;

    private PowerController() {
    }

    public static synchronized PowerController getInstance() {
        if (instance == null) {
            synchronized (PowerController.class) {
                if (instance == null) {
                    instance = new PowerController();
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
            serialPort = new SerialPort(new File("/dev/ttyS3"), 9600, 8, 1, 0);
            out = serialPort.getOutputStream();
            in = serialPort.getInputStream();
            isConnected = true;
            startReceiveThread();
            Log.i("PowerController", "电源控制串口已连接");
        } catch (Exception e) {
            isConnected = false;
            Log.e("PowerController", "串口连接失败: " + e.getMessage());
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
//                        processRawData(cache.toByteArray(), len);
                        cache.reset();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void adjustDC(boolean isOn, int integerPart, int decimalPart) {
        byte[] command = new byte[7];
        command[0] = 0x12;
        command[1] = 0x01;
        command[2] = 0x00;
        command[3] = 0x00;
        command[4] = (byte) (isOn ? 0x55 : 0xAA); // 开关状态
        command[5] = (byte) integerPart;
        command[6] = (byte) decimalPart;
        sendCommand(command);
    }

    public void adjustAC(boolean isOn, int voltage) {
        byte[] command = new byte[7];
        command[0] = 0x12;
        command[1] = 0x02;
        command[2] = 0x00;
        command[3] = 0x00;
        command[4] = (byte) (isOn ? 0x55 : 0xAA); // 开关状态
        command[5] = (byte) voltage;      // 电压值
        command[6] = 0x00;
        sendCommand(command);
    }

    public void adjustHighAC(boolean isOn) {
        byte[] command = new byte[7];
        command[0] = 0x12;
        command[1] = 0x03;
        command[2] = 0x00;
        command[3] = 0x00;
        command[4] = (byte) (isOn ? 0x55 : 0xAA); // 开关状态
        command[5] = 0x00;
        command[6] = 0x00;
        sendCommand(command);
    }

    private final Object sendLock = new Object();

    private void sendCommand(byte[] command) {
        synchronized (sendLock) {
            try {
                if (out != null) {
                    Log.e("PowerController", "power send: " + bytesToHex(command));
                    out.write(command);
                    out.flush();
                    Log.d("PowerController", "指令发送成功: " + bytesToHex(command));
                } else {
                    Log.e("PowerController", "输出流未初始化");
                }
            } catch (IOException e) {
                Log.e("PowerController", "发送指令失败", e);
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
}