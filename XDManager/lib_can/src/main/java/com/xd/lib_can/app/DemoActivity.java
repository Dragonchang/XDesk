package com.xd.lib_can.app;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.xd.lib_can.CanInterfaceDevice;
import com.xd.lib_can.CanMessage;
import com.xd.lib_can.ICanMessageReceiveCallBack;
import com.xd.lib_can.InterfaceStatus;
import com.xd.lib_can.Util;
import com.xd.lib_can.databinding.ActivityDemoBinding;

/**
 * created by zfl
 **/
public class DemoActivity extends AppCompatActivity implements ICanMessageReceiveCallBack {
    private ActivityDemoBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDemoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InterfaceStatus status = CanInterfaceDevice.INSTANCE().getInterfaceStatus();
        if(!status.equals(InterfaceStatus.INTERFACE_STATUS_UP)){
            CanInterfaceDevice.INSTANCE().UpCanInterface();
        } else {
            Log.e("","can has been up");
        }
        CanInterfaceDevice.INSTANCE().registerReceiveCallBack(this);
        new Thread() {
            long msgID = 0x18ffa040;
            byte[] data = {(byte) 0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5, (byte)0xA6, (byte)0xA7};
            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    CanMessage canMessage = new CanMessage(msgID, data);
                    CanInterfaceDevice.INSTANCE().sendMessage(canMessage);
                }
            }
        }.start();
    }

    @Override
    public void onMessage(CanMessage msg) {
        Log.e("","msg: " + msg.toString());
    }
}
