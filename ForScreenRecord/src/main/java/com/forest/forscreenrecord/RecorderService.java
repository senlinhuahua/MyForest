package com.forest.forscreenrecord;


import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplay.Callback;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import com.forest.forscreenrecord.audio.AudioTool;
import com.forest.forscreenrecord.callbak.RecordStatus;

import java.io.IOException;
import java.util.List;


public class RecorderService extends Service {
    private String TAG = this.getClass().getSimpleName();
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private DisplayMetrics dm;
    private VirtualDisplay virtualDisplay;
    private boolean isRecord = true;
    private boolean isMediaRecorderPrepare = false;
    private RecordStatus recordStatus;
    private int bitRate;
    private int frameRate;
    private String savePath;

    public RecorderService() {
    }

    public void init() {
        isMediaRecorderPrepare = false;
        virtualDisplay = null;
        mediaRecorder = null;
        mediaProjection = null;
    }

    public void onCreate() {
        super.onCreate();
        init();
        dm = getResources().getDisplayMetrics();
        startForegroundService();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public IBinder onBind(Intent intent) {
        bitRate = intent.getIntExtra("bitRate", 0);
        frameRate = intent.getIntExtra("frameRate", 0);
        return new RecorderServiceBinder();
    }

    public void startRecorder(MediaProjection mediaProjection, String videoPath, RecordStatus recordStatus) {
        this.recordStatus = recordStatus;
        if (mediaProjection == null) {
            if (recordStatus != null) {
                recordStatus.ScreenRecordFail("startRecorder: But mediaProjection == null !!!");
            }
        } else {
            this.mediaProjection = mediaProjection;
            mediaRecorder = new MediaRecorder();
            initMediaRecorder(videoPath);
            if (isMediaRecorderPrepare) {
                createVirtualDisplay();
                if (recordStatus != null) {
                    recordStatus.ScreenRecordOnStart();
                }
                mediaRecorder.start();
                Log.i(TAG, "startRecorder: ok!");
            }

        }
    }

    public void stopRecorder() {
        if (mediaRecorder != null && isMediaRecorderPrepare) {
            mediaRecorder.stop();
            mediaRecorder.release();
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        init();
    }

    private void initMediaRecorder(String filePath) {
        savePath = filePath;
        if (isRecord){
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//1
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if (isRecord){
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//1
        }
        if (VERSION.SDK_INT >= 26){
            mediaRecorder.setVideoSize(RecordManager.getInstance().realResolutionX, RecordManager.getInstance().realResolutionY);
        }else {
            int[] supportMaxSize = getSupportMaxSize();
            mediaRecorder.setVideoSize(supportMaxSize[0], supportMaxSize[1]);
        }
        mediaRecorder.setVideoEncodingBitRate(bitRate);
        mediaRecorder.setVideoFrameRate(frameRate);

        try {
            mediaRecorder.prepare();
            isMediaRecorderPrepare = true;
        } catch (IOException e) {
            isMediaRecorderPrepare = false;
            e.printStackTrace();
        }

    }

    private int[] getSupportMaxSize(){
        int[] maxSupport = {1920,1080};
        String maxSize = "1920x1080";
        Camera mCamera = Camera.open(1);
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        for (int i = 0; i < previewSizes.size(); i++) {
            maxSize = previewSizes.get(i).width + "x" + previewSizes.get(i).height;
            Log.e("SupportedPreviewSizes","SupportedPreviewSizes : " +maxSize);
        }
        maxSupport[0] = Integer.parseInt(maxSize.split("x")[0]);
        maxSupport[1] = Integer.parseInt(maxSize.split("x")[1]);
        return maxSupport;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecord",
                dm.widthPixels,
                dm.heightPixels,
                dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    private void startForegroundService() {
        if (VERSION.SDK_INT >= 26) {
            NotificationManager mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
            String id = "id";
            CharSequence name = "RecorderService";
            NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
            Notification notification = new Builder(this, "id")
                    .setContentTitle("录屏")
                    .setContentText("录屏中")
                    .setOngoing(true)
                    //.setSmallIcon(R.drawable.logo_r)
                    .build();
            startForeground(1, notification);
        } else {
            Notification notification = new Builder(this)
                    //.setSmallIcon(R.drawable.)
                    .setContentTitle("录屏")
                    .setContentText("录屏中")
                    .setOngoing(true)
                    .build();
            startForeground(1, notification);
        }

    }

    private void closeNotification() {
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }

    public void onDestroy() {
        super.onDestroy();
        stopRecorder();
        closeNotification();
        RecordManager.getInstance().setRecordering(false);
        AudioTool.getInstance().setSreenRecording(false);
        AudioTool.getInstance().stopScreenRecordSound();
    }

    public class RecorderServiceBinder extends Binder {
        public RecorderServiceBinder() {
        }

        public RecorderService getRecorderService() {
            return RecorderService.this;
        }
    }
}
