package com.example.wealth;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.formatters.PatternFormatter;

// Collects the lines written to the "audit" logger, so tests can assert on them.
public final class AuditLog extends Handler {

    private static final AuditLog INSTANCE = new AuditLog();

    static {
        Logger.getLogger("audit").addHandler(INSTANCE);
    }

    private final List<String> lines = new CopyOnWriteArrayList<>();
    private final PatternFormatter messageOnly = new PatternFormatter("%s");

    public static List<String> lines() {
        return INSTANCE.lines;
    }

    public static void clear() {
        INSTANCE.lines.clear();
    }

    @Override
    public void publish(LogRecord record) {
        lines.add(messageOnly.format(record));
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
