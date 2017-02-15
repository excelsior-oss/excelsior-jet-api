package com.excelsiorjet.api.tasks.config.windowsservice;

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * @author kit
 *         Date: 02.02.2017
 */
public enum LogOnType {
    LOCAL_SYSTEM_ACCOUNT,
    USER_ACCOUNT;

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static LogOnType validate(String logOnType) throws JetTaskFailureException {
        try {
            return LogOnType.valueOf(Utils.parameterToEnumConstantName(logOnType));
        } catch (Exception e) {
            throw new JetTaskFailureException(s("JetApi.UnknownLogOnType.Failure", logOnType));        }
    }

    public static LogOnType fromString(String logOnType) {
        try {
            return validate(logOnType);
        } catch (JetTaskFailureException e) {
            throw new AssertionError("logOnType should be valid here");
        }
    }
}
