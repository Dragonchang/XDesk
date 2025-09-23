package com.xd.lib_can;

import android.bluetooth.BluetoothDevice;

public interface ICanMessageReceiveCallBack {
    void onMessage(long[] data);
}
