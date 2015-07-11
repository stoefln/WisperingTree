package net.microtrash.wisperingtree.util;

public class Protocol {
    public static final String COMMAND_START = "COMMAND>";
    public static final String COMMAND_SEND_FILE = "SEND_FILE";
    public static final String COMMAND_SEND_OBJECT = "SEND_OBJECT";
    public static final String SEPARATOR = ":";
    public static final Character COMMAND_END = ';';
    public static final long TRANSFER_DELAY_MS = 50;
}
