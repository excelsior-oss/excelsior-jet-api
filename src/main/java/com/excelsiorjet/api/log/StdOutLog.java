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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Implementation of {@code {@link Log }} that prints messages into standard out and error streams
 * @author Aleksey Zhidkov
 */
public class StdOutLog extends Log {

    @Override
    public void debug(String msg, Throwable t) {
        System.out.println(msg);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(buffer));
        System.out.printf(new String(buffer.toByteArray(), 0, buffer.size()));
    }

    @Override
    public void info(String msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(String msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        System.out.println(msg);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(buffer));
        System.out.printf(new String(buffer.toByteArray(), 0, buffer.size()));
    }

    @Override
    public void error(String msg) {
        System.err.println(msg);
    }

}
