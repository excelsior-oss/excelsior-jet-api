package com.excelsiorjet.api.log;

public abstract class AbstractLog {

    private static AbstractLog instance;

    public abstract void debug(String msg, Throwable t);

    public abstract void info(String msg);

    public abstract void warn(String msg);

    public abstract void warn(String msg, Throwable t);

    public abstract void error(String msg);

    public static void setInstance(AbstractLog instance) {
        AbstractLog.instance = instance;
    }

    public static AbstractLog instance() {
        return instance;
    }
}
