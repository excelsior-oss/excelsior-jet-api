/*
 * Copyright (c) 2015-2017, Excelsior LLC.
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
package com.excelsiorjet.api.tasks.config.compiler;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.JetBuildTask;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.TestRunTask;
import com.excelsiorjet.api.util.Utils;

import java.io.File;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Execution profiles configuration parameters.
 *
 * Currently, the Excelsior JET Maven and Gradle plugins collect execution profiles during special
 * plugins tasks: Test Run (jet:testrun task for Maven, jetTestRun task for Gradle)
 * or/and Profile Run (jet:profile task for Maven, jetProfile task for Gradle).
 *
 * Test Run collects startup profile (.startup file) and reflective-access profile (.usg file) that are used
 * by the Startup Optimizer and the Global Optimizer consequently.
 *
 * Profile Run collects execution profile that is used to perform profile guided optimizations.
 *
 * @author Nikita Lipsky
 */
public class ExecProfilesConfig {

    private static final String PROFILE_DIR = "appToProfile";

    private File usg;
    private File startup;
    private File jprofile;

    /**
     * The target location for application execution profiles gathered during Test Run and Profile tasks.
     * It is recommended to commit the collected profiles (.usg, .startup) to VCS to enable the {@link JetBuildTask}
     * to re-use them during subsequent builds without performing a Test Run or/and Profile.
     *
     * By default, {@link JetProject#jetResourcesDir} is used for the directory.
     *
     * @see TestRunTask
     */
    public File outputDir;

    /**
     * The base file name of execution profiles. By default, {@link JetProject#projectName} is used.
     */
    public String outputName;

    /**
     * In certain cases, you may need to run jet compiled binaries not on a computer where the build is performed
     * but on another computer with a special configured environment.
     * For this case, you may tell the plugin to create a special profile image that you will deploy to such environment
     * to collect execution profile by setting this parameter to {@code false}.
     *
     * You may also set the {@code jet.create.profile.image} system property to force the Profile task to create
     * such an image instead of running generated binary locally.
     *
     * Note, that the parameter is always {@code false} for cross-compilation Excelsior JET flavors (Linux ARM).
     */
    public Boolean profileLocally;

    /**
     * Directory with a special "profile" image that is used to collect execution profile that can be used
     * to enable profile guided optimizations.
     *
     * The value is set to "appToProfile" subdirectory of {@link JetProject#jetOutputDir} by default.
     *
     * If {@link #profileLocally} is set to {@code false} then a zip archive near to the directory will be created
     * with the same name that you can deploy to a target system to perform profiling.
     */
    public File profileDir;

    /**
     * It is recommended to re-collect the profiles periodically as your code base evolve.
     * The plugins issues a warning if you use outdated profiles for your builds.
     * With this parameter you can configure how old in days the profiles are treated as outdated.
     * You may set the parameter to {@code 0} to disable the warning.
     * The default value is 30 days.
     */
    public int daysToWarnAboutOutdatedProfiles = 30;

    public void fillDefaults(JetProject jetProject, ExcelsiorJet excelsiorJet) {
        if (outputDir == null) {
            outputDir = jetProject.jetResourcesDir();
        }
        if (Utils.isEmpty(outputName)) {
            outputName = jetProject.projectName();
        }

        if (excelsiorJet.isCrossCompilation())  {
            if (profileLocally != null) {
                logger.warn(s("JetApi.CannotProfileLocallyForCrossCompilation.Warning"));
            }
            profileLocally = false;
        } else if (System.getProperty("jet.create.profile.image") != null) {
            profileLocally = false;
        } else if (profileLocally == null) {
            profileLocally = true;
        }

        if (profileDir == null) {
            profileDir = new File(jetProject.jetOutputDir(), PROFILE_DIR);
        }

        usg = new File(outputDir, outputName + ".usg");
        startup = new File(outputDir, outputName + ".startup");
        jprofile = new File(outputDir, outputName + ".jprof");
    }


    public File getUsg() {
        return usg;
    }

    public File getStartup() {
        return startup;
    }

    public File getJProfile() {
        return jprofile;
    }
}
