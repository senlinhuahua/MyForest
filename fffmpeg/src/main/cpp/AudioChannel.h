//
// Created by Administrator on 2018/11/19 0019.
//

#ifndef MYFFMPEG_AUDIOCHANNEL_H
#define MYFFMPEG_AUDIOCHANNEL_H


#include "BaseChannel.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
extern "C"{
#include <libswresample/swresample.h>
};


class AudioChannel: public BaseChannel {
public:
    AudioChannel(int id,AVCodecContext *avCodecContext,AVRational time_base);
    ~AudioChannel();
    void paly();

    void stop();

    void decode();
    void _paly();

    int getPcm();

public:
    uint8_t *data = 0;
    int out_channels;
    int out_samplesize;
    int out_sample_rate;

private:
    pthread_t pid_audiodecode;
    pthread_t pid_audioplay;

    //指针的指针必须初始化
    SLObjectItf enginObject = 0;
    SLEngineItf engineItf = 0;

    //混音器
    SLObjectItf outputMixObject = 0;
    //播放器
    SLObjectItf bqPlayerObject = 0;
    //播放器接口
    SLPlayItf bqPlayerInterface = 0;

    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueueInterface =0;


    //重采样
    SwrContext *swrContext = 0;
};


#endif //MYFFMPEG_AUDIOCHANNEL_H
