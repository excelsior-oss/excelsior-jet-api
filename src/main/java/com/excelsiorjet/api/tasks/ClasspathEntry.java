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

import java.io.File;

/**
 * Java application classpath entry.
 */
public class ClasspathEntry {

    private final File file;
    private final boolean isLib;

    public ClasspathEntry(File file, boolean isLib) {
        this.file = file;
        this.isLib = isLib;
    }

    /**
     * File that is pointed by classpath entry.
     */
    public File getFile() {
        return file;
    }

    /**
     * Flag indicating is classpath entry points to part of application (that should be protected and optimized, if requested)
     * or not.
     */
    public boolean isLib() {
        return isLib;
    }

}
