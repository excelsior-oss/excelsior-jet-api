/*
 * Copyright (c) 2016-2017, Excelsior LLC.
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
import com.excelsiorjet.api.log.StdOutLog;
import com.excelsiorjet.api.platform.CpuArch;
import com.excelsiorjet.api.platform.Host;
import com.excelsiorjet.api.platform.OS;
import com.excelsiorjet.api.tasks.config.runtime.RuntimeFlavorType;
import com.excelsiorjet.api.util.Txt;
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

    private JetEdition edition;
    private OS targetOS;
    private CpuArch targetCpu;

    public ExcelsiorJet(JetHome jetHome, Log logger) throws JetHomeException {
        this.jetHome = jetHome;
        this.logger = logger;
        detectEditionAndTargetPlatform();
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

    private String obtainVersionString() throws JetHomeException {
        try {
            String[] result = {null};
            CmdLineTool jetCompiler = new JetCompiler(this.jetHome).withLog(new StdOutLog() {
                public void info(String info) {
                    if (result[0] == null) {
                        if (info.contains("Excelsior JET ")) {
                            result[0] = info;
                        }
                    }
                }

            });
            if ((jetCompiler.execute() != 0) || result[0] == null)  {
                throw new JetHomeException(Txt.s("JetHome.UnableToDetectEdition.Error"));
            }
            return result[0];
        } catch (CmdLineToolException e) {
            throw new JetHomeException(e.getMessage());
        }
    }

    private void detectEditionAndTargetPlatform() throws JetHomeException {
        if (edition == null) {
            String version = obtainVersionString();
            edition = JetEdition.retrieveEdition(version);
            if (edition == null) {
                throw new JetHomeException(Txt.s("JetHome.UnableToDetectEdition.Error"));
            }

            targetOS = Host.getOS();

            if (version.contains("64-bit")) {
                targetCpu = CpuArch.AMD64;
            } else if (version.contains("ARM")) {
                targetCpu = CpuArch.ARM32;
                //currently Excelsior JET supports only ARM Linux
                targetOS = OS.LINUX;
            } else {
                targetCpu = CpuArch.X86;
            }
        }
    }

    public JetEdition getEdition() {
        return edition;
    }

    public OS getTargetOS()  {
        return targetOS;
    }

    private boolean isX86() {
        return targetCpu == CpuArch.X86;
    }

    public boolean isGlobalOptimizerSupported() {
        return (isX86() || since12_0()) && (getEdition() != JetEdition.STANDARD);
    }

    public boolean isSlimDownSupported() {
        return isGlobalOptimizerSupported() && isX86();
    }

    public boolean isUsageListGenerationSupported() {
        return isX86() || since12_0();
    }

    public boolean isStartupProfileGenerationSupported()  {
        return getEdition() != JetEdition.STANDARD;
    }

    private boolean isFullFeaturedEdition() {
        return (edition == JetEdition.EVALUATION) || (edition == JetEdition.ENTERPRISE);
    }

    private boolean isEmbedded() {
        return (edition == JetEdition.EMBEDDED) || (edition == JetEdition.EMBEDDED_EVALUATION);
    }

    public boolean isTomcatSupported() {
        JetEdition edition = getEdition();
        return  isFullFeaturedEdition() ||
                since11_3() && isEmbedded();
    }

    public boolean isSpringBootSupported() {
        JetEdition edition = getEdition();
        return  since16_0() &&
                (isFullFeaturedEdition() || isEmbedded());
    }

    public boolean isPDBConfigurationSupported() {
        return since15_0();
    }

    public boolean isSmartSupported() {
        return since15_0() && !isX86();
    }
    public boolean since11_3() {
        return jetHome.getJetVersion() >= 1130;
    }

    public boolean since12_0() {
        return jetHome.getJetVersion() >= 1200;
    }

    public boolean since15_0() {
        return jetHome.getJetVersion() >= 1500;
    }

    public boolean since16_0() {
        return jetHome.getJetVersion() >= 1600;
    }

    public boolean isCrossCompilation() {
        return targetOS != Host.getOS();
    }

    public boolean isTestRunSupported() {
        return !isCrossCompilation();
    }

    public boolean isExcelsiorInstallerSupported() {
        return !getTargetOS().isOSX() && !(isEmbedded());
    }

    public boolean isAdvancedExcelsiorInstallerFeaturesSupported() {
        return isExcelsiorInstallerSupported() && since11_3() && (edition != JetEdition.STANDARD);
    }

    public boolean isStartupAcceleratorSupported() {
        return !getTargetOS().isOSX() && !isCrossCompilation() && (edition != JetEdition.STANDARD);
    }

    public boolean isWindowsServicesSupported() {
        return targetOS.isWindows() && (edition != JetEdition.STANDARD);
    }

    public boolean isWindowsServicesInExcelsiorInstallerSupported() {
        return isWindowsServicesSupported() && isExcelsiorInstallerSupported() && since11_3();
    }

    public boolean isCompactProfilesSupported() {
        return since11_3() && (edition != JetEdition.STANDARD);
    }

    public boolean isMultiAppSupported() {
        return edition != JetEdition.STANDARD;
    }

    public boolean isTrialSupported() {
        return edition != JetEdition.STANDARD;
    }

    public boolean isDataProtectionSupported() {
        return edition != JetEdition.STANDARD;
    }

    public boolean isDiskFootprintReductionSupported() {
        return since11_3() && isGlobalOptimizerSupported();
    }

    public boolean isHighDiskFootprintReductionSupported() {
        return isDiskFootprintReductionSupported() && isX86();
    }

    public boolean isWindowsVersionInfoSupported() {
        return targetOS.isWindows() && (edition != JetEdition.STANDARD);
    }

    public boolean isRuntimeSupported(RuntimeFlavorType flavor) {
        switch (flavor) {
            case SERVER:
                return isFullFeaturedEdition() ||
                        (since11_3() && isEmbedded());
            case DESKTOP:
                return edition != JetEdition.STANDARD;
            case CLASSIC:
                return true;
            default:
                throw new AssertionError("Unknown runtime flavor:" + flavor);
        }
    }

    public boolean isChangeRTLocationAvailable() {
        return since11_3();
    }

    public boolean isPGOSupported() {
        return since12_0() && !isX86() && (edition != JetEdition.STANDARD) && (edition != JetEdition.PROFESSIONAL);
    }

    /**
     * @return home directory of this Excelsior JET instance
     */
    public String getJetHome() {
        return jetHome.getJetHome();
    }

}
