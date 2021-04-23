package com.forest.forscreenrecord.ffmpeg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class FFcommandExecuteSynchronous {
    private final String[] cmd;
    private Map<String, String> environment;
    private final ShellCommand shellCommand;
    private final long timeout;
    private long startTime;
    private Process process;
    private String output = "";

    FFcommandExecuteSynchronous(String[] cmd, Map<String, String> environment, long timeout) {
        this.cmd = cmd;
        this.timeout = timeout;
        this.environment = environment;
        this.shellCommand = new ShellCommand();
    }

    private CommandResult runCommand() {
        this.startTime = System.currentTimeMillis();

        try {
            process = shellCommand.run(cmd, environment);
            CommandResult commandResult;
            if (process == null) {
                commandResult = CommandResult.getDummyFailureResponse();
                return commandResult;
            }

            Log.d("Running publishing updates method");
            this.checkAndUpdateProcess();
            commandResult = CommandResult.getOutputFromProcess(this.process);
            return commandResult;
        } catch (TimeoutException var7) {
            Log.e("FFmpeg binary timed out", var7);
            CommandResult var2 = new CommandResult(false, var7.getMessage());
            return var2;
        } catch (Exception var8) {
            Log.e("Error running FFmpeg binary", var8);
        } finally {
            Util.destroyProcess(this.process);
        }

        return CommandResult.getDummyFailureResponse();
    }

    public boolean execute() {
        return this.runCommand().success;
    }

    private void checkAndUpdateProcess() throws TimeoutException {
        while(!Util.isProcessCompleted(this.process)) {
            if (Util.isProcessCompleted(this.process)) {
                return;
            }

            if (this.timeout != 9223372036854775807L && System.currentTimeMillis() > this.startTime + this.timeout) {
                throw new TimeoutException("FFmpeg binary timed out");
            }

            String line;
            try {
                for(BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getErrorStream())); (line = reader.readLine()) != null; this.output = this.output + line + "\n") {
                }
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

    }
}
