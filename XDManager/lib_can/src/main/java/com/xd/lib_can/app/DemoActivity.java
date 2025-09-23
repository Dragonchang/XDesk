package com.xd.lib_can.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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


    }

}
