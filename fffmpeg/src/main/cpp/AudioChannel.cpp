//
// Created by Administrator on 2018/11/19 0019.
//

#include "AudioChannel.h"

void* audiodecode(void *args){
    AudioChannel *audioChannel = (AudioChannel *) args;
    audioChannel->decode();
    return 0;
}
void *audiopaly(void *args){
    AudioChannel *audioChannel = (AudioChannel *) args;
    audioChannel->_paly();
    return 0;
}
AudioChannel::AudioChannel(int id,AVCodecContext *avCodecContext,AVRational time_base):BaseChannel(id,
        avCodecContext,time_base) {

    out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    out_samplesize = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
    out_sample_rate = 44100;
    //44100个16位 44100 * 2
    // 44100*(双声道)*(16位)
    data = static_cast<uint8_t *>(malloc(out_sample_rate * out_channels * out_samplesize));
    memset(data,0,out_sample_rate * out_channels * out_samplesize);
}

AudioChannel::~AudioChannel() {
    if(data){
        free(data);
        data = 0;
    }
}
void AudioChannel::paly() {

    isPlaying =1;
    packagets.setWork(1);
    avframes.setWork(1);
//0+输出声道+输出采样位+输出采样率+  输入的3个参数
    swrContext = swr_alloc_set_opts(0, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, out_sample_rate,
                                    avCodecContext->channel_layout, avCodecContext->sample_fmt,
                                    avCodecContext->sample_rate, 0, 0);
    //初始化
    swr_init(swrContext);
    //解码，
    pthread_create(&pid_audiodecode,0,audiodecode,this);
    //播放
    pthread_create(&pid_audioplay,0,audiopaly,this);
}

void AudioChannel::decode() {
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
        } else{

        }
        avframes.push(avFrame);
    }
    releaseAvPacket(&avPacket);
}


//返回获取的pcm数据大小
int AudioChannel::getPcm() {
    int data_size = 0;
    AVFrame *frame;
    int ret = avframes.pop(frame);
    if (!isPlaying) {
        if (ret) {
            releaseAvFrame(&frame);
        }
        return data_size;
    }
    //48000HZ 8位 =》 44100 16位
    //重采样
    // 假设我们输入了10个数据 ，swrContext转码器 这一次处理了8个数据
    // 那么如果不加delays(上次没处理完的数据) , 积压
    int64_t delays = swr_get_delay(swrContext,frame->sample_rate);
    // 将 nb_samples 个数据 由 sample_rate采样率转成 44100 后 返回多少个数据
    // 10  个 48000 = nb 个 44100
    // AV_ROUND_UP : 向上取整 1.1 = 2
    int64_t max_samples =  av_rescale_rnd(delays+frame->nb_samples,out_sample_rate,frame->sample_rate,AV_ROUND_UP);
    //上下文+输出缓冲区+输出缓冲区能接受的最大数据量+输入数据+输入数据个数
    //返回 每一个声道的输出数据个数
    int samples = swr_convert(swrContext, &data, max_samples, (const uint8_t **)frame->data, frame->nb_samples);
    //获得   samples 个   * 2 声道 * 2字节（16位）
    data_size =  samples * out_samplesize * out_channels;
    //获取 frame 的一个相对播放时间 （相对开始播放）
    // 获得 相对播放这一段数据的秒数
    clock = frame->pts * av_q2d(time_base);
    return data_size;
}

void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    AudioChannel *audioChannel = static_cast<AudioChannel *>(context);
    //获得pcm 数据 多少个字节 data
    int dataSize = audioChannel->getPcm();
    if(dataSize > 0 ){
        // 接收16位数据
        (*bq)->Enqueue(bq,audioChannel->data,dataSize);
    }
}

void AudioChannel::_paly() {
    //创建引擎并获取引擎接口
    SLresult lresult;
    lresult = slCreateEngine(&enginObject,0,NULL,0,NULL,NULL);
    //assert(SL_RESULT_SUCCESS != lresult);
    if (SL_RESULT_SUCCESS != lresult){
        return;
    }

    lresult = (*enginObject)->Realize(enginObject,SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != lresult){
        return;
    }
    lresult = (*enginObject)->GetInterface(enginObject,SL_IID_ENGINE,&engineItf);
    if (SL_RESULT_SUCCESS != lresult){
        return;
    }

    /**
     * 2、设置混音器
     */
    // 2.1 创建混音器SLObjectItf outputMixObject
    lresult = (*engineItf)->CreateOutputMix(engineItf, &outputMixObject, 0,
                                                 0, 0);
    if (SL_RESULT_SUCCESS != lresult) {
        return;
    }
    // 2.2 初始化混音器outputMixObject
    lresult = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != lresult) {
        return;
    }

    /**
     * 3、创建播放器
     */
    //3.1 配置输入声音信息
    //创建buffer缓冲类型的队列 2个队列
    SLDataLocator_AndroidSimpleBufferQueue android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
                                                            2};
    //pcm数据格式
    //pcm+2(双声道)+44100(采样率)+ 16(采样位)+16(数据的大小)+LEFT|RIGHT(双声道)+小端数据
    SLDataFormat_PCM pcm = {SL_DATAFORMAT_PCM, 2, SL_SAMPLINGRATE_44_1, SL_PCMSAMPLEFORMAT_FIXED_16,
                            SL_PCMSAMPLEFORMAT_FIXED_16,
                            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
                            SL_BYTEORDER_LITTLEENDIAN};

    //数据源 将上述配置信息放到这个数据源中
    SLDataSource slDataSource = {&android_queue, &pcm};

    //3.2  配置音轨(输出)
    //设置混音器
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&outputMix, NULL};
    //需要的接口  操作队列的接口
    const SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    //3.3 创建播放器
    (*engineItf)->CreateAudioPlayer(engineItf, &bqPlayerObject, &slDataSource,
                                          &audioSnk, 1,
                                          ids, req);
    //初始化播放器
    (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);

    //得到接口后调用  获取Player接口
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerInterface);

    /**
    * 4、设置播放回调函数
    */
    //获取播放器队列接口
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
                                    &bqPlayerBufferQueueInterface);
    //设置回调
    (*bqPlayerBufferQueueInterface)->RegisterCallback(bqPlayerBufferQueueInterface,
                                                      bqPlayerCallback, this);
    /**
     * 5、设置播放状态
     */
    (*bqPlayerInterface)->SetPlayState(bqPlayerInterface, SL_PLAYSTATE_PLAYING);
    /**
     * 6、手动激活一下这个回调
     */
    bqPlayerCallback(bqPlayerBufferQueueInterface, this);
}

//add new
void AudioChannel::stop() {
    LOGD("AudioChannel stop--开始释放");
    isPlaying = 0;
    packagets.setWork(0);
    avframes.setWork(0);
    pthread_join(pid_audiodecode,0);
    pthread_join(pid_audioplay,0);
    if(swrContext){
        swr_free(&swrContext);
        swrContext = 0;
    }

    //释放播放器
    if(bqPlayerObject){
        (*bqPlayerObject)->Destroy(bqPlayerObject);
        bqPlayerObject = 0;
        bqPlayerInterface = 0;
        bqPlayerBufferQueueInterface = 0;
    }

    //释放混音器
    if(outputMixObject){
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = 0;
    }

    //释放引擎
    if(enginObject){
        (*enginObject)->Destroy(enginObject);
        enginObject = 0;
        engineItf = 0;
    }
    LOGD("AudioChannel stop--释放完成");
}
