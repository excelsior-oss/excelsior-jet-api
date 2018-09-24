/*
 * Copyright (c) 2018, Excelsior LLC.
 *
 *  This file is part of Excelsior JET API.
 *
 *  Excelsior JET API is free software:
 *  you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Excelsior JET API is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Excelsior JET API.
 *  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * We support two termination policies: via sending Ctrl-C event to a running application or via calling
 * {@code java.lang.Shutdown.halt()} within a running application.
 */
public enum TerminationPolicy {
    CTRL_C,
    HALT;

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static TerminationPolicy validate(String terminationPolicy) throws JetTaskFailureException {
        try {
            return TerminationPolicy.valueOf(Utils.parameterToEnumConstantName(terminationPolicy));
        } catch (Exception e) {
            throw new JetTaskFailureException(s("JetApi.UnknownTerminationPolicy.Failure", terminationPolicy));
        }
    }

    public static TerminationPolicy fromString(String packaging) {
        try {
            return validate(packaging);
        } catch (JetTaskFailureException e) {
            throw new AssertionError("terminationPolicy should be valid here", e);
        }
    }

}
