package com.excelsiorjet.api.tasks.config.windowsservice;

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * @author kit
 *         Date: 02.02.2017
 */
public enum StartupType {
    AUTOMATIC("auto"),
    MANUAL("manual"),
    DISABLED("disabled");

    private String xpackValue;

    StartupType(String xpackValue) {
        this.xpackValue = xpackValue;
    }

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static StartupType validate(String startupType) throws JetTaskFailureException {
        try {
            return StartupType.valueOf(Utils.parameterToEnumConstantName(startupType));
        } catch (Exception e) {
            throw new JetTaskFailureException(s("JetApi.UnknownStartupType.Failure", startupType));
        }
    }

    public static StartupType fromString(String startupType) {
        try {
            return validate(startupType);
        } catch (Exception e) {
            throw new AssertionError("startupType should be valid here");
        }
    }

    public String toISrvCmdFlag() {
        return "-" + xpackValue;
    }

    public String toXPackValue() {
        return xpackValue;
    }
}
