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

import com.excelsiorjet.api.tasks.JetProject;

/**
 * (Winodws) Shortcut description.
 *
 * @author Nikita Lipsky
 */
public class Shortcut {

    /**
     * Shortcut location.
     * Valid values are {@code program-folder}, {@code desktop}, {@code start-menu} or {@code startup}.
     */
    public String location;

    /**
     * Location of the shortcut target within the package.
     */
    public String target;

    /**
     * Name of the shortcut. By default, short name of the target is used.
     */
    public String name;

    /**
     * The location of the shortcut icon.
     * You may omit {@link PackageFile#path} parameter of the icon,
     * if {@link JetProject#packageFilesDir} already contains an icon at the specified {@link PackageFile#packagePath}
     * parameter else the icon will be added to the package at the specified location and used for the shortcut.
     * If the icon is not set for the shortcut the default icon will be used
     * (f.i. the icon associated with the executable target).
     */
    public PackageFile icon;

    /**
     * Pathname of the working directory of the shortcut target within the package.
     * If it is not set, the the directory containing target will be used.
     */
    public String workingDirectory;

    /**
     * Command line arguments for the target.
     */
    public String[] arguments;
}
