package net.microtrash.wisperingtree.util;

public interface LoggerInterface {
    void log(String message);
    void log(String key, String value);

    void setOnLogListener(LoggerInterface logListener);
}
