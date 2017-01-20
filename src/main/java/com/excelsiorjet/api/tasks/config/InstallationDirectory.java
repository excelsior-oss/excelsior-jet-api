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
package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.tasks.config.enums.InstallationDirectoryType;

import java.util.Objects;
import java.util.stream.Stream;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * Installation directory configuration.
 *
 * @author Nikita Lipsky
 */
public class InstallationDirectory {

    /**
     * Installation directory type. Valid values are:
     * {@code program-files} (default on Windows, Windows only), {@code system-drive} (Windows Only), {@code absolute-path},
     * {@code current-directory} (default on Linux), {@code user-home} (Linux only).
     * <p>
     * Specifies whether the default installation directory pathname is relative to the Program Files folder,
     * is relative to the root of the system drive, is an absolute path, is relative to the curent directory, or
     * is relative to the user home directory respectively.
     * </p>
     */
    public String type;

    /**
     * Default installation directory.
     * <p>
     * The full or partial pathname (depending on {@link #type}) of the desired installation directory.
     * If this parameter is not explicitly specified, its value is derived from the values of {@link JetProject#vendor},
     * {@link JetProject#product} and {@link JetProject#version} parameters as follows:
     * {@code company-name\product-name[ product-version]}
     * </p>
     */
    public String path;

    /**
     * Prohibits changes of the installation directory, if set to {@code true}.
     */
    public boolean fixed;

    public boolean isEmpty() {
        return Stream.of(type, path).allMatch(Objects::isNull) && !fixed;
    }

    void validate(ExcelsiorJet excelsiorJet) throws JetTaskFailureException {
        if (type != null) {
            InstallationDirectoryType dirType = InstallationDirectoryType.fromString(type);
            if (dirType == null) {
                throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.UnknownInstallationDirectoryType", type));
            } else {
                switch (dirType) {
                    case PROGRAM_FILES:
                    case SYSTEM_DRIVE:
                        if (!excelsiorJet.getTargetOS().isWindows()) {
                            throw new JetTaskFailureException(
                                    s("JetApi.ExcelsiorInstaller.SpecificOSInstallationDirectoryType", type, "Windows"));
                        }
                        break;
                    case USER_HOME:
                        if (!excelsiorJet.getTargetOS().isLinux()) {
                            throw new JetTaskFailureException(
                                    s("JetApi.ExcelsiorInstaller.SpecificOSInstallationDirectoryType", type, "Linux"));
                        }
                        break;
                }
            }
        }
    }

}
