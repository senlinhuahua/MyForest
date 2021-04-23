package com.forest.forscreenrecord.ffmpeg;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class FFcommandExecuteAsyncTask extends AsyncTask<Void, String, CommandResult> implements FFtask {
    private final String[] cmd;
    private Map<String, String> environment;
    private final FFcommandExecuteResponseHandler ffmpegExecuteResponseHandler;
    private final ShellCommand shellCommand;
    private final long timeout;
    private long startTime;
    private Process process;
    private String output = "";
    private boolean quitPending;

    FFcommandExecuteAsyncTask(String[] cmd, Map<String, String> environment, long timeout, FFcommandExecuteResponseHandler ffmpegExecuteResponseHandler) {
        this.cmd = cmd;
        this.timeout = timeout;
        this.environment = environment;
        this.ffmpegExecuteResponseHandler = ffmpegExecuteResponseHandler;
        this.shellCommand = new ShellCommand();
    }

    protected void onPreExecute() {
        this.startTime = System.currentTimeMillis();
        if (this.ffmpegExecuteResponseHandler != null) {
            this.ffmpegExecuteResponseHandler.onStart();
        }

    }

    protected CommandResult doInBackground(Void... params) {
        CommandResult var2;
        try {
            this.process = this.shellCommand.run(this.cmd, this.environment);
            if (this.process == null) {
                var2 = CommandResult.getDummyFailureResponse();
                return var2;
            }

            Log.d("Running publishing updates method");
            this.checkAndUpdateProcess();
            var2 = CommandResult.getOutputFromProcess(this.process);
        } catch (TimeoutException var8) {
            Log.e("FFmpeg binary timed out", var8);
            CommandResult var3 = new CommandResult(false, var8.getMessage());
            return var3;
        } catch (Exception var9) {
            Log.e("Error running FFmpeg binary", var9);
            return CommandResult.getDummyFailureResponse();
        } finally {
            Util.destroyProcess(this.process);
        }

        return var2;
    }

    protected void onProgressUpdate(String... values) {
        if (values != null && values[0] != null && this.ffmpegExecuteResponseHandler != null) {
            this.ffmpegExecuteResponseHandler.onProgress(values[0]);
        }

    }

    protected void onPostExecute(CommandResult commandResult) {
        if (this.ffmpegExecuteResponseHandler != null) {
            this.output = this.output + commandResult.output;
            if (commandResult.success) {
                this.ffmpegExecuteResponseHandler.onSuccess(this.output);
            } else {
                this.ffmpegExecuteResponseHandler.onFailure(this.output);
            }

            this.ffmpegExecuteResponseHandler.onFinish();
        }

    }

    private void checkAndUpdateProcess() throws TimeoutException, InterruptedException {
        while(!Util.isProcessCompleted(this.process)) {
            if (Util.isProcessCompleted(this.process)) {
                return;
            }

            if (this.timeout != 9223372036854775807L && System.currentTimeMillis() > this.startTime + this.timeout) {
                throw new TimeoutException("FFmpeg binary timed out");
            }

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));

                String line;
                while((line = reader.readLine()) != null) {
                    if (this.isCancelled()) {
                        this.process.destroy();
                        this.process.waitFor();
                        return;
                    }

                    if (this.quitPending) {
                        this.sendQ();
                        this.process = null;
                        return;
                    }

                    this.output = this.output + line + "\n";
                    this.publishProgress(new String[]{line});
                }
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

    }

    public boolean isProcessCompleted() {
        return Util.isProcessCompleted(this.process);
    }

    public boolean killRunningProcess() {
        return Util.killAsync(this);
    }

    public void sendQuitSignal() {
        this.quitPending = true;
    }

    private void sendQ() {
        OutputStream outputStream = this.process.getOutputStream();

        try {
            outputStream.write("q\n".getBytes());
            outputStream.flush();
        } catch (IOException var3) {
            var3.printStackTrace();
        }

    }
}
