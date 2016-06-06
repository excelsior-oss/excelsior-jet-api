package com.excelsiorjet.api;

public abstract class AbstractLog {

    private static AbstractLog instance;

    public abstract void info(CharSequence msg);

    public abstract void warn(CharSequence msg);

    public abstract void warn(CharSequence msg, Throwable t);

    public abstract void error(CharSequence msg);

    public static void setInstance(AbstractLog instance) {
        AbstractLog.instance = instance;
    }

    public static AbstractLog instance() {
        return instance;
    }
}
