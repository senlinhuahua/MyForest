package com.forest.forscreenrecord.ffmpeg;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Util {
    Util() {
    }

    static boolean isDebug(Context context) {
        return (context.getApplicationContext().getApplicationInfo().flags & 2) != 0;
    }

    static String convertInputStreamToString(InputStream inputStream) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();

            String str;
            while((str = r.readLine()) != null) {
                sb.append(str);
            }

            return sb.toString();
        } catch (IOException var4) {
            Log.e("error converting input stream to string", var4);
            return null;
        }
    }

    static void destroyProcess(Process process) {
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception var2) {
                Log.e("progress destroy error", var2);
            }
        }

    }

    static boolean killAsync(AsyncTask asyncTask) {
        return asyncTask != null && !asyncTask.isCancelled() && asyncTask.cancel(true);
    }

    static boolean isProcessCompleted(Process process) {
        try {
            if (process == null) {
                return true;
            } else {
                process.exitValue();
                return true;
            }
        } catch (IllegalThreadStateException var2) {
            return false;
        }
    }

    static FFbinaryObserver observeOnce(final Util.ObservePredicate predicate, final Runnable run, final int timeout) {
        final Handler observer = new Handler();
        FFbinaryObserver observeAction = new FFbinaryObserver() {
            private boolean canceled = false;
            private int timeElapsed = 0;

            public void run() {
                if (this.timeElapsed + 40 > timeout) {
                    this.cancel();
                }

                this.timeElapsed += 40;
                if (!this.canceled) {
                    boolean readyToProceed = false;

                    try {
                        readyToProceed = predicate.isReadyToProceed();
                    } catch (Exception var3) {
                        Log.v("Observing " + var3.getMessage());
                        observer.postDelayed(this, 40L);
                        return;
                    }

                    if (readyToProceed) {
                        Log.v("Observed");
                        run.run();
                    } else {
                        Log.v("Observing");
                        observer.postDelayed(this, 40L);
                    }

                }
            }

            public void cancel() {
                this.canceled = true;
            }
        };
        observer.post(observeAction);
        return observeAction;
    }

    public interface ObservePredicate {
        Boolean isReadyToProceed();
    }
}
