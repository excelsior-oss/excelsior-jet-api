/*
 * Copyright (c) 2016-2017, Excelsior LLC.
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
package com.excelsiorjet.api.tasks.config.enums;

import com.excelsiorjet.api.util.Utils;

/**
 * Excelsior JET packaging types enumeration.
 */
public enum PackagingType {
    NONE,
    ZIP,
    EXCELSIOR_INSTALLER,
    OSX_APP_BUNDLE,
    NATIVE_BUNDLE;

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static PackagingType fromString(String packaging) {
        try {
            return PackagingType.valueOf(Utils.parameterToEnumConstantName(packaging));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isNativeBundle() {
        return (this == EXCELSIOR_INSTALLER) || (this == OSX_APP_BUNDLE) || (this == NATIVE_BUNDLE);
    }
}
