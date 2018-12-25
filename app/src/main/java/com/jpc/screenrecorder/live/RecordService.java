package com.jpc.screenrecorder.live;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * 录屏service
 */
public class RecordService extends Service {
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private String rootDir, rootDirectory;//保存的视频文件

    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;


    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        //mediaRecorder = new MediaRecorder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    /**
     * 开始录屏
     *
     * @return
     */
    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }
        initRecorder();
        try {
            if (virtualDisplay == null) {
                createVirtualDisplay();
            }

            mediaRecorder.start();
        } catch (IllegalStateException e) {
            Log.i("RecordService", "----e= " + e);
            e.printStackTrace();
        }
        running = true;
        return true;
    }

    /**
     * 结束录屏
     *
     * @return
     */
    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {

            }
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (null != virtualDisplay) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        mediaProjection.stop();
        mediaProjection = null;
        return true;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    /***
     * 录屏参数设置  这里只录制视频
     */
    private void initRecorder() {
        rootDir = getRecordVideoPath();
        mediaRecorder = new MediaRecorder();
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(rootDir);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(15);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reStart() {
        running = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        running = false;
    }

    public String getRecordFile() {
        return rootDir;
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }

    /**
     * 录制视频的保存目录
     *
     * @return 文件目录
     */
    public static String getRecordVideoPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //String rootDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ScreenRecord";
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ScreenRecord" + "/video" + ".mp4";
            /*File fileDirectory = new File(rootDirectory);
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs();
            }*/
            File file = new File(rootDir);
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return rootDir;
        } else {
            return null;
        }
    }
}