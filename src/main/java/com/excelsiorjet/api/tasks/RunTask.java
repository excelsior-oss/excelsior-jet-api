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
package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.cmd.CmdLineTool;
import com.excelsiorjet.api.cmd.CmdLineToolException;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.excelsiorjet.api.log.Log.logger;

/**
 * Task for running a generated executable.
 */
public class RunTask {

    private final ExcelsiorJet excelsiorJet;
    private final JetProject project;

    public RunTask(ExcelsiorJet excelsiorJet, JetProject project) throws JetTaskFailureException {
        this.excelsiorJet = excelsiorJet;
        this.project = project;
    }

    /**
     * Runs the executable when the project is already validated (from other tasks).
     */
    public void run(File appDir) throws CmdLineToolException, JetTaskFailureException {
        String[] args = Utils.prepend(new File(appDir, project.exeRelativePath(excelsiorJet)).getAbsolutePath(),
                project.exeRunArgs());

        String cmdLine = Arrays.stream(args)
                .map(Utils::quoteCmdLineArgument)
                .collect(Collectors.joining(" "));

        logger.info(Txt.s("RunTask.Start.Info", cmdLine));

        RunStopSupport runStopSupport = new RunStopSupport(project.jetOutputDir(), false);

        File termFile = runStopSupport.prepareToRunTask();

        int errCode = new CmdLineTool(args)
                .workingDirectory(appDir)
                .withLog(logger)
                .withEnvironment("JETVMPROP", project.getTerminationVMProp(termFile))
                .execute();

        runStopSupport.taskFinished();

        String finishText = Txt.s("RunTask.Finish.Info", errCode);
        if (errCode != 0) {
            logger.warn(finishText);
        } else {
            logger.info(finishText);
        }
    }

    public void execute() throws JetTaskFailureException, IOException, CmdLineToolException {
        if (excelsiorJet.isCrossCompilation()) {
            throw new JetTaskFailureException(Txt.s("RunTask.NoRunForCrossCompilation.Error"));
        }

        project.validate(excelsiorJet, true);

        switch (project.appType()) {
            case WINDOWS_SERVICE:
            case DYNAMIC_LIBRARY:
                throw new JetTaskFailureException(Txt.s("RunTask.AppTypeNotForRun.Error", project.appType()));
        }

        File appDir = project.jetAppDir();
        if (!new File(appDir, project.exeRelativePath(excelsiorJet)).exists()) {
            logger.info (new File(appDir, project.exeRelativePath(excelsiorJet)).getAbsolutePath());
            throw new JetTaskFailureException(Txt.s("RunTask.NoReadyBuild.Error"));
        }

        run(appDir);
    }

}
