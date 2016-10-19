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
package com.excelsiorjet.api;

import com.excelsiorjet.api.cmd.*;
import com.excelsiorjet.api.log.Log;
import com.excelsiorjet.api.util.Utils;

import java.io.File;

/**
 * This class represents a facade for the Excelsior JET toolchain.
 *
 * @author Aleksey Zhidkov
 */
public class ExcelsiorJet {

    private final JetHome jetHome;
    private final Log logger;

    public ExcelsiorJet(JetHome jetHome, Log logger) {
        this.jetHome = jetHome;
        this.logger = logger;
    }

    public ExcelsiorJet(String jetHome) throws JetHomeException {
        this(Utils.isEmpty(jetHome) ? new JetHome() : new JetHome(jetHome), Log.logger);
    }

    /**
     * Invokes the {@code jc} command line tool in the given {@code workingDirectory} with a logger specified at construction time, passing
     * {@code args} to it.
     *
     * @param workingDirectory working directory for {@code jc}. May be null, in which case the working directory of the current process is used.
     * @param args command line arguments that will be passed to {@code jc}.
     */
    public int compile(File workingDirectory, String... args) throws CmdLineToolException {
        return new JetCompiler(jetHome, args)
                .workingDirectory(workingDirectory)
                .withLog(logger)
                .execute();
    }

    /**
     * Invokes the {@code xpack} command line tool in the given {@code workingDirectory} with a logger specified at construction time, passing
     * {@code args} to it.
     *
     * @param workingDirectory working directory for {@code xpack}. May be null, in which case the working directory of the current process is used.
     * @param args command line arguments that will be passed to {@code xpack}.
     */
    public int pack(File workingDirectory, String... args) throws CmdLineToolException {
        return new JetPackager(jetHome, args)
                .workingDirectory(workingDirectory)
                .withLog(logger)
                .execute();
    }

    /**
     * Invokes the {@code xjava} command line tool in the given {@code workingDirectory} with a logger specified at construction time, passing
     * {@code args} to it.
     *
     * @param workingDirectory working directory for {@code xjava}. May be null, in which case the working directory of the current process is used.
     * @param args command line arguments that will be passed to {@code xjava}.
     */
    public int testRun(File workingDirectory, String... args) throws CmdLineToolException {
        return testRun(workingDirectory, logger, false, args);
    }

    /**
     * Invokes the {@code xjava} command line tool in the given {@code workingDirectory} with the specified {@code logger}, passing
     * {@code args} to it.
     *
     * @param workingDirectory working directory for {@code xjava}. May be null, in this case current process's working directory is used.
     * @param logger logger to which the output of {@code xjava} should be redirected.
     * @param errToOut specifies whether the stderr of xjava should be redirected with info level.
     * @param args command line arguments that will be passed to {@code xjava}.
     */
    public int testRun(File workingDirectory, Log logger, boolean errToOut, String... args) throws CmdLineToolException {
        return new XJava(jetHome, args)
                .workingDirectory(workingDirectory)
                .withLog(logger, errToOut)
                .execute();
    }

    /**
     * @return bitness of this Excelsior JET instance
     */
    public boolean is64bit() throws JetHomeException {
        return jetHome.is64bit();
    }

    /**
     * @return edition of this Excelsior JET instance
     */
    public JetEdition getEdition() throws JetHomeException {
        return jetHome.getEdition();
    }

    /**
     * @return home directory of this Excelsior JET instance
     */
    public JetHome getJetHome() {
        return jetHome;
    }
}
