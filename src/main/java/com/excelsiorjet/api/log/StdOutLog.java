package com.excelsiorjet.api.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class StdOutLog extends AbstractLog {

    @Override
    public void debug(String msg, Throwable t) {
        System.out.println(msg);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(buffer));
        System.out.printf(new String(buffer.toByteArray(), 0, buffer.size()));
    }

    @Override
    public void info(String msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(String msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        System.out.println(msg);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(buffer));
        System.out.printf(new String(buffer.toByteArray(), 0, buffer.size()));
    }

    @Override
    public void error(String msg) {
        System.err.println(msg);
    }

}
