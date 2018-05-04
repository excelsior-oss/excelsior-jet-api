package com.excelsiorjet.api.tasks.config;

import java.io.File;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.*;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Project Data Base (PDB) is a directory created by the JET compiler at the very start of compilation.
 * That directory will later hold all auxiliary files produced by the compiler.
 * The files are then used for incremental compilation, so only the changed project dependencies
 * are recompiled during the subsequent builds.
 *
 * You may configure the placement of PDB using the parameters below.
 * To clean the PDB in case of problems use {@link JetCleanTask}.
 *
 * Note, that incremental builds and PDB configuration is only supported since Excelsior JET 15 for non x86 targets.
 */
public class PDBConfig {

    private static final String JET_PDB_BASEDIR_PROPERTY = "jet.pdb.basedir";
    private static final String JET_PDB_BASEDIR_ENV_VARIABLE = "JETPDBBASEDIR";


    /**
     * If the parameter is set to {@code true} the PDB directory will be created in the {@link JetProject#jetBuildDir}
     * directory and thus will be cleaned on every clean build.
     *
     * By default, the parameter is set to {@code false}.
     */
    public boolean keepInBuildDir;

    /**
     * Base directory for the PDB.
     *
     * If {@link #keepInBuildDir} is set to {@code false} and {@link #specificLocation} is not set,
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
     * If this parameter is set, it will be used as the PDB location.
     */
    public File specificLocation;

    // computed PDB location according above parameters
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
                    throw new JetTaskFailureException(s("JetApi.PDBConfigurationNotSupported.Failure"));
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
