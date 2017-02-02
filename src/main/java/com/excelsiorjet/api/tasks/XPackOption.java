package com.excelsiorjet.api.tasks;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * xpack option representation.
 * It may be passed to xpack as separate arguments in the form of "option [parameters]" and
 * as a line in an xpack response file, unless {@code validForRspFile} is false.
 */
public class XPackOption {
    String option;
    String[] parameters;

    // Unfortunately some xpack options may contain a parameter that is list of arguments such as
    // "arg1 arg2 arg3". However if an argument has a space inside, there is no way to express such an argument
    // in an xpack response file in JET 11.3 and it should be passed to xpack directly.
    boolean validForRspFile;

    XPackOption(String option, String... parameters) {
        this(option, true, parameters);
    }

    XPackOption(String option, boolean validForRspFile, String... parameters) {
        this.option = option;
        this.parameters = parameters;
        this.validForRspFile = validForRspFile;
    }

    @Override
    public boolean equals(Object obj) {
        assert obj instanceof XPackOption;
        XPackOption option2 = (XPackOption) obj;
        return option.equals(option2.option) && Arrays.equals(parameters, option2.parameters);
    }

    String toArgFileLine() {
        String line = option + " " +
                Arrays.stream(parameters).map(p -> p.isEmpty() || p.contains(" ") ? "\"" + p + "\"" : p)
                        .collect(Collectors.joining(" "));
        return validForRspFile ? line : "#" + line;
    }
}
