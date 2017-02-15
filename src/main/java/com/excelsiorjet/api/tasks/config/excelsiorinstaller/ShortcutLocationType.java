/*
 * Copyright (c) 2017, Excelsior LLC.
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
package com.excelsiorjet.api.tasks.config.excelsiorinstaller;

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * (Windows) Excelsior Installer shortcut locations enumeration.
 */
public enum ShortcutLocationType {
    PROGRAM_FOLDER,
    DESKTOP,
    START_MENU,
    STARTUP;

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static ShortcutLocationType validate(String location, String shortcutName) throws JetTaskFailureException {
        try {
            return ShortcutLocationType.valueOf(Utils.parameterToEnumConstantName(location));
        } catch (Exception e) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.UnknownShortcutLocationType", shortcutName, location));
        }
    }

    public static ShortcutLocationType fromString(String location) {
        try {
            return validate(location, "");
        } catch (JetTaskFailureException e) {
            throw new AssertionError("shortcutLocation should be valid here", e);
        }
    }
}
