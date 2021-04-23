package com.forest.forscreenrecord.audio;

import java.io.ByteArrayOutputStream;
import java.net.Socket;

public class AudioTool {
    public static boolean m_bAudioRunFlag = false;
    public static Socket m_recvSocket = null;
    public static boolean m_isHeadsetPlug = false;
    public static boolean m_bResetFlag = false;
    public static Object m_voice_rec_obj = new Object();
    public static Object m_voice_play_obj = new Object();
    public static ByteArrayOutputStream m_EncodeData = new ByteArrayOutputStream();
    public static ByteArrayOutputStream m_UploadData = new ByteArrayOutputStream();
    public static ByteArrayOutputStream m_DecodeData = new ByteArrayOutputStream();
    public static int deyplay = 200;
    private static AudioTool instance = null;
    private boolean isSreenRecording;
    private long audioVideo_TimeOut = 5000L;
    public static boolean m_Record_AudioRunFlag = false;
    public boolean isSaveBytesToLoacl = true;
    public boolean isShoutDownMic = false;
    private String pcmParentDir;
    public long endTime;


    public static synchronized AudioTool getInstance() {
        if (instance == null) {
            instance = new AudioTool();
        }

        return instance;
    }


    public void startScreenRecordSound(String localPcmPath) {
        m_Record_AudioRunFlag = true;

        new Thread(new AudioRecThread(localPcmPath)).start();
    }
    public void stopScreenRecordSound() {
        m_Record_AudioRunFlag = false;
    }

    public void setSreenRecording(boolean sreenRecording) {
        isSreenRecording = sreenRecording;
    }
    public boolean isSreenRecording() {
        return this.isSreenRecording;
    }

}
