package com.forest.forscreenrecord.ffmpeg;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Map;

public class FFmpeg implements FFbinaryInterface {
    private static final int VERSION = 17;
    private static final String KEY_PREF_VERSION = "ffmpeg_version";
    private final FFbinaryContextProvider context;
    private static final long MINIMUM_TIMEOUT = 10000L;
    private long timeout = 9223372036854775807L;
    private static FFmpeg instance = null;
    private FFcheckFileListener fFcheckFileListener;
    private String prefix;

    private FFmpeg(FFbinaryContextProvider context) {
        this.context = context;
        Log.setDebug(Util.isDebug(this.context.provide()));
    }

    public void setFFmpegFile(InputStream input) {
        Log.d("call setFFmpegFile");
        SharedPreferences settings = this.context.provide().getSharedPreferences("ffmpeg_prefs", 0);
        File ffmpeg = FileUtils.getFFmpeg(this.context.provide());

        try {
            FileUtils.copyFile(input, ffmpeg);
            Log.d("successfully wrote ffmpeg file!");
            settings.edit().putInt("ffmpeg_version", 17).apply();
        } catch (IOException var5) {
            Log.d("copy file fail");
            if (this.fFcheckFileListener != null) {
                this.fFcheckFileListener.onCopyFileFail(var5);
            }
        }

        if (this.checkFFmpeg(ffmpeg)) {
            if (this.fFcheckFileListener != null) {
                this.fFcheckFileListener.onAllFinish();
            }
        } else if (this.fFcheckFileListener != null) {
            this.fFcheckFileListener.onVerifyFileFail();
        }

    }

    public void setFFcheckFileListener(FFcheckFileListener fFcheckFileListener) {
        this.fFcheckFileListener = fFcheckFileListener;
    }

    public static FFmpeg getInstance(final Context context) {
        if (instance == null) {
            instance = new FFmpeg(new FFbinaryContextProvider() {
                public Context provide() {
                    return context;
                }
            });
        }

        return instance;
    }

    public boolean isSupported() {
        CpuArch cpuArch = CpuArchHelper.getCpuArch();
        if (cpuArch == CpuArch.NONE) {
            Log.e("arch not supported");
            return false;
        } else {
            return true;
        }
    }

    public boolean isFFmpegExist() {
        CpuArch cpuArch = CpuArchHelper.getCpuArch();
        File ffmpeg = FileUtils.getFFmpeg(this.context.provide());
        SharedPreferences settings = this.context.provide().getSharedPreferences("ffmpeg_prefs", 0);
        int version = settings.getInt("ffmpeg_version", 0);
        if (ffmpeg.exists() && version >= 17) {
            return this.checkFFmpeg(ffmpeg);
        } else {
            switch(cpuArch) {
                case ARMv7:
                    this.prefix = "armv7-a/";
                    break;
                case ARM64:
                    this.prefix = "arm64-v8a/";
                    break;
                case x86:
                    this.prefix = "x86/";
                    break;
                case x86_64:
                    this.prefix = "x86_64/";
                    break;
                default:
                    Log.e("arch not supported");
                    return false;
            }

            Log.d("file does not exist, creating it...");
            Log.d("isSupported: call download");
            return false;
        }
    }

    public FFtask execute(Map<String, String> environvenmentVars, String[] cmd, FFcommandExecuteResponseHandler ffmpegExecuteResponseHandler) {
        if (cmd.length != 0) {
            String[] ffmpegBinary = new String[]{FileUtils.getFFmpeg(this.context.provide()).getAbsolutePath()};
            String[] command = (String[])concatenate(ffmpegBinary, cmd);
            FFcommandExecuteAsyncTask task = new FFcommandExecuteAsyncTask(command, environvenmentVars, this.timeout, ffmpegExecuteResponseHandler);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            return task;
        } else {
            throw new IllegalArgumentException("shell command cannot be empty");
        }
    }

    private static <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public FFtask execute(String[] cmd, FFcommandExecuteResponseHandler ffmpegExecuteResponseHandler) {
        return this.execute((Map)null, cmd, ffmpegExecuteResponseHandler);
    }

    public boolean execute(Map<String, String> environmentVars, String[] cmd) {
        if (cmd.length != 0) {
            String[] ffmpegBinary = new String[]{FileUtils.getFFmpeg(this.context.provide()).getAbsolutePath()};
            String[] command = (String[])concatenate(ffmpegBinary, cmd);
            FFcommandExecuteSynchronous synchronous = new FFcommandExecuteSynchronous(command, environmentVars, this.timeout);
            return synchronous.execute();
        } else {
            throw new IllegalArgumentException("shell command cannot be empty");
        }
    }

    public boolean execute(String[] cmd) {
        return this.execute((Map)null, (String[])cmd);
    }

    public boolean isCommandRunning(FFtask task) {
        return task != null && !task.isProcessCompleted();
    }

    public boolean killRunningProcesses(FFtask task) {
        return task != null && task.killRunningProcess();
    }

    public void setTimeout(long timeout) {
        if (timeout >= 10000L) {
            this.timeout = timeout;
        }

    }

    private boolean checkFFmpeg(File ffmpeg) {
        if (!ffmpeg.canExecute()) {
            try {
                try {
                    Runtime.getRuntime().exec("chmod -R 777 " + ffmpeg.getAbsolutePath()).waitFor();
                } catch (InterruptedException var3) {
                    Log.e("interrupted exception", var3);
                    return false;
                } catch (IOException var4) {
                    Log.e("io exception", var4);
                    return false;
                }

                if (!ffmpeg.canExecute() && !ffmpeg.setExecutable(true)) {
                    Log.e("unable to make executable");
                    return false;
                }
            } catch (SecurityException var5) {
                Log.e("security exception", var5);
                return false;
            }
        }

        Log.d("ffmpeg is ready!");
        return true;
    }

    public String getPrefix() {
        return this.prefix;
    }
}
