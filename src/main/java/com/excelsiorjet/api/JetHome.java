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
package com.excelsiorjet.api;

import com.excelsiorjet.api.cmd.JetCompiler;
import com.excelsiorjet.api.cmd.JetPackager;
import com.excelsiorjet.api.platform.Host;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;

/**
 * Encapsulates the Excelsior JET home directory.
 * 
 * @author Nikita Lipsky
 */
public class JetHome {

    private static final int MIN_SUPPORTED_JET_VERSION = 1100;

    private static final String MARKER_FILE_PREFIX = "jet";
    private static final String MARKER_FILE_SUFFIX = ".home";

    private static final String BIN_DIR = "bin";

    private String jetHome;
    private int jetVersion;

    /**
     * @param jetHome Excelsior JET home directory
     * @return Excelsior JET version "multiplied by 100" (i.e. 1150 means version 11.5),
     *         or -1 if {@code jetHome} does not point to an Excelsior JET home directory
     */
    private static int getJetVersion(String jetHome) {
        File[] files = new File(jetHome, BIN_DIR).listFiles();
        if (files == null) {
            return -1;
        }
        for (File f : files) {
            String fname = f.getName();
            if (fname.startsWith(MARKER_FILE_PREFIX) && fname.endsWith(MARKER_FILE_SUFFIX)) {
                try {
                     // expected file name: jet<version>.home
                    return Integer.parseInt(fname.substring(MARKER_FILE_PREFIX.length(), fname.length() - MARKER_FILE_SUFFIX.length()));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static boolean isSupportedJetVersion(int jetVersion) {
        return jetVersion >= MIN_SUPPORTED_JET_VERSION;
    }

    private void checkAndSetJetHome(String jetHome, String errorPrefix) throws JetHomeException {
        if (!isJetDir(jetHome)) {
            throw new JetHomeException(Txt.s("JetHome.BadJETHomeDir.Error", errorPrefix, jetHome));
        }
        this.jetVersion = getJetVersion(jetHome);
        if (!isSupportedJetVersion(this.jetVersion)) {
            throw new JetHomeException(Txt.s("JetHome.UnsupportedJETHomeDir.Error", errorPrefix, jetHome));
        }
        this.jetHome = jetHome;
    }

    /**
     * Constructs a JetHome object given a filesystem location supposedly containing a copy of Excelsior JET
     * 
     * @param jetHome Excelsior JET home directory pathname
     * @throws JetHomeException if {@code jetHome} does not point to a supported version of Excelsior JET
     */
    public JetHome(String jetHome) throws JetHomeException {
        if (Host.isUnix() && jetHome.startsWith("~/")) {
            // expand "~/" on Unixes
            jetHome = System.getProperty("user.home") + jetHome.substring(1);
        }
        checkAndSetJetHome(jetHome, Txt.s("JetHome.PluginParameter.Error.Prefix"));
    }

    private boolean trySetJetHome(String jetHome, String errorPrefix) throws JetHomeException {
        if (!Utils.isEmpty(jetHome)) {
            checkAndSetJetHome(jetHome, errorPrefix);
            return true;
        }
        return false;
    }

    /**
     * <p>Attempts to locate an Excelsior JET home directory automatically.</p>
     * <ul>
     *   <li> If the {@code jet.home} system property is set,
     *        check that it points to a suitable Excelsior JET installation</li>
     *   <li> Otherwise, if the {@code JET_HOME} environment variable is set,
     *        check that it points to a suitable Excelsior JET installation</li>
     *   <li> Otherwise scan the {@code PATH} environment variable
     *        for a suitable Excelsior JET installation</li>
     * </ul>
     * @throws JetHomeException if either {@code jet.home} or {@code JET_HOME} is set, but does not point to a suitable
     *                          Excelsior JET installation, or no such installation could be found along the {@code PATH}
     */
    public JetHome() throws JetHomeException {
        // try to detect jet home
        if (!trySetJetHome(System.getProperty("jet.home"), Txt.s("JetHome.ViaVMProp.Error.Prefix"))
                && !trySetJetHome(System.getenv("JET_HOME"), Txt.s("JetHome.ViaEnvVar.Error.Prefix"))) {
            // try to detect jetHome via path
            String path = System.getenv("PATH");
            for (String p : path.split(File.pathSeparator)) {
                if (isJetBinDir(p)) {
                    String jetPath = new File(p).getParentFile().getAbsolutePath();
                    jetVersion = getJetVersion(jetPath);
                    if (isSupportedJetVersion(jetVersion)) {
                        jetHome = jetPath;
                        return;
                    }
                }
            }
            throw new JetHomeException(Txt.s("JetHome.JetNotFound.Error"));
        }
    }

    public String getJetHome() {
        return jetHome;
    }

    public String getJetBinDirectory() {
        return getJetBinDirectory(getJetHome());
    }

    /**
     * @return Excelsior JET version, where first two digits is major version and last two digits is minor version.
     *         Thus for Excelsior JET version 11.3, the returned value will be 1130.
     */
    public int getJetVersion() {
        return jetVersion;
    }

    private static boolean isJetBinDir(String jetBin) {
        return new File(jetBin, "jet.config").exists() &&
               new File(jetBin, Host.mangleExeName(JetCompiler.JET_COMPILER)).exists() &&
               new File(jetBin, Host.mangleExeName(JetPackager.JET_PACKAGER)).exists() ;
    }

    private static String getJetBinDirectory(String jetHome) {
        return jetHome + File.separator + BIN_DIR;
    }

    private static boolean isJetDir(String jetHome) {
        return isJetBinDir(getJetBinDirectory(jetHome));
    }

}
