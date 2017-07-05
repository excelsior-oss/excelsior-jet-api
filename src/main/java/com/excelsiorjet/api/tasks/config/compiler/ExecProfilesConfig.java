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
import com.excelsiorjet.api.tasks.*;
import com.excelsiorjet.api.util.Utils;

import java.io.File;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.tasks.config.compiler.ExecProfilesExistenceType.ALL;
import static com.excelsiorjet.api.tasks.config.compiler.ExecProfilesExistenceType.PROFILE;
import static com.excelsiorjet.api.tasks.config.compiler.ExecProfilesExistenceType.TEST_RUN;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * <p>Application execution profiles configuration parameters.</p>
 * <p>
 * Currently, the Excelsior JET Maven and Gradle plugins collect application execution profiles
 * when one of the two special plugin tasks is invoked:
 * <b>Test Run</b> (Maven task {@code jet:testrun}, Gradle task {@code jetTestRun})
 * and/or <b>Profile</b> ({@code jet:profile} and {@code jetProfile}, respectively).
 * </p>
 * <p>
 * The Test Run task collects an application startup profile ({@code .startup} file) and reflective-access
 * profile ({@code .usg} file) prior to the native build. These profiles are used by the Startup Optimizer
 * and the Global Optimizer respectively.
 * </p>
 * <p>
 * The Profile task runs a natively compiled application to collect its execution profile, enabling
 * profile-guided optimization on subsequent builds.
 * </p>
 *
 * @author Nikita Lipsky
 */
public class ExecProfilesConfig {

    private static final String PROFILE_DIR = "appToProfile";

    /**
     * The target location for application execution profiles gathered by the Test Run and Profile tasks.
     * It is recommended to commit the collected profiles ({@code .usg}, {@code .startup}, {@code .jprof})
     * to the VCS to enable the {@link JetBuildTask} to re-use them during subsequent builds without performing
     * a Test Run and/or Profile task again. Once you have committed the profiles, it is recommended to set
     * the {@link #checkExistence} parameter to an appropriate value in order to verify that the profiles
     * are available during subsequent builds.
     * <p>
     * By default, {@link JetProject#jetResourcesDir} is used.
     * </p>
     * @see TestRunTask
     * @see RunTask
     */
    public File outputDir;

    /**
     * The base file name of all execution profiles. By default, {@link JetProject#projectName} is used.
     */
    public String outputName;

    /**
     * <p>
     * Whether to run the Profile task on the same machine or prepare an image for deployment
     * to a reference system.
     * </p>
     * <p>
     * It may be necessary to profile the natively compiled application on a computer other than the one
     * conducting the build, e.g. because profiling requires a specially configured environment.
     * Setting this parameter to {@code false} forces the plugin to create a special <em>profiling image</em>
     * that you can then deploy to such an environment to collect an application execution profile.
     * </p>
     * <p>
     * You can also set the {@code jet.create.profiling.image} system property to force the Profile task to create
     * such an image instead of running the generated binary locally.
     * </p>
     * <p>
     * Note that this parameter is always set to {@code false} for the cross-compiling flavors of Excelsior JET,
     * e.g. those targeting Linux/ARM.
     * </p>
     *
     * @see #profilingImageDir
     */
    public Boolean profileLocally;

    /**
     * <p>
     * Directory where the special "profiling" image of the natively compiled application has to be placed.
     * </p>
     * <p>
     * By default, points to the {@code "appToProfile"} subdirectory of {@link JetProject#jetOutputDir}.
     * </p>
     * <p>
     * To facilitate deployment of the profiling image to a reference system when {@link #profileLocally}
     * is set to {@code false}, the plugin also creates a zip archive that contains a copy of that image,
     * gives it the same base name and places it next to this directory.
     * </p>
     */
    public File profilingImageDir;

    /**
     * <p>
     * Profile validity threshold in days.
     * </p>
     * <p>
     * It is recommended to re-collect all profiles periodically as your code base evolves.
     * The plugins issue a warning upon detecting an outdated profile during a build.
     * With this parameter, you can adjust the respective threshold, measured in days,
     * or set it to {@code 0} to disable the warning. The default value is 30.
     * </p>
     */
    public int daysToWarnAboutOutdatedProfiles = 30;

    /**
     * Check that all or certain application profiles are available before starting a build.
     * Valid values are: "all" , "test-run", "profile", "none" (default):
     * <p>
     * {@code test-run} - profiles collected by the Test Run task (".usg", ".startup").
     * </p>
     * <p>
     * {@code profile} - application executiion profile collected by the Profile task (".jprof").
     * </p>
     * <p>
     * {@code all} - all profiles (".usg", ".startup", and ".jprof").
     * </p>
     */
    public String checkExistence = ExecProfilesExistenceType.NONE.toString();

    public void fillDefaults(JetProject jetProject, ExcelsiorJet excelsiorJet) throws JetTaskFailureException {
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
        } else if (System.getProperty("jet.create.profiling.image") != null) {
            profileLocally = false;
        } else if (profileLocally == null) {
            profileLocally = true;
        }

        if (profilingImageDir == null) {
            profilingImageDir = new File(jetProject.jetOutputDir(), PROFILE_DIR);
        }

        ExecProfilesExistenceType existenceType = ExecProfilesExistenceType.validate(checkExistence);
        if ((existenceType == ALL) || (existenceType == TEST_RUN)) {
            if (excelsiorJet.isUsageListGenerationSupported() && !getUsg().exists()) {
                throw new JetTaskFailureException(s("JetApi.NoTestRunProfile.Failure", getUsg().getAbsolutePath()));
            }
            if (excelsiorJet.isStartupProfileGenerationSupported() && !getStartup().exists()) {
                throw new JetTaskFailureException(s("JetApi.NoTestRunProfile.Failure", getStartup().getAbsolutePath()));
            }
        }
        if ((existenceType == ALL) || (existenceType == PROFILE)) {
            if (excelsiorJet.isPGOSupported() && !getJProfile().exists()) {
                throw new JetTaskFailureException(s("JetApi.NoJProfile.Failure", getJProfile().getAbsolutePath()));
            }
        }
    }


    public File getUsg() {
        return new File(outputDir, outputName + ".usg");
    }

    public File getStartup() {
        return new File(outputDir, outputName + ".startup");
    }

    public File getJProfile() {
        return new File(outputDir, outputName + ".jprof");
    }

}
