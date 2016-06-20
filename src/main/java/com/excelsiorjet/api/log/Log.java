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
 * Abstraction of a logging system of a specific build tool.
 *
 * @author Aleksey Zhidkov
 */
public abstract class Log {

    public static Log logger;

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

}
