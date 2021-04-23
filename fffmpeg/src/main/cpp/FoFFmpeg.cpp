//
// Created by Administrator on 2018/11/16 0016.
//

#include <cstring>
#include "FoFFmpeg.h"

extern "C" {
#include <libavutil/time.h>
}

#include <pthread.h>
#include "macro.h"

void *task_pthread(void *args){
    //FoFFmpeg *fmpeg = (FoFFmpeg *) args;
    FoFFmpeg *fmpeg = static_cast<FoFFmpeg *> (args);
    fmpeg->_prepare();
    return 0;

}

FoFFmpeg::FoFFmpeg(JavaCallHelper *callHelper,const char *dataSource) {
    this->callHelper = callHelper;
    this->dataSource = new char[strlen(dataSource)+1];
    strcpy(this->dataSource,dataSource);

}

FoFFmpeg::~FoFFmpeg() {
    DELETE (dataSource);
    DELETE (callHelper);
}

void FoFFmpeg::prepare() {
    pthread_create(&pid,0,task_pthread,this);
}

void FoFFmpeg::_prepare() {
    //打开网络
    avformat_network_init();
//打开直播地址
    context = 0;
    int ret = avformat_open_input(&context,dataSource,0,0);
    if (ret){
        LOGE("打开地址失败：%s",av_err2str(ret));
        callHelper->onError(THREAD_CHILD,FFMPEG_CAN_NOT_OPEN_URL);
        return;
    }
//查找音视频流
    ret = avformat_find_stream_info(context,0);
    if (ret<0){
        //失败
        callHelper->onError(THREAD_CHILD,FFMPEG_CAN_NOT_FIND_STREAMS);
        return;
    }

    //几段数据流，视频，音频
    for (int i = 0; i < context->nb_streams; ++i) {
        AVStream *avStream = context->streams[i];
        AVCodecParameters *parameters = avStream->codecpar;

        //解码器
        AVCodec *dec = avcodec_find_decoder(parameters->codec_id);
        if (dec ==NULL){
            callHelper->onError(THREAD_CHILD,FFMPEG_FIND_DECODER_FAIL);
            return;
        }
        AVCodecContext *avCodecContext = avcodec_alloc_context3(dec);
        if (avCodecContext ==NULL){
            callHelper->onError(THREAD_CHILD,FFMPEG_ALLOC_CODEC_CONTEXT_FAIL);
            return;
        }
        //设置上下文的参数
        ret = avcodec_parameters_to_context(avCodecContext,parameters);
        if (ret<0){
            callHelper->onError(THREAD_CHILD,FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL);
            return;
        }

        //打开解码器
        ret = avcodec_open2(avCodecContext,dec,0);
        if (ret != 0){
            callHelper->onError(THREAD_CHILD,FFMPEG_OPEN_DECODER_FAIL);
            return;
        }
        AVRational time_base = avStream->time_base;
        //音频
        if (parameters->codec_type == AVMEDIA_TYPE_AUDIO){
            audioChannel = new AudioChannel(i,avCodecContext,time_base);

        } else if (parameters->codec_type == AVMEDIA_TYPE_VIDEO){

             //帧率
            AVRational frame_rate = avStream->avg_frame_rate;
            int fps = av_q2d(frame_rate);

            vadeoChannel = new VideoChannel(i, avCodecContext, time_base, fps);
            vadeoChannel->setRenderFramCallback(framCallback);
        }


    }
    if (audioChannel ==NULL && vadeoChannel == NULL){
        callHelper->onError(THREAD_CHILD,FFMPEG_NOMEDIA);
        return;
    }
    //准备完成，通知可以播放
    callHelper->onPrepare(THREAD_CHILD);
}

void *paly(void *args){
    //1,读取音视频包
    FoFFmpeg *fmpeg = (FoFFmpeg *) args;
    fmpeg->_start();
    //2，解码

    return 0;
}
void FoFFmpeg::start() {
    //正在播放
    isPalying = 1;

    //声音的解码与播放
    if (audioChannel){
        audioChannel->paly();
    }

    if (vadeoChannel){
        vadeoChannel->setAudioChannel(audioChannel);
        vadeoChannel->paly();
    }
    pthread_create(&pid_play,0,paly,this);

}

void FoFFmpeg::_start() {
    int  ret;
    while (isPalying){
        //读取文件的时候没有网络请求，一下子读完了，可能导致oom
        //特别是读本地文件的时候 一下子就读完了
        if (audioChannel && audioChannel->packagets.size() > 100) {
            //10ms
            av_usleep(1000 * 10);
            continue;
        }
        if (vadeoChannel && vadeoChannel->packagets.size() > 100) {
            av_usleep(1000 * 10);
            continue;
        }

        AVPacket *avPacket = av_packet_alloc();
        ret = av_read_frame(context,avPacket);
        //0,成功
        if (ret ==0){
            if (audioChannel && avPacket->stream_index == audioChannel->channelId){
                audioChannel->packagets.push(avPacket);
            } else if (vadeoChannel && avPacket->stream_index == vadeoChannel->channelId){
                vadeoChannel->packagets.push(avPacket);

            }
        } else if (ret == AVERROR_EOF ){
            //读取完成 但是可能还没播放完
            if (audioChannel->packagets.empty() && audioChannel->avframes.empty()
                && vadeoChannel->packagets.empty() && vadeoChannel->avframes.empty()) {
                break;
            }
            //为什么这里要让它继续循环 而不是sleep
            //如果是做直播 ，可以sleep
            //如果要支持点播(播放本地文件） seek 后退
        } else{
            break;

        }
    }
    isPalying = 0;
    audioChannel->stop();
    vadeoChannel->stop();
}

void FoFFmpeg::setRenderFremCallback(RenderFramCallback callback) {
    this->framCallback = callback;

}


void *aync_stop(void *args) {
    FoFFmpeg *ffmpeg = static_cast<FoFFmpeg *>(args);
    //   等待prepare结束
    pthread_join(ffmpeg->pid, 0);
    // 保证 start线程结束
    pthread_join(ffmpeg->pid_play, 0);
    DELETE(ffmpeg->vadeoChannel);
    DELETE(ffmpeg->audioChannel);
    // 这时候释放就不会出现问题了
    if (ffmpeg->context) {
        //先关闭读取 (关闭fileintputstream)
        avformat_close_input(&ffmpeg->context);
        avformat_free_context(ffmpeg->context);
        ffmpeg->context = 0;
    }
    DELETE(ffmpeg);
    return 0;
}


void FoFFmpeg::stop() {
    isPalying=0;
    callHelper = 0;
//    if (audioChannel) {
//        audioChannel->callHelper = 0;
//    }
//    if (videoChannel) {
//        videoChannel->javaCallHelper = 0;
//    }
    pthread_create(&pid_stop,0,aync_stop,this);
}
