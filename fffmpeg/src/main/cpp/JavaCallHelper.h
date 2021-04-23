//
// Created by Administrator on 2018/11/19 0019.
//

#ifndef MYFFMPEG_JAVACALLHELPER_H
#define MYFFMPEG_JAVACALLHELPER_H


#include <jni.h>

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *vm, JNIEnv *env,jobject instance);
    ~JavaCallHelper();

    void onError(int therd,int errorCode);
    void onPrepare(int therd);

private:
    JavaVM *vm;
    JNIEnv *env;

    jobject instance;
    jmethodID methodid;
    jmethodID onPrepareid;
};


#endif //MYFFMPEG_JAVACALLHELPER_H
