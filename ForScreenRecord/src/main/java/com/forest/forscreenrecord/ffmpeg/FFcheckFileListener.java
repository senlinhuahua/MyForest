package com.forest.forscreenrecord.ffmpeg;

import java.io.IOException;

public interface FFcheckFileListener {
    void onCopyFileFail(IOException var1);

    void onAllFinish();

    void onVerifyFileFail();
}
