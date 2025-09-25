package com.xd.lib_can;

public class Util {
    public static int[] subLongArrayToIntArray(long[] longArray) {
        // 处理空数组情况
        if (longArray == null) {
            return new int[0];
        }

        // 计算起始索引（从第4个元素开始，索引为4）
        int startIndex = 4;

        // 如果数组长度小于等于起始索引，返回空数组
        if (longArray.length <= startIndex) {
            return new int[0];
        }

        // 计算需要截取的元素数量
        int length = longArray.length - startIndex;

        // 创建目标int数组
        int[] intArray = new int[length];

        // 循环复制并转换（注意：long转int可能丢失精度）
        for (int i = 0; i < length; i++) {
            // 直接强制转换，若long值超出int范围会溢出
            intArray[i] = (int) longArray[startIndex + i];
        }

        return intArray;
    }

    /**
     * 将int数组转换为十六进制字符串（默认格式：大写，无前缀，每个int占8位十六进制）
     * @param intArray 输入的int数组
     * @return 转换后的十六进制字符串
     */
    public static String intArrayToHexString(int[] intArray) {
        if (intArray == null || intArray.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int num : intArray) {
            // %08X 表示：补零至8位，大写十六进制
            sb.append(String.format("%08X", num));
        }
        return sb.toString();
    }
}
