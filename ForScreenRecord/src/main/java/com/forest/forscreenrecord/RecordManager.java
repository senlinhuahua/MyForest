package com.forest.forscreenrecord;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.forest.forscreenrecord.audio.AudioTool;
import com.forest.forscreenrecord.callbak.MergerStatus;
import com.forest.forscreenrecord.callbak.RecordStatus;
import com.forest.forscreenrecord.ffmpeg.ExecuteBinaryResponseHandler;
import com.forest.forscreenrecord.ffmpeg.FFmpeg;
import com.forest.forscreenrecord.ffmpeg.FFtask;
import com.forest.forscreenrecord.ffmpeg.Log;

import java.io.File;


public class RecordManager {
    private static RecordManager instance;
    private Context mContext;
    private FFmpeg ffmpeg;
    private FFtask fftask;
    private RecordStatus recordStatus;
    private RecorderService recorderService;
    private MediaProjection mediaProjection;
    private boolean isRecordering = false;
    private boolean isMergering = false;
    public int realResolutionX;
    public int realResolutionY;
    private int bitRate = 2097152;
    private int frameRate = 30;
    private int decibelNum = 30;
    private int tempHow = 0;
    private boolean delCache;
    private String savePath;
    private String parentDir;
    private String localPcmPath;
    private String audioAAcPath;
    private String audioImproveAACPath;
    private ServiceConnection recorderServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecorderService.RecorderServiceBinder recorderServiceBinder = (RecorderService.RecorderServiceBinder)service;
            recorderService = recorderServiceBinder.getRecorderService();
            recorderService.startRecorder(mediaProjection, savePath, recordStatus);
        }

        public void onServiceDisconnected(ComponentName name) {
            recorderService.stopRecorder();
        }
    };



    private RecordManager() {
    }

    public static synchronized RecordManager getInstance() {
        if (instance == null) {
            instance = new RecordManager();
        }

        return instance;
    }

    public void setDecibelNum(int decibelNum) {
        this.decibelNum = decibelNum;
    }

    public int getTempHow() {
        return this.tempHow;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public boolean isRecordering() {
        return this.isRecordering;
    }

    public void setRecordering(boolean recordering) {
        this.isRecordering = recordering;
    }


    public void startRecord(Activity activity, MediaProjection mediaProjection, String tempPath, RecordStatus recordStatus) {
        this.getResolution(activity);
        this.mContext = activity;
        this.recordStatus = recordStatus;
        this.mediaProjection = mediaProjection;
        this.savePath = tempPath;
        setRecordering(true);
        //startRecordSound();

        Intent intent = new Intent(mContext, RecorderService.class);
        intent.putExtra("bitRate", bitRate);
        intent.putExtra("frameRate", frameRate);
        mContext.bindService(intent, recorderServiceConnection, Context.BIND_AUTO_CREATE);
        initFFmpge(mContext);
    }

    //录制本地声音
    private void startRecordSound() {
        File video = new File(savePath);
        String pcmName = video.getName();
        parentDir = video.getParentFile().getAbsolutePath();
        localPcmPath =  parentDir+ "/local_" + pcmName + ".pcm";
        audioAAcPath = parentDir + "/audio.aac";
        audioImproveAACPath = parentDir + "/improveAudio.aac";
        AudioTool.getInstance().setSreenRecording(true);
        AudioTool.getInstance().startScreenRecordSound(localPcmPath);
    }

    public void stopRecord(Context context) {
        if (isRecordering()) {
            recorderService.stopRecorder();
            context.unbindService(this.recorderServiceConnection);
            if (recordStatus != null) {
                recordStatus.ScreenRecordOnEnd(savePath);
            }

        }
    }


    public void initFFmpge(Context context) {
        try {
            ffmpeg = FFmpeg.getInstance(context);
            if (!this.ffmpeg.isSupported()) {
                Toast.makeText(context, "当前设备不支持ffmpge", Toast.LENGTH_LONG).show();
                ffmpeg = null;
                return;
            }
            ffmpeg.setFFmpegFile(context.getAssets().open("ffmpeg-armv7-a"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //aac合并
    public void mergeAudioVideoFile(String noSoundVideoPath, String aaCfilePath, String endMp4, ExecuteBinaryResponseHandler executeBinaryResponseHandler) {
        String[] cmd = new String[]{"-i", noSoundVideoPath, "-i", aaCfilePath, "-c:v", "copy", "-c:a", "copy", endMp4};
        if (!ffmpeg.isCommandRunning(fftask)) {
            fftask = ffmpeg.execute(cmd, executeBinaryResponseHandler);
        }

    }


    //pcm 转aac
    public void pcmToAAC(String pcmPath, String aacOutPath, ExecuteBinaryResponseHandler executeBinaryResponseHandler) {
        String[] cmd = new String[]{"-f", "s16le", "-ar", "16000", "-ac", "1", "-i", pcmPath, aacOutPath};
        if (!ffmpeg.isCommandRunning(this.fftask)) {
            fftask = ffmpeg.execute(cmd, executeBinaryResponseHandler);
        }

    }

    //aac增大 30
    public void improveAAC(String aacInPath, String aacOutPath, int decibelNum, ExecuteBinaryResponseHandler executeBinaryResponseHandler) {
        String volume = "volume=+" + decibelNum + "dB";
        String[] cmd = new String[]{"-i", aacInPath, "-af", volume, aacOutPath};
        if (!ffmpeg.isCommandRunning(fftask)) {
            fftask = ffmpeg.execute(cmd, executeBinaryResponseHandler);
        }

    }


    //pcm转aac且增大
    public void pcmToAACThenImprove(String pcmPath, String aacOutPath, String aacImproveOutPath, int decibelNum, ExecuteBinaryResponseHandler executeBinaryResponseHandler) {
        String volume = "volume=+" + decibelNum + "dB";
        String[] cmd = new String[]{"-f", "s16le", "-ar", "16000", "-ac", "1", "-i", pcmPath, aacOutPath, "-af", volume, aacImproveOutPath};
        if (!ffmpeg.isCommandRunning(fftask)) {
            fftask = ffmpeg.execute(cmd, executeBinaryResponseHandler);
        }
    }

    private void greatParentDir(String filePath) {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

    }

    public void renameFile(String path, String oldname, String newname) {
        if (!oldname.equals(newname)) {
            File oldfile = new File(path + "/" + oldname);
            File newfile = new File(path + "/" + newname);
            if (!oldfile.exists()) {
                return;
            }

            if (newfile.exists()) {
                System.out.println(newname + "已经存在！");
            } else {
                oldfile.renameTo(newfile);
            }
        } else {
            System.out.println("新文件名和旧文件名相同...");
        }

    }

    private void getResolution(Activity activity) {
        DisplayMetrics metric = new DisplayMetrics();
        if (VERSION.SDK_INT >= 17) {
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metric);
        }

        this.realResolutionX = metric.widthPixels;
        this.realResolutionY = metric.heightPixels;
    }


}
