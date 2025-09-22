package com.xd.xdcontrol.net;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class StatusProtocol {
    public static byte[] buildStatusPacket(StatusParser parser) {
        // 准备数据
        String clientId = parser.getClientId();
        byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
        int clientIdLen = clientIdBytes.length;

        // 计算数据部分总长度
        int dataLength = 1 + clientIdLen + 4 + 2 + 4 + 4;
        ByteBuffer buffer = ByteBuffer.allocate(2 + 1 + 2 + dataLength + 2)
                .order(ByteOrder.BIG_ENDIAN);

        // 协议头
        buffer.putShort((short) 0xAA55);  // 起始符
        buffer.put((byte) 0x01);          // 版本
        buffer.putShort((short) dataLength); // 数据长度

        // 数据部分
        buffer.put((byte) clientIdLen);    // Client ID长度
        buffer.put(clientIdBytes);         // Client ID内容
        putIP(buffer, parser.getIp());     // IP地址
        putBooleanFlags(buffer, parser);   // 布尔字段
        putVoltages(buffer, parser);       // 电压值

        // 计算CRC
        byte[] dataPart = new byte[dataLength];
        System.arraycopy(buffer.array(), 5, dataPart, 0, dataLength);
        int crc = calculateCRC16(dataPart);
        buffer.putShort((short) crc);

        return buffer.array();
    }

    private static void putIP(ByteBuffer buf, String ip) {
        try {
            String[] segments = ip.split("\\.");
            for (String seg : segments) {
                buf.put((byte) Integer.parseInt(seg));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void putBooleanFlags(ByteBuffer buf, StatusParser parser) {
        // 字节1
        byte b1 = (byte) (
                (parser.getLock() ? 1 : 0) |
                        (parser.getOn220AC() ? 1 << 1 : 0) |
                        (parser.getPowerStatus().isACon ? 1 << 2 : 0) |
                        (parser.getPowerStatus().isDCon ? 1 << 3 : 0) |
                        (parser.getErrorStatus().acOverCurrent ? 1 << 4 : 0) |
                        (parser.getErrorStatus().eggEndTimeout ? 1 << 5 : 0) |
                        (parser.getErrorStatus().eggOriginTimeout ? 1 << 6 : 0) |
                        (parser.getErrorStatus().topOriginTimeout ? 1 << 7 : 0)
        );

        // 字节2
        StatusParser.WorkStatus work = parser.getWorkStatus();
        byte b2 = (byte) (
                (work.eggMotorOrigin ? 1 : 0) |
                        (work.eggMotorEnd ? 1 << 1 : 0) |
                        (work.topMotorOrigin ? 1 << 2 : 0) |
                        (work.topMotorPosition ? 1 << 3 : 0) |
                        (work.acOutputOn ? 1 << 4 : 0)
        );

        buf.put(b1);
        buf.put(b2);
    }

    private static void putVoltages(ByteBuffer buf, StatusParser parser) {
        buf.putInt(Float.floatToIntBits(parser.getPowerStatus().acVoltage));
        buf.putInt(Float.floatToIntBits(parser.getPowerStatus().dcVoltage));
    }

    private static int calculateCRC16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }
}