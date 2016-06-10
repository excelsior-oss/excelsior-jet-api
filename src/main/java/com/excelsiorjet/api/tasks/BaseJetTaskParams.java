package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.cmd.JetEdition;
import com.excelsiorjet.api.cmd.JetHome;
import com.excelsiorjet.api.cmd.JetHomeException;
import com.excelsiorjet.api.log.AbstractLog;
import com.excelsiorjet.api.tasks.config.TomcatConfig;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.excelsiorjet.api.util.Txt.s;

public class BaseJetTaskParams {

    private File mainWar;

    private String jetHome;

    private String packaging;

    private File mainJar;

    private String mainClass;

    private TomcatConfig tomcatConfiguration;

    private Stream<ClasspathEntry> artifacts;

    private String groupId;

    private File buildDir;

    private String finalName;

    private File basedir;

    private File packageFilesDir;

    private File execProfilesDir;

    private String execProfilesName;

    private String[] jvmArgs;

    BaseJetTaskParams(File mainWar, String jetHome, String packaging, File mainJar, String mainClass, TomcatConfig tomcatConfiguration, Stream<ClasspathEntry> artifacts, String groupId, File buildDir, String finalName, File basedir, File packageFilesDir, File execProfilesDir, String execProfilesName, String[] jvmArgs) {
        this.mainWar = mainWar;
        this.jetHome = jetHome;
        this.packaging = packaging;
        this.mainJar = mainJar;
        this.mainClass = mainClass;
        this.tomcatConfiguration = tomcatConfiguration;
        this.artifacts = artifacts;
        this.groupId = groupId;
        this.buildDir = buildDir;
        this.finalName = finalName;
        this.basedir = basedir;
        this.packageFilesDir = packageFilesDir;
        this.execProfilesDir = execProfilesDir;
        this.execProfilesName = execProfilesName;
        this.jvmArgs = jvmArgs;
    }

    private File mainWar() {
        return mainWar;
    }

    public String jetHome() {
        return jetHome;
    }

    public String packaging() {
        return packaging;
    }

    public File mainJar() {
        return mainJar;
    }

    public String mainClass() {
        return mainClass;
    }

    public TomcatConfig tomcatConfiguration() {
        return tomcatConfiguration;
    }

    Stream<ClasspathEntry> getArtifacts() {
        return artifacts;
    }

    public String groupId() {
        return groupId;
    }

    private File buildDir() {
        return buildDir;
    }

    public String finalName() {
        return finalName;
    }

    public File basedir() {
        return basedir;
    }

    public File packageFilesDir() {
        return packageFilesDir;
    }

    File execProfilesDir() {
        return execProfilesDir;
    }

    String execProfilesName() {
        return execProfilesName;
    }

    public String[] jvmArgs() {
        return jvmArgs;
    }

    public JetHome validate() throws JetTaskFailureException {
        Txt.log = AbstractLog.instance();

        // check jet home
        JetHome jetHomeObj;
        try {
            jetHomeObj = Utils.isEmpty(jetHome()) ? new JetHome() : new JetHome(jetHome());

        } catch (JetHomeException e) {
            throw new JetTaskFailureException(e.getMessage());
        }

        switch (appType()) {
            case PLAIN:
                //normalize main and set outputName
                mainClass = mainClass.replace('.', '/');
                break;
            case TOMCAT:
                mainClass = "org/apache/catalina/startup/Bootstrap";
                break;
            default:
                throw new AssertionError("Unknown application type");
        }

        switch (packaging().toLowerCase()) {
            case "jar":
                if (!mainJar().exists()) {
                    throw new JetTaskFailureException(s("JetMojo.MainJarNotFound.Failure", mainJar().getAbsolutePath()));
                }
                // check main class
                if (Utils.isEmpty(mainClass())) {
                    throw new JetTaskFailureException(s("JetMojo.MainNotSpecified.Failure"));
                }

                break;
            case "war":
                JetEdition edition;
                try {
                    edition = jetHomeObj.getEdition();
                } catch (JetHomeException e) {
                    throw new JetTaskFailureException(e.getMessage());
                }
                if ((edition != JetEdition.EVALUATION) && (edition != JetEdition.ENTERPRISE)) {
                    throw new JetTaskFailureException(s("JetMojo.TomcatNotSupported.Failure"));
                }

                if (!mainWar().exists()) {
                    throw new JetTaskFailureException(s("JetMojo.MainWarNotFound.Failure", mainWar().getAbsolutePath()));
                }

                tomcatConfiguration().fillDefaults();

                break;
            default:
                throw new JetTaskFailureException(s("JetMojo.BadPackaging.Failure", packaging()));
        }


        return jetHomeObj;
    }

    /**
     * Copies the master Tomcat server to the build directory and main project artifact (.war)
     * to the "webapps" folder of copied Tomcat.
     *
     * @throws JetTaskFailureException
     */
    void copyTomcatAndWar() throws JetTaskFailureException {
        try {
            Utils.copyDirectory(Paths.get(tomcatConfiguration().tomcatHome), tomcatInBuildDir().toPath());
            String warName = (Utils.isEmpty(tomcatConfiguration().warDeployName)) ? mainWar().getName() : tomcatConfiguration().warDeployName;
            Utils.copyFile(mainWar().toPath(), new File(tomcatInBuildDir(), TomcatConfig.WEBAPPS_DIR + File.separator + warName).toPath());
        } catch (IOException e) {
            throw new JetTaskFailureException(s("JetMojo.ErrorCopyingTomcat.Exception"), e);
        }
    }
    File createBuildDir() throws JetTaskFailureException {
        File buildDir = buildDir();
        Utils.mkdir(buildDir);
        return buildDir;
    }

    File tomcatInBuildDir() {
        return new File(buildDir(), tomcatConfiguration().tomcatHome);
    }

    ApplicationType appType() throws JetTaskFailureException {
        switch (packaging().toLowerCase()) {
            case "jar" :
                return ApplicationType.PLAIN;
            case "war":
                return ApplicationType.TOMCAT;
            default:
                throw new JetTaskFailureException(s("JetMojo.BadPackaging.Failure", packaging()));
        }
    }

}
