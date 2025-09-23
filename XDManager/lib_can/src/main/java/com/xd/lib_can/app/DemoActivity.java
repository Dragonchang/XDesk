package com.xd.lib_can.app;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

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
        android_socketcan androidSocketcan = new android_socketcan();
        int fd = androidSocketcan.socketcanOpen("can0");
        if(fd < 0) {
            Log.e("DemoActivity", "open can0 failed with: "+ fd);
        }


    }

}
