package com.forest.fffmpeg;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by forest on 2018/11/16 0016.
 */

public class FOPlayer implements SurfaceHolder.Callback {

    static {
        System.loadLibrary("fffmpeg-native-lib");
    }

    private String dataSource;
    private SurfaceHolder holder;
    private OnPrepareListener listener;

    /**
     * 设置地址
     * @param dataSource
     */
    public void setDataSource(String dataSource){
        this.dataSource = dataSource;
    }

    /**
     * 播放显示的画布
     * @param surfaceView
     */
    public void setSurfaceView(SurfaceView surfaceView){
        holder = surfaceView.getHolder();
        holder.addCallback(this);


    }

    private void onError(int error){
        System.out.println("Java接到回调:"+error);
    }

    public void onPrepare(){
        if (listener != null){
            listener.onPrepare();
        }

    }
    public void setOnPrepareListener(OnPrepareListener listener){
        this.listener = listener;
    }

    public interface OnPrepareListener{
        void onPrepare();
    }

    /**
     * 准备视频
     */
    public void prepare(){
        nativePrepare(dataSource);

    }

    public void start(){
        nativeStart();

    }

    public void stop(){
        nativeStop();
    }

    public void release(){
        holder.removeCallback(this);
        nativeRelease();

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {


    }

    //横竖屏切换时
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        nativeSurface(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    native void nativePrepare(String dataSource);
    native void nativeStart();
    native void nativeStop();
    native void nativeRelease();
    native void nativeSurface(Surface surface);
}
