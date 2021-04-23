package com.forest.forscreenrecord.ffmpeg;

public interface FFtask {
    void sendQuitSignal();

    boolean isProcessCompleted();

    boolean killRunningProcess();
}
