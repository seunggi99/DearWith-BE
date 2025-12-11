package com.dearwith.dearwith_backend.logging.context;

public class LogContextHolder {

    private static final ThreadLocal<LogContext> CONTEXT = new ThreadLocal<>();

    public static void set(LogContext ctx) {
        CONTEXT.set(ctx);
    }

    public static LogContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}