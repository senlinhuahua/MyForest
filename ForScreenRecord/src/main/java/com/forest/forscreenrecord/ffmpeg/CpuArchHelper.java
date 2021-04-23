package com.forest.forscreenrecord.ffmpeg;

import android.os.Build;
import android.util.Log;

public class CpuArchHelper {
    private static final String X86_CPU = "x86";
    private static final String X86_64_CPU = "x86_64";
    private static final String ARM_64_CPU = "arm64-v8a";
    private static final String ARM_V7_CPU = "armeabi-v7a";

    public CpuArchHelper() {
    }

    public static CpuArch getCpuArch() {
        Log.d("Build.CPU_ABI : " , Build.CPU_ABI);
        String var0 = Build.CPU_ABI;
        byte var1 = -1;
        switch(var0.hashCode()) {
            case -806050265:
                if (var0.equals("x86_64")) {
                    var1 = 1;
                }
                break;
            case 117110:
                if (var0.equals("x86")) {
                    var1 = 0;
                }
                break;
            case 145444210:
                if (var0.equals("armeabi-v7a")) {
                    var1 = 3;
                }
                break;
            case 1431565292:
                if (var0.equals("arm64-v8a")) {
                    var1 = 2;
                }
        }

        switch(var1) {
            case 0:
                return CpuArch.x86;
            case 1:
                return CpuArch.x86_64;
            case 2:
                return CpuArch.ARM64;
            case 3:
                return CpuArch.ARMv7;
            default:
                return CpuArch.NONE;
        }
    }
}
