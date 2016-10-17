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
import com.excelsiorjet.api.util.Utils;

import java.io.File;

/**
 * Common code for executors of Excelsior JET tools that reside in the [JET-Home]/bin directory.
 *
 * @author Nikita Lipsky
 */
class JetTool extends CmdLineTool {

    protected JetHome jetHome;

    private static String[] prependCommand(JetHome jetHome, String tool, String[] args) {
        String newArgs[] = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = Utils.mangleExeName(jetHome.getJetBinDirectory() + File.separator + tool);
        return newArgs;
    }

    public JetTool(JetHome jetHome, String tool, String... args) {
        super(prependCommand(jetHome, tool, args));
        this.jetHome = jetHome;
        String path = System.getenv("PATH");
        //place itself to the start of path
        path = jetHome.getJetBinDirectory() + File.pathSeparator + path;
        withEnvironment("PATH", path);
    }

    public JetTool(String exeName, String... args) throws JetHomeException {
        this(new JetHome(), exeName, args);
    }

}
