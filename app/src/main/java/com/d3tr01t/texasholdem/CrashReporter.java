package com.d3tr01t.texasholdem;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class CrashReporter extends Application {
    private static volatile Thread.UncaughtExceptionHandler previousHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            saveThrowable(this, thread, error);
            if (previousHandler != null) previousHandler.uncaughtException(thread, error);
        });
    }

    static void saveReport(Context context, String message) {
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), "crash-last.txt"))) {
            out.write(message.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.getFD().sync();
        } catch (Exception ignored) { }
    }

    private static void saveThrowable(Context context, Thread thread, Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String report = "Texas Hold'em crash report\n"
            + "Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
            + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
            + "Thread: " + thread.getName() + "\n\n"
            + trace;
        saveReport(context, report);
    }
}
