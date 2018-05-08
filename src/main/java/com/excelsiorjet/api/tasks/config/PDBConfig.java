package com.excelsiorjet.api.tasks.config;

import java.io.File;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.*;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Project Data Base (PDB) is a directory that holds all auxiliary files produced by 
 * the Excelsior JET AOT compiler. If the PDB directory does not exist, or a clean build
 * is enforced, the compiler creates it automatically. Otherwise, it can re-use 
 * the information that is already there for <i>incremental compilation</i>, skipping
 * over the dependencies that did not change since the previous build.
 *
 * You may configure the location of the PDB using the parameters below.
 * To clean the PDB in case of problems use {@link JetCleanTask}.
 *
 * Note, that incremental builds and PDB configuration are only supported since 
 * Excelsior JET 15 for targets other than 32-bit x86.
 */
public class PDBConfig {

    private static final String JET_PDB_BASEDIR_PROPERTY = "jet.pdb.basedir";
    private static final String JET_PDB_BASEDIR_ENV_VARIABLE = "JETPDBBASEDIR";

    /**
     * If this parameter is set to {@code true}, the PDB directory will be created in the {@link JetProject#jetBuildDir}
     * directory and thus will be cleaned on every clean build.
     *
     * By default, this parameter is set to {@code false}.
     */
    public boolean keepInBuildDir;

    /**
     * Base directory for the PDB.
     *
     * If {@link #keepInBuildDir} is set to {@code false} <i>and</i> {@link #specificLocation} is not set,
     * the PDB directory for the current project will be located in the
     * {@link JetProject#groupId}/{@link JetProject#projectName} subdirectory of {@link #baseDir}.
     *
     * You may set the parameter either directly from a Maven/Gradle plugin configuration or
     * using either the {@code jet.pdb.basedir} system property or {@code JETPDBBASEDIR} environment variable.
     *
     * The default value for {@link #baseDir} is {@code ${user.home}/.ExcelsiorJET/PDB}.
     */
    public File baseDir;

    /**
     * In some cases, you may need to fully control the placement of the PDB.
     * If this parameter is set to a pathname of a directory, the compiler will use it 
     * as the PDB location, possibly creating the directory if it does not exist.
     */
    public File specificLocation;

    // PDB location computed from the above parameters
    private File pdbLocation;

    public File pdbLocation() {
        return pdbLocation;
    }

    public void fillDefaults(JetProject project, ExcelsiorJet excelsiorJet) throws JetTaskFailureException {
        if (excelsiorJet.isSmartSupported()) {
            if (keepInBuildDir) {
                if (baseDir != null) {
                    throw new JetTaskFailureException(s("JetApi.PDBInBuildDir.Failure", "baseDir"));
                }
                if (specificLocation != null) {
                    throw new JetTaskFailureException(s("JetApi.PDBInBuildDir.Failure", "specificLocation"));
                }
            } else if (specificLocation != null) {
                if (baseDir != null) {
                    throw new JetTaskFailureException(s("JetApi.PDBBasedDirAndSpecificLocation.Failure"));
                }
                pdbLocation = specificLocation;
            } else {
                if (baseDir == null) {
                    if (!Utils.isEmpty(System.getProperty(JET_PDB_BASEDIR_PROPERTY))) {
                        baseDir = new File(System.getProperty(JET_PDB_BASEDIR_PROPERTY));
                    } else if (!Utils.isEmpty(System.getenv(JET_PDB_BASEDIR_ENV_VARIABLE))) {
                        baseDir = new File(System.getenv(JET_PDB_BASEDIR_ENV_VARIABLE));
                    } else {
                        baseDir = new File(System.getProperty("user.home"), ".ExcelsiorJET" + File.separator + "PDB");
                    }
                }
                pdbLocation = new File(baseDir, project.groupId() + File.separator + project.projectName());
            }
        } else {
            if ((baseDir!=null) || (specificLocation != null)) {
                if (excelsiorJet.isPDBConfigurationSupported()) {
                    logger.warn(s("JetApi.NoSmartForX86.Warning"));
                } else {
                    logger.warn(s("JetApi.PDBConfigurationNotSupported.Warning"));
                }
            }
            keepInBuildDir = true;
        }

        if (keepInBuildDir) {
            //set pdb location for JetCleanTask
            pdbLocation = new File(project.jetBuildDir(), project.outputName() + "_jetpdb");
        }
    }
}
