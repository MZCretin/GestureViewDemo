package com.example.cretin.secondtest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.cretin.secondtest.views.GestureView;

import java.util.List;

public class Main2Activity extends AppCompatActivity implements GestureView.GestureCallBack {
    private GestureView gestureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        getSupportActionBar().hide();
        gestureView = (GestureView) findViewById(R.id.gesture1);
        gestureView.setGestureCallBack(this);
    }

    @Override
    public void gestureVerifySuccessListener(int stateFlag, List<GestureView.GestureBean> data, String message, boolean success) {
        if (success) {
            startActivity(new Intent(Main2Activity.this, Main3Activity.class));
            this.finish();
        }
    }
}
