/*
 * Copyright (c) 2015, Excelsior LLC.
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
package com.excelsiorjet.api.cmd;

import com.excelsiorjet.api.JetHome;
import com.excelsiorjet.api.JetHomeException;

/**
 * Excelsior JET "xpack" tool executor utility class.
 *
 * @author Nikita Lipsky
 */
public class JetPackager extends JetTool {

    public static final String JET_PACKAGER = "xpack";

    public JetPackager(JetHome jetHome, String... args) {
        super(jetHome, JET_PACKAGER, args);
    }

    public JetPackager(String... args) throws JetHomeException {
        super(JET_PACKAGER, args);
    }
}
