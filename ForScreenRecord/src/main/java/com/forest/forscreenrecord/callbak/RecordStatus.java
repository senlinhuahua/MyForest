package com.forest.forscreenrecord.callbak;

public interface RecordStatus {
    void ScreenRecordOnStart();

    void ScreenRecordFail(String err);

    void ScreenRecordOnEnd(String var);
}
