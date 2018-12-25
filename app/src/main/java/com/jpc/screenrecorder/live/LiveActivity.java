package com.jpc.screenrecorder.live;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jpc.screenrecorder.R;
import com.jpc.screenrecorder.ScreenRecordService;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;


public class LiveActivity extends AppCompatActivity {
    private RxPermissions rxPermissions;
    private MediaProjectionManager projectionManager;
    private RecordService recordService;//录屏ervice
    private Button start;
    private Button stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rxPermissions = new RxPermissions(this);
        start = findViewById(R.id.buttonRecord);
        stop = findViewById(R.id.buttonCapture);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecord();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
            }
        });
        initRecord();
    }

    /**
     * 绑定录屏service
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /**
     * 初始化录屏数据
     */
    public void initRecord() {
        if (Build.VERSION.SDK_INT >= 21) {//大于5.0
            projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent intent = new Intent(this, RecordService.class);
            bindService(intent, connection, BIND_AUTO_CREATE);
        }
    }

    //录屏相关模块
    private static final int RECORD_REQUEST_CODE = 101;

    /**
     * 开始录屏
     */
    private void startRecord() {

        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
    }

    /**
     * 结束录屏
     */
    public void stopRecord() {
        if (recordService != null && recordService.isRunning()) {
            recordService.stopRecord();
        }

    }

    private MediaProjection mediaProjection;
    //更新录屏进度条
    private Handler recordHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0://录屏进度

                    break;
                case 1://开始录屏
                    //resultCode  data
                    mediaProjection = projectionManager.getMediaProjection(msg.arg1, (Intent) msg.obj);
                    recordService.setMediaProject(mediaProjection);
                    recordService.startRecord();
//                    recordStart();
                    break;
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //录屏result
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            final android.os.Message message = new android.os.Message();
            message.what = 1;
            message.arg1 = resultCode;
            message.obj = data;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    recordHandler.sendMessage(message);
                }
            });


        }
    }
}

