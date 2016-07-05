package com.example.cretin.secondtest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.cretin.secondtest.views.GestureView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements GestureView.GestureCallBack {
    private GestureView gestureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        gestureView = (GestureView) findViewById(R.id.gesture);
        gestureView.setGestureCallBack(this);
        //不调用这个方法会造成第二次启动程序直接进入手势识别而不是手势设置
        gestureView.clearCache();
        gestureView.setMinPointNums(5);
    }

    @Override
    public void gestureVerifySuccessListener(int stateFlag, List<GestureView.GestureBean> data, String message, boolean success) {
        if (stateFlag == GestureView.STATE_LOGIN) {
            startActivity(new Intent(MainActivity.this, Main2Activity.class));
            this.finish();
        }
    }
}