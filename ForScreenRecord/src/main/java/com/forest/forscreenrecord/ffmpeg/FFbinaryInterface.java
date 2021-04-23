package com.forest.forscreenrecord.ffmpeg;

import java.io.InputStream;
import java.util.Map;

public interface FFbinaryInterface {
    FFtask execute(Map<String, String> var1, String[] var2, FFcommandExecuteResponseHandler var3);

    FFtask execute(String[] var1, FFcommandExecuteResponseHandler var2);

    boolean execute(Map<String, String> var1, String[] var2);

    boolean execute(String[] var1);

    boolean isSupported();

    boolean isCommandRunning(FFtask var1);

    boolean killRunningProcesses(FFtask var1);

    void setTimeout(long var1);

    void setFFmpegFile(InputStream var1);

    boolean isFFmpegExist();
}
