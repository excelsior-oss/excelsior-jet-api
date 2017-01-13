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

import com.excelsiorjet.api.tasks.ClasspathEntry.OptimizationType;
import com.excelsiorjet.api.tasks.ClasspathEntry.ProtectionType;
import com.excelsiorjet.api.util.Utils;

/**
 * Optimization presets enumeration.
 */
public enum OptimizationPreset {
    TYPICAL,
    SMART;

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static OptimizationPreset fromString(String preset) {
        try {
            return OptimizationPreset.valueOf(Utils.parameterToEnumConstantName(preset));
        } catch (Exception e) {
            return null;
        }
    }

    public ProtectionType getDefaultProtectionType(boolean forLibrary) {
        if (forLibrary && this == SMART) {
            return ProtectionType.NOT_REQUIRED;
        } else {
            return ProtectionType.ALL;
        }
    }

    public OptimizationType getDefaultOptimizationType(boolean forLibrary) {
        if (forLibrary && this == SMART) {
            return OptimizationType.AUTO_DETECT;
        } else {
            return OptimizationType.ALL;
        }
    }

}
