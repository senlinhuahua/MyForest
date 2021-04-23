package com.forest.fffmpeg;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;



/**
 * @date 2018/9/7
 */
public class PlayActivity extends AppCompatActivity {
    private FOPlayer dnPlayer;
    public String url;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager
                .LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_play);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        dnPlayer = new FOPlayer();
        dnPlayer.setSurfaceView(surfaceView);
        dnPlayer.setOnPrepareListener(new FOPlayer.OnPrepareListener() {

            @Override
            public void onPrepare() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PlayActivity.this, "可以播放", Toast.LENGTH_SHORT).show();
                    }
                });
                //dnPlayer.start();
            }
        });

        url = getIntent().getStringExtra("rtmpurl");
        dnPlayer.setDataSource(url);
        dnPlayer.prepare();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
                    .LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_play);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        dnPlayer.setSurfaceView(surfaceView);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        dnPlayer.prepare();
//    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        dnPlayer.stop();
//    }

    @Override
    protected void onStop() {
        super.onStop();
        dnPlayer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dnPlayer.release();
    }

    public void onInit(View view) {
        dnPlayer.prepare();
    }

    public void onPlay(View view) {
        dnPlayer.start();

    }



    public void onStop(View view) {
        dnPlayer.stop();
    }

    public void onRelease(View view) {
        dnPlayer.release();
    }
}
