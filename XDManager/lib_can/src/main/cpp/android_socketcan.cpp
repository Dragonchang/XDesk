#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>

extern "C"
__attribute__((unused)) JNIEXPORT jint JNICALL
Java_com_xd_lib_1can_android_1socketcan_socketcanOpen(JNIEnv *env, jobject thiz, jstring canx) {
    int fd;
    struct ifreq ifr;
    struct sockaddr_can addr;

    /* open socket */
    if ((fd = socket(PF_CAN, SOCK_RAW, CAN_RAW)) < 0) {
        return -1;
    }

    const char *str = env->GetStringUTFChars(canx, 0);
    strcpy(ifr.ifr_name, str);
    ioctl(fd, SIOCGIFINDEX, &ifr);

    memset(&addr, 0, sizeof(addr));
    addr.can_family = AF_CAN;
    addr.can_ifindex = ifr.ifr_ifindex;

    if (bind(fd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        return -2;
    }
    return fd;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_xd_lib_1can_android_1socketcan_socketcanClose(JNIEnv *env, jobject thiz, jint fd) {
    return close(fd);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_xd_lib_1can_android_1socketcan_socketcanSend(JNIEnv *env, jobject thiz, jint fd,
                                                      jlong canid, jlong eff, jlong rtr, jint len,
                                                      jbyteArray data) {
    struct can_frame frame;
    frame.can_id = (eff << 31) | (rtr << 30) | canid;
    frame.can_dlc = len;
    jbyte *pdata = env->GetByteArrayElements(data, 0);
    for(uint8_t i = 0; i < len; i++) {
        frame.data[i] = pdata[i] & 0xFF;
    }
    int ret =  write(fd, &frame, sizeof(struct can_frame));
    env->ReleaseByteArrayElements(data, pdata, 0);
    return  ret;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_xd_lib_1can_android_1socketcan_socketcanReceive(JNIEnv *env, jobject thiz, jint fd) {
    jbyteArray ret;
    ret = env->NewByteArray(12);
    struct can_frame frame;
    read(fd, &frame, sizeof(struct can_frame));
    jbyte data[12];
    // can_id是32位无符号整数，按大端字节序（网络字节序）拆分
    uint32_t canId = frame.can_id;
    // 大端格式：高位字节在前（符合CAN总线协议的通常约定）
    data[0] = (jbyte)((canId >> 24) & 0x1F);  // 最高位字节
    data[1] = (jbyte)((canId >> 16) & 0xFF);
    data[2] = (jbyte)((canId >> 8) & 0xFF);
    data[3] = (jbyte)(canId & 0xFF);          // 最低位字节

    // 若需要小端字节序（本地字节序），使用以下代码：
    // data[0] = (jbyte)(canId & 0xFF);
    // data[1] = (jbyte)((canId >> 8) & 0xFF);
    // data[2] = (jbyte)((canId >> 16) & 0xFF);
    // data[3] = (jbyte)((canId >> 24) & 0xFF);

    for(uint8_t i = 0; i < frame.can_dlc; i++) {
        data[i + 4] = frame.data[i];
    }
    env->SetByteArrayRegion(ret, 0, 12, data);
    return ret;
}
