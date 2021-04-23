//
// Created by Administrator on 2018/11/19 0019.
//

#ifndef MYFFMPEG_VADEOCHANNEL_H
#define MYFFMPEG_VADEOCHANNEL_H


#include "BaseChannel.h"
#include "AudioChannel.h"

extern "C"
{
#include <libswscale/swscale.h>
};

typedef void (*RenderFramCallback)(uint8_t *, int, int, int);
class VideoChannel : public BaseChannel{
public:
    VideoChannel(int id, AVCodecContext *avCodecContext, AVRational time_base, int fps);
    ~VideoChannel();
//解码+播放
    void paly();

    void stop();

    void setAudioChannel(AudioChannel* audioChannel);

    void decode();
    void render();
    void setRenderFramCallback(RenderFramCallback framCallback);

private:
    pthread_t pid_decode;
    pthread_t pid_render;
    int fps;
    SafeQueue<AVFrame*>frame;
    SwsContext * swsContext;
    RenderFramCallback callback;
    AudioChannel *audioChannel=0;
};


#endif //MYFFMPEG_VADEOCHANNEL_H
