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

import java.io.File;

/**
 * Description of a file within resulting package.
 *
 * @author Nikita Lipsky
 */
public class PackageFile {

    /**
     * Path to the file on the host system.
     */
    public File path;

    /**
     * Location of the file in the resulting package.
     * If it is no set, the file will be added to the root installation directory.
     */
    public String packagePath;

    public PackageFile() {
    }

    public PackageFile(File path, String packagePath) {
        this.path = path;
        this.packagePath = packagePath;
    }
}
