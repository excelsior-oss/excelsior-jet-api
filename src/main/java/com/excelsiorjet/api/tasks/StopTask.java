/*
 * Copyright (c) 2018, Excelsior LLC.
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
import com.excelsiorjet.api.util.Txt;

import java.io.File;

import static com.excelsiorjet.api.log.Log.logger;

/**
 * Task for stopping a testrun/run/profile tasks.
 */
public class StopTask {

    private final ExcelsiorJet excelsiorJet;
    private final JetProject project;

    public StopTask(ExcelsiorJet excelsiorJet, JetProject project) throws JetTaskFailureException {
        this.excelsiorJet = excelsiorJet;
        this.project = project;
    }

    public void execute() throws JetTaskFailureException {
        if (excelsiorJet.isCrossCompilation()) {
            throw new JetTaskFailureException(Txt.s("RunTask.NoRunForCrossCompilation.Error"));
        }

        project.validate(excelsiorJet, true);

        switch (project.appType()) {
            case WINDOWS_SERVICE:
            case DYNAMIC_LIBRARY:
                throw new JetTaskFailureException(Txt.s("RunTask.AppTypeNotForRun.Error", project.appType()));
        }

        if (!new RunStopSupport(project.jetOutputDir(), true).stopRunTask()) {
            throw new JetTaskFailureException(Txt.s("StopTask.NoRunApp.Error", project.appType()));
        }
    }

}
