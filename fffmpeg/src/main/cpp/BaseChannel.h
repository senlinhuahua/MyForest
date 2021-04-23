//
// Created by Administrator on 2018/11/19 0019.
//

#ifndef MYFFMPEG_BASECHANNEL_H
#define MYFFMPEG_BASECHANNEL_H

extern "C"
{
#include <libavcodec/avcodec.h>
};
#include "safe_queue.h"
#include "macro.h"
#include "JavaCallHelper.h"

class BaseChannel{
public:
    BaseChannel(int id,AVCodecContext *avCodecContext,AVRational time_base):channelId(id),
    avCodecContext(avCodecContext),time_base(time_base){
        avframes.setReleaseCallback(BaseChannel::releaseAvFrame);
        packagets.setReleaseCallback(BaseChannel::releaseAvPacket);
    }
    virtual ~BaseChannel(){
        if (avCodecContext) {
            avcodec_close(avCodecContext);
            avcodec_free_context(&avCodecContext);
            avCodecContext = 0;
        }
        packagets.clear();
        avframes.clear();
        LOGE("释放channel:%d %d", packagets.size(), avframes.size());

    }
    //int id;
    /**
     * 释放
     * @param avPacket
     */
    static void releaseAvPacket(AVPacket **avPacket){
        if (avPacket){
            av_packet_free(avPacket);
            *avPacket=0;
        }
    }
    static void releaseAvFrame(AVFrame **frame){
        if (frame){
            av_frame_free(frame);
            *frame=0;
        }
    }

    //抽象方法
    virtual void paly() = 0;

    virtual void stop() = 0;

    void clear() {
        packagets.clear();
        avframes.clear();
    }

    void stopWork() {
        packagets.setWork(0);
        avframes.setWork(0);
    }

    void startWork() {
        packagets.setWork(1);
        avframes.setWork(1);
    }

    SafeQueue<AVPacket*> packagets;
    //解码数据包队列
    SafeQueue<AVFrame *> avframes;
    volatile int channelId;
    bool isPlaying;
    AVCodecContext *avCodecContext;
    AVRational time_base;
    JavaCallHelper *javaCallHelper;
public:
    double clock;
};
#endif //MYFFMPEG_BASECHANNEL_H
