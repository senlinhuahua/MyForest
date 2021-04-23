//
// Created by Administrator on 2018/11/16 0016.
//

#ifndef MYFFMPEG_FOFFMPEG_H
#define MYFFMPEG_FOFFMPEG_H

#include "JavaCallHelper.h"
#include "AudioChannel.h"
#include "VideoChannel.h"

extern "C"{
#include <libavformat/avformat.h>
}


class FoFFmpeg {
public:
    FoFFmpeg(JavaCallHelper *callHelper,const char *dataSource);
    ~FoFFmpeg();

    void prepare();
    void _prepare();
    void start();
    void _start();
    void stop();
    void setRenderFremCallback(RenderFramCallback callback);
public:
    bool isPalying;
    char *dataSource;
    pthread_t pid;
    pthread_t pid_play;
    pthread_t pid_stop;
    AVFormatContext *context = 0;
    JavaCallHelper *callHelper;
    AudioChannel *audioChannel = 0;
    VideoChannel *vadeoChannel = 0;
    RenderFramCallback framCallback;

};


#endif //MYFFMPEG_FOFFMPEG_H
