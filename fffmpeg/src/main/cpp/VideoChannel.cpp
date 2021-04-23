//
// Created by Administrator on 2018/11/19 0019.
//

#include "VideoChannel.h"
#include "macro.h"

extern "C"{
#include <libavutil/imgutils.h>
#include <libavutil/time.h>
}

void *decodetask(void *args){
    VideoChannel *vadeoChannel = (VideoChannel *) args;
    vadeoChannel->decode();

    return 0;
}

void *rendertask(void *args){
    VideoChannel *vadeoChannel = (VideoChannel *) args;
    vadeoChannel->render();

    return 0;
}

/**
 * 丢包
 * @param q
 */
void dropAvPacket(queue<AVPacket*> &q){
    while (!q.empty()){
        AVPacket *packet = q.front();
        if (packet->flags != AV_PKT_FLAG_KEY){
            //如果不属于I帧
            BaseChannel::releaseAvPacket(&packet);
            q.pop();
        } else{
            break;
        }
    }

}

void dropAvFrame(queue<AVFrame*> &q){
    while (!q.empty()){
       AVFrame * avFrame = q.front();
       BaseChannel::releaseAvFrame(&avFrame);
       q.pop();
    }

}

VideoChannel::VideoChannel(int id, AVCodecContext *avCodecContext, AVRational time_base, int fps)
        :BaseChannel(id,avCodecContext,time_base){

     this->fps = fps;
     //用于同步操作队列函数指针
     //packagets.setSyncHandle(dropAvPacket);
     avframes.setSyncHandle(dropAvFrame);


}

VideoChannel::~VideoChannel() {
    //avframes.clear();
}

void VideoChannel::setAudioChannel(AudioChannel *audioChannel) {
    this->audioChannel = audioChannel;

}

void VideoChannel::paly() {
    isPlaying = 1;
    //解码
    avframes.setWork(1);
    packagets.setWork(1);
    pthread_create(&pid_decode,0,decodetask,this);
    //播放
    pthread_create(&pid_render,0,rendertask,this);
}

void VideoChannel::decode() {
    AVPacket *avPacket = 0;
    while (isPlaying){
        //取出一个数据包 1，成功
        int ret = packagets.pop(avPacket);
        if (!isPlaying){
            break;
        }
        if (!ret){
            continue;
        }
        ret = avcodec_send_packet(avCodecContext,avPacket);
        releaseAvPacket(&avPacket);
        if (ret !=0){
            break;
        }
        AVFrame *avFrame = av_frame_alloc();
        ret = avcodec_receive_frame(avCodecContext,avFrame);
        if (ret == AVERROR(EAGAIN)){
            continue;
        } else if(ret != 0){
            break;
        }
        //再开一个线程 来播放 (流畅度)
        avframes.push(avFrame);
    }
    releaseAvPacket(&avPacket);
}

void VideoChannel::render() {
    // 目标：RGBA  AV_PIX_FMT_ARGB
    swsContext = sws_getContext(avCodecContext->width,avCodecContext->height,
    avCodecContext->pix_fmt,avCodecContext->width,avCodecContext->height,AV_PIX_FMT_RGBA,
    SWS_BILINEAR,0,0,0);
    //每个画面刷新的间隔
    double frame_delays = 1.0 / fps;
    AVFrame *avFrame = 0;
    uint8_t *dst_data[4];
    int dst_linsize[4];
    av_image_alloc(dst_data,dst_linsize,avCodecContext->width,
                   avCodecContext->height,AV_PIX_FMT_RGBA,1);
    while (isPlaying){
        int ret = avframes.pop(avFrame);
        if (!isPlaying){
            break;
        }
        sws_scale(swsContext, (const uint8_t *const *) avFrame->data,
                  avFrame->linesize,0,
                  avCodecContext->height,
                  dst_data,
                  dst_linsize);

#if 1
        //获得 当前这一个画面 播放的相对的时间
        double clock = avFrame->best_effort_timestamp * av_q2d(time_base);
        //额外的间隔时间
        double extra_delay = avFrame->repeat_pict / (2 * fps);
        // 真实需要的间隔时间
        double delays = extra_delay + frame_delays;
        if (!audioChannel){
            //休眠
            av_usleep(delays*1000000);
        } else {
            if (clock == 0) {
                av_usleep(delays*1000000);
            } else {
                //比较音频与视频
                double audioClock = audioChannel->clock;
                //间隔
                double diff = clock - audioClock;
                if (diff > 0){
                    LOGE("视频快了：%lf", diff);
                    if (diff < 3){
                        av_usleep((delays + diff)*1000000);
                    }
                } else if (diff < 0){
                    LOGE("音频快了：%lf", diff);
                    //不睡了 快点赶上 音频
                    // 视频包积压的太多了 （丢包）
                    if (fabs(diff) >= 0.05) {
                        releaseAvFrame(&avFrame);
                        avframes.sync();
                        continue;
                    } else{

                    }
                }
            }
        }

#endif
        callback(dst_data[0],dst_linsize[0],avCodecContext->width,avCodecContext->height);
        releaseAvFrame(&avFrame);
    }
    LOGD("VideoChannel stop--6");
    av_freep(&dst_data[0]);
    releaseAvFrame(&avFrame);
    isPlaying = 0;
    sws_freeContext(swsContext);
    swsContext = 0;

}

void VideoChannel::setRenderFramCallback(RenderFramCallback framCallback) {
    this->callback = framCallback;
}

void VideoChannel::stop() {
    LOGD("VideoChannel stop--开始");
    isPlaying = 0;
    frame.setWork(0);
    packagets.setWork(0);
    pthread_join(pid_decode, 0);
    pthread_join(pid_render, 0);
    LOGD("VideoChannel stop--完成");

}
