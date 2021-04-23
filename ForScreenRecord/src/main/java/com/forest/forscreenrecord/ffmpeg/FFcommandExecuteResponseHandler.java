package com.forest.forscreenrecord.ffmpeg;

public interface FFcommandExecuteResponseHandler extends ResponseHandler {
    void onSuccess(String var1);

    void onProgress(String var1);

    void onFailure(String var1);
}
