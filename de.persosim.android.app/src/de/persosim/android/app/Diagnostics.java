package de.persosim.android.app;

import android.util.Log;
import java.util.ArrayDeque;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.LogListener;
import org.globaltester.logging.Message;

public final class Diagnostics {
    private static final ArrayDeque<String> LINES = new ArrayDeque<>();
    private static boolean initialized;

    private Diagnostics() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        BasicLogger.addLogListener(new LogListener() {
            @Override public void log(Message message) {
                record(message.getMessageContent());
                for (org.globaltester.logging.tags.LogTag tag : message.getLogTags()) {
                    if (BasicLogger.EXCEPTION_STACK_TAG_ID.equals(tag.getId())) {
                        for (String detail : tag.getAdditionalData()) {
                            record(detail);
                            Log.e("PersoSim", detail);
                        }
                    }
                }
            }
        });
    }

    public static synchronized void record(String message) {
        if (message == null) return;
        LINES.addLast(message.length() > 600 ? message.substring(0, 600) : message);
        while (LINES.size() > 80) LINES.removeFirst();
    }

    public static void failure(String message, Exception error) {
        record(message + ": " + error.getMessage());
        Log.e("PersoSim", message, error);
    }

    public static synchronized String text() {
        StringBuilder result = new StringBuilder();
        for (String line : LINES) result.append(line).append('\n');
        return result.toString();
    }
}
