package com.forest.forscreenrecord.ffmpeg;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String FFMPEG_FILE_NAME = "ffmpeg";

    FileUtils() {
    }

    static File getFFmpeg(Context context) {
        File folder = context.getFilesDir();
        return new File(folder, "ffmpeg");
    }

    static void copyFile(File inputFile, File outputFile) throws IOException {
        InputStream input = new FileInputStream(inputFile);
        OutputStream output = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];

        int bytesRead;
        while((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
            output.write(buffer, 0, bytesRead);
        }

        output.flush();
        output.close();
        input.close();
    }

    static void copyFile(InputStream input, File outputFile) throws IOException {
        OutputStream output = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];

        int bytesRead;
        while((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
            output.write(buffer, 0, bytesRead);
        }

        output.flush();
        output.close();
        input.close();
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();

            for(int i = 0; i < children.length; ++i) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }
}
