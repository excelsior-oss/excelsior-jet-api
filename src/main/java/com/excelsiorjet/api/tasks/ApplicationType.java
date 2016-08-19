/*
 * Copyright (c) 2016, Excelsior LLC.
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
package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.util.Utils;

/**
 * Supported application types enumeration.
 */
public enum ApplicationType {

    /**
     * Plain Java application, that runs standalone.
     */
    PLAIN,

    /**
     * Servlet-based Java application, that runs within Tomcat servlet container.
     */
    TOMCAT,

    /**
     * Dynamic library callable from a non-Java environment.
     */
    INVOCATION_DYNAMIC_LIBRARY,

    /**
     * Windows service (Windows only).
     */
    WINDOWS_SERVICE
    ;

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static ApplicationType fromString(String appType) {
        try {
            return ApplicationType.valueOf(Utils.parameterToEnumConstantName(appType));
        } catch (Exception e) {
            return null;
        }
    }


}
