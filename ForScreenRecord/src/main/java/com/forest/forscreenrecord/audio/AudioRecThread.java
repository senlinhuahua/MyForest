package com.forest.forscreenrecord.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class AudioRecThread extends Thread{

    private static final int SAMPLE_RATE_IN_HZ = 8000;
    private static final int CHANNEL_CONFIG = 16;
    private static final int AUDIO_FORMAT = 2;
    protected AudioRecord mAuRec = null;
    protected int m_in_buf_size;
    protected byte[] m_in_bytes;
    protected int m_Rec_Size = 0;
    private String savePcmPath;

    public AudioRecThread(String savePcmPath) {
        this.savePcmPath = savePcmPath;
    }

    @Override
    public void run() {
        initRec();
        if (mAuRec.getState() == 0) {
            AudioTool.m_Record_AudioRunFlag = false;
        } else {
            mAuRec.startRecording();
            if (this.checkPermission()) {
            } else {
                AudioTool.m_Record_AudioRunFlag = false;
            }

            while (AudioTool.m_Record_AudioRunFlag) {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException var2) {
                    var2.printStackTrace();
                }

                m_Rec_Size = mAuRec.read(this.m_in_bytes, 0, m_in_buf_size);
                if (AudioTool.getInstance().isSaveBytesToLoacl) {
                    getFileFromBytes(convert8kTo16k(this.m_in_bytes), savePcmPath);
                }
            }
            freeRec();
        }
    }

    private boolean checkPermission() {
        if (mAuRec.getRecordingState() != 1) {
            byte[] temp = new byte[this.m_in_buf_size];
            int testLen = 0;
            int count = 0;

            for(int i = 0; i < 10; ++i) {
                testLen += mAuRec.read(this.m_in_bytes, 0, this.m_in_buf_size);
                if (Arrays.equals(this.m_in_bytes, temp)) {
                    ++count;
                }
            }

            if (testLen > 0 && count != 10) {
                return true;
            }
        }

        return false;
    }

    public void initRec() {
        m_in_buf_size = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        mAuRec = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, m_in_buf_size);
        m_in_bytes = new byte[this.m_in_buf_size];
    }

    protected void freeRec() {
        mAuRec.stop();
        mAuRec.release();
        mAuRec = null;
    }
    public static File getFileFromBytes(byte[] b, String outputFile) {
        BufferedOutputStream stream = null;
        File file = null;

        try {
            file = new File(outputFile);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fstream = new FileOutputStream(file, true);
            stream = new BufferedOutputStream(fstream);
            stream.write(b);
        } catch (Exception var13) {
            var13.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException var12) {
                    var12.printStackTrace();
                }
            }

        }

        return file;
    }

    public static byte[] convert8kTo16k(byte[] orig) {
        byte[] dest = new byte[0];

        for(int j = 0; j < orig.length; j += 2) {
            byte[] byte2 = new byte[]{orig[j], orig[j + 1]};
            dest = append(dest, byte2);
            dest = append(dest, byte2);
        }

        return dest;
    }
    private static byte[] append(byte[] orig, byte[] dest) {
        byte[] newByte = new byte[orig.length + dest.length];
        System.arraycopy(orig, 0, newByte, 0, orig.length);
        System.arraycopy(dest, 0, newByte, orig.length, dest.length);
        return newByte;
    }
}
