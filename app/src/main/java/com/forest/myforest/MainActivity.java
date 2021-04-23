package com.forest.myforest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import com.forest.fffmpeg.PlayActivity;
import com.forest.forscreenrecord.RecordManager;
import com.forest.forscreenrecord.callbak.RecordStatus;
import com.forest.forscreenrecord.ffmpeg.ExecuteBinaryResponseHandler;
import com.forest.imeilib.ImeiServer;
import com.forest.myforest.utils.RandomFile;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    //录屏
    private int realResolutionX;
    private int realResolutionY;
    private MediaProjectionManager mMediaProjectionManager;
    private String path;
    private String parentDir;
    private String localPcmPath;
    private String audioAAcPath;
    private String audioImproveAACPath;
    private int RECORD_REQ_CODE3 = 10003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        ImeiServer.onBind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                Intent intent = new Intent(MainActivity.this,PlayActivity.class);
                //intent.putExtra("rtmpurl","rtmp://media3.sinovision.net:1935/live/livestream");
                //intent.putExtra("rtmpurl","rtmp://58.200.131.2:1935/livetv/hunantv");
                intent.putExtra("rtmpurl","rtmp://39.100.192.159/myapp/123");

                startActivity(intent);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startRecord(View view) throws IOException {
        String fileName = System.currentTimeMillis()+".mp4";
        path = getExternalFilesDir(null).getAbsolutePath() + "/video/screen/"+ fileName;
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        parentDir = file.getParentFile().getAbsolutePath();
        localPcmPath = parentDir+"/local_"+fileName+".pcm";
        audioAAcPath = parentDir+"/audio.aac";
        audioImproveAACPath = parentDir + "/improveAudio.aac";
        if (!RecordManager.getInstance().isRecordering()){
            mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent screenCaptureIntent = mMediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(screenCaptureIntent,RECORD_REQ_CODE3);
        }else {
            Log.d("MAIN","待本次操作完成再执行录屏操作");
        }
    }

    public void stopRecord(View view) {
        RecordManager.getInstance().stopRecord(this);
    }
    public void pcmtoaac(View view) {
        RecordManager.getInstance().pcmToAAC(localPcmPath,audioAAcPath,new ExecuteBinaryResponseHandler(){
            @Override
            public void onFailure(String message) {
                Log.d("MAIN","pcmToAAC_onFailure:"+message);
            }

            @Override
            public void onFinish() {
                Log.d("MAIN","pcmToAAC_onFinish:");
            }

            @Override
            public void onSuccess(String message) {
                Log.d("MAIN","pcmToAAC_onSuccess:"+message);
            }
        });

//        RecordManager.getInstance().pcmToAACThenImprove(localPcmPath,audioAAcPath,audioImproveAACPath,30,new ExecuteBinaryResponseHandler(){
//            @Override
//            public void onFailure(String message) {
//                Log.d("MAIN","pcmToAAC_onFailure:"+message);
//            }
//
//            @Override
//            public void onFinish() {
//                Log.d("MAIN","pcmToAAC_onFinish:");
//            }
//
//            @Override
//            public void onSuccess(String message) {
//                Log.d("MAIN","pcmToAAC_onSuccess:"+message);
//            }
//        });
//
    }
    public void aacZoom(View view) {
        RecordManager.getInstance().improveAAC(audioAAcPath,audioImproveAACPath,30,new ExecuteBinaryResponseHandler(){
            @Override
            public void onFailure(String message) {
                super.onFailure(message);
                Log.d("MAIN","aacZoom_onFailure:"+message);
            }

            @Override
            public void onFinish() {
                super.onFinish();
                Log.d("MAIN","aacZoom_onFinish:");
            }

            @Override
            public void onSuccess(String message) {
                super.onSuccess(message);
                Log.d("MAIN","aacZoom_onSuccess:"+message);
            }
        });
    }
    public void merge(View view) {
        String outVedioPath = parentDir+"/end666.mp4";
        RecordManager.getInstance().mergeAudioVideoFile(path,audioImproveAACPath,outVedioPath,new ExecuteBinaryResponseHandler(){
            @Override
            public void onFailure(String message) {
                super.onFailure(message);
                Log.d("MAIN","merge_onFailure:"+message);
            }

            @Override
            public void onFinish() {
                super.onFinish();
                Log.d("MAIN","merge_onFinish:");
            }

            @Override
            public void onSuccess(String message) {
                super.onSuccess(message);
                Log.d("MAIN","merge_onSuccess:"+message);
            }
        });


    }
    public void getImei(View view) {
        Toast.makeText(this,ImeiServer.getIMEI(),Toast.LENGTH_LONG).show();
    }

    public void jiajiemi(View view) {
        //String filepath = getExternalFilesDir(null).getAbsolutePath()+"/test.txt";
        String filepath = getExternalFilesDir(null).getAbsolutePath()+"/20210409111457543.mp4";
        Log.d("forest","filePATH:"+filepath);
        RandomFile ran=new RandomFile(filepath);
        try{
            ran.openFile();
            ran.coding();
            ran.closeFile();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            if (requestCode == RECORD_REQ_CODE3){
                MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {
                    Log.e("MAIN", "media projection is null");

                    return;
                }
                RecordManager.getInstance().startRecord(this,mediaProjection,path,recordStatus);
            }
        }

    }

    RecordStatus recordStatus = new RecordStatus() {
        @Override
        public void ScreenRecordOnStart() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("MAIN","recordStatus:开始录制");
                }
            });
        }

        @Override
        public void ScreenRecordFail(String error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("MAIN","recordStatus:录屏失败");

                }
            });
        }

        @Override
        public void ScreenRecordOnEnd(String videoPath) {
            Log.d("MAIN","录制成功：videoPath："+videoPath);

        }
    };

    public void checkPermission() {
        // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）
        PackageManager pm = getPackageManager();
        boolean permission_readStorage = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.READ_EXTERNAL_STORAGE", getPackageName()));
        boolean permission_writeStorage = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", getPackageName()));

//        if (permission_writeStorage){
//            Log.d("forest","有写文件权限:"+mActivity.getPackageName());
//        }else {
//            Log.d("forest","没有写文件权限:"+mActivity.getPackageName());
//        }
//
//        if (!(permission_readStorage && permission_writeStorage)) {
//            ActivityCompat.requestPermissions(mActivity, new String[]{
//                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
//            }, 0x01);
//        }

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 0x01);
    }
}
