package com.xd.lib_can.app;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.xd.lib_can.CanInterfaceDevice;
import com.xd.lib_can.CanMessage;
import com.xd.lib_can.android_socketcan;
import com.xd.lib_can.databinding.ActivityDemoBinding;

/**
 * created by zfl
 **/
public class DemoActivity extends AppCompatActivity {
    private ActivityDemoBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDemoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        int[] data = {0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7};
        CanMessage canMessage = new CanMessage();
        canMessage.canid = 123;
        canMessage.eff = 0;
        canMessage.rtr = 0;
        canMessage.len = 8;
        canMessage.data = data;
        CanInterfaceDevice.INSTANCE().sendMessage(canMessage);
        CanInterfaceDevice.INSTANCE().getInterfaceStatus();
    }

}
