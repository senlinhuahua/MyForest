#include <jni.h>
#include <string>
#include <android/native_window_jni.h>
#include "FoFFmpeg.h"
#include "macro.h"

FoFFmpeg *fFmpeg = 0;
JavaVM *javaVm = 0;
ANativeWindow *aNativewindow = 0;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
JavaCallHelper *helper = 0;


int JNI_OnLoad(JavaVM *vm, void *r){
    javaVm = vm;
    return JNI_VERSION_1_6;
}
//画
void render(uint8_t *data,int linesize, int w,int h){
    pthread_mutex_lock(&mutex);
    if (!aNativewindow){
        pthread_mutex_unlock(&mutex);
        return;
    }
    ANativeWindow_setBuffersGeometry(aNativewindow,w,h,WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer window_buffer;
    if(ANativeWindow_lock(aNativewindow,&window_buffer,0)){
        ANativeWindow_release(aNativewindow);
        aNativewindow = 0;
        pthread_mutex_unlock(&mutex);
        return;
    }
    //填充
    uint8_t *dst_data = (uint8_t *) window_buffer.bits;
    int dst_linsize = window_buffer.stride*4;
    for (int i = 0; i < window_buffer.height; ++i) {
        memcpy(dst_data + i * dst_linsize, data + i * linesize,dst_linsize);

    }
    ANativeWindow_unlockAndPost(aNativewindow);
    pthread_mutex_unlock(&mutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_forest_fffmpeg_FOPlayer_nativePrepare(JNIEnv *env, jobject instance,
                                                jstring dataSource_) {
    const char *dataSource = env->GetStringUTFChars(dataSource_, 0);


    //JavaCallHelper *callHelper =  new JavaCallHelper(javaVm, env, instance);
    //创建播放器
    helper = new JavaCallHelper(javaVm, env, instance);
    fFmpeg = new FoFFmpeg(helper,dataSource);
    fFmpeg->setRenderFremCallback(render);
    fFmpeg->prepare();

    env->ReleaseStringUTFChars(dataSource_, dataSource);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_forest_fffmpeg_FOPlayer_nativeStart(JNIEnv *env, jobject instance) {
    if (fFmpeg){
        fFmpeg->start();
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_com_forest_fffmpeg_FOPlayer_nativeSurface(JNIEnv *env, jobject instance, jobject surface) {

    pthread_mutex_lock(&mutex);
    // TODO
    if (aNativewindow){
        ANativeWindow_release(aNativewindow);
        aNativewindow=0;
    }
    aNativewindow = ANativeWindow_fromSurface(env,surface);
    pthread_mutex_unlock(&mutex);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_forest_fffmpeg_FOPlayer_nativeStop(JNIEnv *env, jobject thiz) {
    if (fFmpeg){
        fFmpeg->stop();
        fFmpeg = 0;
    }
    if (helper) {
        delete helper;
        helper = 0;
    }
    DELETE(helper);
    LOGD("NativeStop-----end");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_forest_fffmpeg_FOPlayer_nativeRelease(JNIEnv *env, jobject thiz) {
    // TODO: implement nativeRelease()
    pthread_mutex_lock(&mutex);
    if (aNativewindow) {
        //把老的释放
        ANativeWindow_release(aNativewindow);
        aNativewindow = 0;
    }
    pthread_mutex_unlock(&mutex);
    LOGD("NativeRelease-----end");
}