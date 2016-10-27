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
 * Host platform (OS + CPU) utility methods.
 *
 * @author Nikita Lipsky
 */
public class Host {

    private static final OS hostOS;

    static {
        //detect Host OS
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            hostOS = OS.WINDOWS;
        } else if (osName.contains("Linux")) {
            hostOS = OS.LINUX;
        } else if (osName.contains("OS X")) {
            hostOS = OS.OSX;
        } else {
            throw new AssertionError("Unknown OS: " + osName);
        }
    }

    public static boolean isWindows() {
        return hostOS.isWindows();
    }

    public static boolean isLinux() {
        return hostOS.isLinux();
    }

    public static boolean isOSX() {
        return hostOS.isOSX();
    }

    public static boolean isUnix() {
        return hostOS.isUnix();
    }

    public static String mangleExeName(String exe) {
        return hostOS.mangleExeName(exe);
    }

    public static OS getOS() {
        return hostOS;
    }
}
