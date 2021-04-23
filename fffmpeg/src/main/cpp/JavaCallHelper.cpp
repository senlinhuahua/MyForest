//
// Created by Administrator on 2018/11/19 0019.
//

#include "JavaCallHelper.h"
#include "macro.h"


JavaCallHelper::JavaCallHelper(JavaVM *vm, JNIEnv *env, jobject instance) {

    this->vm = vm;
    //如果在主线程调用
    this->env = env;
    //涉及到jobject跨线程创建全局
    this->instance = env->NewGlobalRef(instance);

    jclass  clz = env->GetObjectClass(instance);
    methodid = env->GetMethodID(clz,"onError","(I)V");
    onPrepareid = env->GetMethodID(clz,"onPrepare","()V");


}
JavaCallHelper::~JavaCallHelper() {
    env->DeleteGlobalRef(instance);
}

void JavaCallHelper::onError(int therd, int errorCode) {
    if (therd == THREAD_MAIN){
        env->CallVoidMethod(instance,methodid,errorCode);

    } else{
        //子线程，借助javaVm
        JNIEnv *env;
        vm->AttachCurrentThread(&env,0);
        env->CallVoidMethod(instance,methodid,errorCode);
        vm->DetachCurrentThread();
    }
}

void JavaCallHelper::onPrepare(int therd) {
    if (therd == THREAD_MAIN){
        env->CallVoidMethod(instance,onPrepareid);

    } else{
        //子线程，借助javaVm
        JNIEnv *env;
        vm->AttachCurrentThread(&env,0);
        env->CallVoidMethod(instance,onPrepareid);
        vm->DetachCurrentThread();
    }
}
