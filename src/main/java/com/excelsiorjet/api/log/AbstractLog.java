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
package com.excelsiorjet.api.log;

/**
 * Class that abstracts logging system of specific build tool.
 * @author Aleksey Zhidkov
 */
public abstract class AbstractLog {

    private static AbstractLog instance;

    /**
     * Should print given msg and exception with debug level
     */
    public abstract void debug(String msg, Throwable t);

    /**
     * Should print given msg with info level
     */
    public abstract void info(String msg);

    /**
     * Should print given msg with warn level
     */
    public abstract void warn(String msg);

    /**
     * Should print given msg and exception with warn level
     */
    public abstract void warn(String msg, Throwable t);

    /**
     * Should print given msg with error level
     */
    public abstract void error(String msg);

    /**
     * Should be call by build tool entry point before any calls to tasks or cmd line tools
     * @param instance
     */
    public static void setInstance(AbstractLog instance) {
        AbstractLog.instance = instance;
    }

    public static AbstractLog instance() {
        return instance;
    }
}
