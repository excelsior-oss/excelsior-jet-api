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
package com.excelsiorjet.api.platform;

/**
 * List of operating systems currently supported by Excelsior JET.
 *
 * @author Nikita Lipsky
 */
public enum OS {
    WINDOWS,
    LINUX,
    OSX;

    public boolean isWindows() {
        return this == WINDOWS;
    }

    public boolean isLinux() {
        return this == LINUX;
    }

    public boolean isOSX() {
        return this == OSX;
    }

    public boolean isUnix() {
        return this == LINUX ||  this == OSX;
    }

    public String getExeFileExtension() {
        if (isWindows()) {
            return ".exe";
        } else if (isUnix()) {
            return "";
        } else {
            throw new AssertionError("Unknown OS: " + this);
        }
    }

    public String getDllFileExtension() {
        switch (this) {
            case WINDOWS:   return ".dll";
            case LINUX:     return ".so";
            case OSX:       return ".dylib";
            default:
                throw new AssertionError("Unknown OS: " + this);
        }
    }

    public String getDllFilePrefix() {
        switch (this) {
            case WINDOWS:   return "";
            case LINUX:     return "lib";
            case OSX:       return "lib";
            default:
                throw new AssertionError("Unknown OS: " + this);
        }
    }


    public String mangleExeName(String exe) {
        return exe + getExeFileExtension();
    }

    public String mangleDllName(String dll, boolean addPrefix) {
        return (addPrefix ? getDllFilePrefix() : "") + dll + getDllFileExtension();
    }

    public String mangleDllName(String dll) {
        return mangleDllName(dll, true);
    }
}

