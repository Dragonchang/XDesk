package com.xd.lib_can;

public class Util {
    // 十六进制字符表，用于快速查找
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /**
     * 将byte数组转换为十六进制字符串（大写）
     * @param bytes 输入的byte数组
     * @return 转换后的十六进制字符串，若输入为null则返回null
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            // 将byte转为无符号整数（0-255）
            int value = bytes[i] & 0xFF;
            // 高4位对应十六进制的第一个字符
            hexChars[i * 2] = HEX_CHARS[value >>> 4];
            // 低4位对应十六进制的第二个字符
            hexChars[i * 2 + 1] = HEX_CHARS[value & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * 将指定范围的byte数组转换为long
     * @param bytes 源byte数组
     * @param offset 起始偏移量
     * @param length 要转换的字节长度（最多8字节）
     * @return 转换后的long值
     */
    public static long byteToLong(byte[] bytes, int offset, int length) {
        if (length < 1 || length > 8) {
            throw new IllegalArgumentException("转换长度必须在1-8之间");
        }

        long result = 0;
        for (int i = 0; i < length; i++) {
            // 按大端模式（高位在前）拼接
            result = (result << 8) | (bytes[offset + i] & 0xFF);
        }
        return result;
    }
}
