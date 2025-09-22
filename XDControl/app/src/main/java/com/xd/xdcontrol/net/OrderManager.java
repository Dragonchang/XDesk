package com.xd.xdcontrol.net;

import static com.xd.xdcontrol.config.Const.DEFAULT_RATE;
import static com.xd.xdcontrol.config.Const.DEFAULT_SERIAL_NAME;

import android.serialport.SerialPort;
import android.util.Log;

import com.xd.xdcontrol.frame.Frame;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//控制板
public class OrderManager {
    //TODO
    private static final int ORDER_INTERVAL = 200; // 指令发送间隔 单位ms
    private static final byte END_MARKER = 0x3B;
    private static final byte START_MARKER = 0x55;
    private static final byte[][] HEARTBEAT_COMMANDS = new byte[3][]; // 初始化时填充心跳指令
    private static final long HEARTBEAT_TIMEOUT = 8000; // 8秒无响应判定断开
    private volatile boolean isConnected = false;
    private long lastResponseTime = 0;
    private static final int CHECK_CONNECT_INTERVAL = 5000; // 检查串口连接状态间隔 单位ms

    private SerialPort serialPort;
    private OutputStream out;
    private InputStream in;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
    private volatile boolean isRunning = true;

    // 指令队列（特殊指令优先）
    private final BlockingQueue<byte[]> commandQueue = new LinkedBlockingQueue<>();
    private int heartbeatIndex = 0;

    private static volatile OrderManager instance;

    private OrderManager() {
    }

    public static synchronized OrderManager getInstance() {
        if (instance == null) {
            synchronized (OrderManager.class) {
                if (instance == null) {
                    instance = new OrderManager();
                }
            }
        }
        return instance;
    }

    // 初始化串口
    public void init() throws IOException {
        serialPort = new SerialPort(new File(DEFAULT_SERIAL_NAME), DEFAULT_RATE, 8, 1, 0);
        out = serialPort.getOutputStream();
        in = serialPort.getInputStream();

        isConnected = true;
        lastResponseTime = System.currentTimeMillis();
        Frame.HANDLES.sentAll(90001, 1);
        Log.i("OrderManager", "控制板串口已连接");
        HEARTBEAT_COMMANDS[0] = buildCommand((byte) 0x20, (byte) 0x00, (byte) 0x00);
        HEARTBEAT_COMMANDS[1] = buildCommand((byte) 0x21, (byte) 0x00, (byte) 0x00);
        HEARTBEAT_COMMANDS[2] = buildCommand((byte) 0x22, (byte) 0x00, (byte) 0x00);

        startReceiveThread();
        startSendThread();
        startConnectionMonitor();
    }

    private void startConnectionMonitor() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(CHECK_CONNECT_INTERVAL);
                    checkConnectionStatus();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void checkConnectionStatus() {
        boolean shouldConnect = System.currentTimeMillis() - lastResponseTime < HEARTBEAT_TIMEOUT;
        if (isConnected != shouldConnect) {
            isConnected = shouldConnect;
            Frame.HANDLES.sentAll(90001, shouldConnect ? 1 : 0);

            if (!isConnected) {
                attemptReconnect();
            }
        }

    }

    // 外部获取连接状态
    public boolean isConnected() {
        return isConnected;
    }

    private void startSendThread() {
        new Thread(() -> {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    // 1. 优先获取特殊指令（非阻塞）
                    byte[] cmd = commandQueue.poll();

                    // 2. 无特殊指令时使用心跳指令
                    if (cmd == null) {
                        cmd = HEARTBEAT_COMMANDS[heartbeatIndex];
                        heartbeatIndex = (heartbeatIndex + 1) % HEARTBEAT_COMMANDS.length;
                    }

                    // 3. 发送指令并等待间隔
                    sendImmediately(cmd);
                    Thread.sleep(ORDER_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    Log.e("OrderManager", "Send error", e);
                }
            }
        }).start();
    }

    private void attemptReconnect() {
        new Thread(() -> {
            Log.e("OrderManager", "Reconnect start");
            int retryCount = 0;
            while (retryCount < 3 && isRunning) {
                try {
                    closeResources();
//                    init();
                    if (isConnected) break;
                    Thread.sleep(5000);
                    retryCount++;
                } catch (Exception e) {
                    Log.e("OrderManager", "Reconnect failed: " + e.getMessage());
                }
            }
        }).start();
    }

    private void startReceiveThread() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isRunning) {
                try {
                    int len = in.read(buffer);
                    if (len > 0 && len <= 1024) {
                        byte[] received = new byte[len];
                        System.arraycopy(buffer, 0, received, 0, len);
                        processReceivedData(received, len);
                    }
                } catch (IOException e) {
                    if (isRunning) Log.e("OrderManager", "Receive error", e);
                }
            }
        }).start();
    }

    // 核心发送方法（含校验生成）
    private byte[] buildCommand(byte cmdType, byte dataHigh, byte dataLow) {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.put((byte) 0x55);    // 协议头1
        buffer.put((byte) 0xAA);    // 协议头2
        buffer.put(cmdType);        // 命令类型
        buffer.put(dataHigh);       // 数据高字节
        buffer.put(dataLow);        // 数据低字节

        // 计算校验位（前5字节求和）
        byte checksum = 0;
        for (int i = 0; i < 5; i++) {
            checksum += buffer.get(i);
        }
        buffer.put(checksum);
        buffer.put(END_MARKER);     // 结束符

        return buffer.array();
    }

    public void sendCommand(byte cmdType, byte dataHigh, byte dataLow) {
        sendCommand(cmdType, dataHigh, dataLow, 1); // 默认发送1次
    }

    // 新增重载方法支持重复次数
    public void sendCommand(byte cmdType, byte dataHigh, byte dataLow, int repeat) {
        if (repeat < 1) repeat = 1;
        byte[] cmd = buildCommand(cmdType, dataHigh, dataLow);
        for (int i = 0; i < repeat; i++) {
            commandQueue.offer(cmd);  // 将指令多次加入队列
            Log.d("OrderManager", "Command enqueued: " + bytesToHex(cmd));
        }
    }

    public void sendCommand06(float positionMeters, int repeat) {
        if (repeat < 1) repeat = 1;
        int mmValue = Math.min((int) (positionMeters * 1000), 0x0BB8);
        byte dataHigh = (byte) ((mmValue >> 8) & 0xFF);
        byte dataLow = (byte) (mmValue & 0xFF);
        byte[] cmd = buildCommand((byte) 0x06, dataHigh, dataLow);
        for (int i = 0; i < repeat; i++) {
            commandQueue.offer(cmd);  // 将指令多次加入队列
            Log.d("OrderManager", "Command enqueued: " + bytesToHex(cmd));
        }
    }

    // 立即发送指令（线程安全）
    private synchronized void sendImmediately(byte[] command) throws IOException {
        if (!isConnected) {
            Frame.HANDLES.sentAll(90001, 0);
            throw new IOException("Not connected");
        }
        out.write(command);
        out.flush();
        Log.d("OrderManager", "Sent: " + bytesToHex(command));
    }

    // 接收数据解析
