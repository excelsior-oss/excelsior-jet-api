package com.excelsiorjet.api.tasks;

public class ExcelsiorJetApiException extends Exception {

    public ExcelsiorJetApiException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public ExcelsiorJetApiException(String msg) {
        super(msg);
    }
}
