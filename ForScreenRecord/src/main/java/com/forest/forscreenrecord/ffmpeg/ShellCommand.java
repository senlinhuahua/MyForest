package com.forest.forscreenrecord.ffmpeg;

import java.util.Arrays;
import java.util.Map;

public class ShellCommand {
    ShellCommand() {
    }

    Process run(String[] commandString, Map<String, String> environment) {
        Process process = null;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandString);
            if (environment != null) {
                processBuilder.environment().putAll(environment);
            }

            process = processBuilder.start();
        } catch (Throwable var5) {
            Log.e("Exception while trying to run: " + Arrays.toString(commandString), var5);
        }

        return process;
    }
}