//    private void processReceivedData(byte[] data, int length) {
//        try {
//            receiveBuffer.put(data, 0, length);
//            receiveBuffer.flip();
//
//            while (receiveBuffer.remaining() >= 7) {
//                // 查找结束符0x3B
//                int endIndex = findEndMarker(receiveBuffer);
//                if (endIndex != -1 && endIndex >= 6) {
//                    byte[] frame = new byte[7];
//                    receiveBuffer.get(frame, 0, 7);
//
//                    Log.e("OrderManager", "Received: " + bytesToHex(frame));
//                    parseResponse(frame);
//
//                    // 重置缓冲区
//                    receiveBuffer.compact();
//                    receiveBuffer.flip();
//                } else {
//                    break;
//                }
//            }
//            receiveBuffer.compact();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    private void processReceivedData(byte[] data, int length) {
        try {
            receiveBuffer.put(data, 0, length);
            receiveBuffer.flip();

            while (true) {
                // 查找起始符 0x55 0xAA
                int startIndex = -1;
                for (int pos = receiveBuffer.position(); pos <= receiveBuffer.limit() - 2; pos++) {
                    if (receiveBuffer.get(pos) == (byte) 0x55 && receiveBuffer.get(pos + 1) == (byte) 0xAA) {
                        startIndex = pos;
                        break;
                    }
                }
                if (startIndex == -1) {
                    // 未找到起始符，清空缓冲区
                    receiveBuffer.position(receiveBuffer.limit());
                    break;
                }
                receiveBuffer.position(startIndex);
                if (receiveBuffer.remaining() < 7) {
                    // 数据不足，等待下次读取
                    break;
                }

                byte[] frame = new byte[7];
                receiveBuffer.get(frame);
                // 检查结束符
                if (frame[6] != END_MARKER) {
                    // 结束符错误，跳过当前起始符，继续查找
                    receiveBuffer.position(startIndex + 1);
                    continue;
                }
                // 计算校验和
                byte checksum = 0;
                for (int i = 0; i < 5; i++) {
                    checksum += frame[i];
                }
                if (checksum != frame[5]) {
                    // 校验和错误，跳过当前帧
                    receiveBuffer.position(startIndex + 7);
                    continue;
                }

                // 处理有效帧
                Log.e("OrderManager", "Received: " + bytesToHex(frame));
                parseResponse(frame);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            receiveBuffer.compact();
        }
    }

    private int findEndMarker(ByteBuffer buffer) {
        for (int i = 0; i < buffer.remaining(); i++) {
            if (buffer.get(i) == END_MARKER) {
                return i;
            }
        }
        return -1;
    }

    // 接收数据解析
    private void parseResponse(byte[] response) {
        lastResponseTime = System.currentTimeMillis();
        if (response.length != 7) return;

        if (response[0] != START_MARKER) return;

        // 校验验证
        byte calculatedChecksum = 0;
        for (int i = 0; i < 5; i++) {
            calculatedChecksum += response[i];
        }

        if (response[5] == calculatedChecksum && response[6] == END_MARKER) {
            // 根据命令类型处理数据
            switch (response[2]) {
                case 0x20:
                    StatusParser.getInstance().updateWorkStatus(response[3], response[4]);
                    Frame.HANDLES.sentAll(10001, 1);
                    break;
                case 0x21:
                    StatusParser.getInstance().updateErrorStatus(response[4]);
                    Frame.HANDLES.sentAll(10002, 1);
                    break;
                case 0x22:
                    StatusParser.getInstance().updatePosition(response[3], response[4]);
                    Frame.HANDLES.sentAll(10003, 1);
                    break;
            }
        }
    }


    public void close() {
        isRunning = false;
        closeResources();
        Frame.HANDLES.sentAll(90001, 0);
    }

    private void closeResources() {
        try {
            commandQueue.clear();
            if (in != null) in.close();
            if (out != null) out.close();
            if (serialPort != null) serialPort.close();
        } catch (IOException e) {
            Log.e("OrderManager", "Close error", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
