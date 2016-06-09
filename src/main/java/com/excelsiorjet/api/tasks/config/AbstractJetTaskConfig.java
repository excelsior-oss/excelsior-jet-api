package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.cmd.JetEdition;
import com.excelsiorjet.api.cmd.JetHome;
import com.excelsiorjet.api.cmd.JetHomeException;
import com.excelsiorjet.api.log.AbstractLog;
import com.excelsiorjet.api.tasks.ApplicationType;
import com.excelsiorjet.api.tasks.ClasspathEntry;
import com.excelsiorjet.api.tasks.ExcelsiorJetApiException;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.excelsiorjet.api.util.Txt.s;

public interface AbstractJetTaskConfig {

    String TOMCAT_MAIN_CLASS = "org/apache/catalina/startup/Bootstrap";

    File mainWar();

    String jetHome();

    String packaging();

    File mainJar();

    String mainClass();

    void setMainClass(String mainClass);

    TomcatConfig tomcatConfiguration();

    Stream<ClasspathEntry> getArtifacts();

    String groupId();

    File buildDir();

    String finalName();

    File basedir();

    File packageFilesDir();

    File execProfilesDir();

    String execProfilesName();

    String[] jvmArgs();

    default JetHome validate() throws ExcelsiorJetApiException {
        Txt.log = AbstractLog.instance();

        // check jet home
        JetHome jetHomeObj;
        try {
            jetHomeObj = Utils.isEmpty(jetHome()) ? new JetHome() : new JetHome(jetHome());

        } catch (JetHomeException e) {
            throw new ExcelsiorJetApiException(e.getMessage());
        }

        switch (appType()) {
            case PLAIN:
                //normalize main and set outputName
                setMainClass(mainClass().replace('.', '/'));
                break;
            case TOMCAT:
                setMainClass(TOMCAT_MAIN_CLASS);
                break;
            default:
                throw new AssertionError("Unknown application type");
        }

        switch (packaging().toLowerCase()) {
            case "jar":
                if (!mainJar().exists()) {
                    throw new ExcelsiorJetApiException(s("JetMojo.MainJarNotFound.Failure", mainJar().getAbsolutePath()));
                }
                // check main class
                if (Utils.isEmpty(mainClass())) {
                    throw new ExcelsiorJetApiException(s("JetMojo.MainNotSpecified.Failure"));
                }

                break;
            case "war":
                JetEdition edition;
                try {
                    edition = jetHomeObj.getEdition();
                } catch (JetHomeException e) {
                    throw new ExcelsiorJetApiException(e.getMessage());
                }
                if ((edition != JetEdition.EVALUATION) && (edition != JetEdition.ENTERPRISE)) {
                    throw new ExcelsiorJetApiException(s("JetMojo.TomcatNotSupported.Failure"));
                }

                if (!mainWar().exists()) {
                    throw new ExcelsiorJetApiException(s("JetMojo.MainWarNotFound.Failure", mainWar().getAbsolutePath()));
                }

                tomcatConfiguration().fillDefaults();

                break;
            default:
                throw new ExcelsiorJetApiException(s("JetMojo.BadPackaging.Failure", packaging()));
        }


        return jetHomeObj;
    }

    /**
     * Copies the master Tomcat server to the build directory and main project artifact (.war)
     * to the "webapps" folder of copied Tomcat.
     *
     * @throws ExcelsiorJetApiException
     */
    default void copyTomcatAndWar() throws ExcelsiorJetApiException {
        try {
            Utils.copyDirectory(Paths.get(tomcatConfiguration().tomcatHome), tomcatInBuildDir().toPath());
            String warName = (Utils.isEmpty(tomcatConfiguration().warDeployName)) ? mainWar().getName() : tomcatConfiguration().warDeployName;
            Utils.copyFile(mainWar().toPath(), new File(tomcatInBuildDir(), TomcatConfig.WEBAPPS_DIR + File.separator + warName).toPath());
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(s("JetMojo.ErrorCopyingTomcat.Exception"), e);
        }
    }
    default File createBuildDir() throws ExcelsiorJetApiException {
        File buildDir = buildDir();
        Utils.mkdir(buildDir);
        return buildDir;
    }

    default File tomcatInBuildDir() {
        return new File(buildDir(), tomcatConfiguration().tomcatHome);
    }

    default ApplicationType appType() throws ExcelsiorJetApiException {
        switch (packaging().toLowerCase()) {
            case "jar" :
                return ApplicationType.PLAIN;
            case "war":
                return ApplicationType.TOMCAT;
            default:
                throw new ExcelsiorJetApiException(s("JetMojo.BadPackaging.Failure", packaging()));
        }
    }

}
