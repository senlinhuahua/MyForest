package com.forest.forscreenrecord.callbak;

public interface MergerStatus {
    void mergering();

    void error(String error);

    void success(String success);
}
