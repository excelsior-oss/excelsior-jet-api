package com.excelsiorjet.api.tasks;

public class JetTaskFailureException extends Exception {

    public JetTaskFailureException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public JetTaskFailureException(String msg) {
        super(msg);
    }
}
