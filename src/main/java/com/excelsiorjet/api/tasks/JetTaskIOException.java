package com.excelsiorjet.api.tasks;

import java.io.IOException;

/**
 * Unchecked IOException wrapper to throw in methods, that are called from lambdas
 */
class JetTaskIOException extends RuntimeException {

    JetTaskIOException(IOException cause) {
        super(cause);
    }

}
