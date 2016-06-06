package com.excelsiorjet.api;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class StdOutLog extends AbstractLog {

    @Override
    public void info(CharSequence msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(CharSequence msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(CharSequence msg, Throwable t) {
        System.out.println(msg);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(buffer));
        System.out.printf(new String(buffer.toByteArray(), 0, buffer.size()));
    }

    @Override
    public void error(CharSequence msg) {
        System.err.println(msg);
    }

}
