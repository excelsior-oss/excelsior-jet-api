package com.excelsiorjet.api.tasks;

import java.io.IOException;

/**
 * Unchecked IOException wrapper to throw in methods, that are called from lambdas
 */
class JetTaskIoException extends RuntimeException {

    JetTaskIoException(IOException cause) {
        super(cause);
    }

}
