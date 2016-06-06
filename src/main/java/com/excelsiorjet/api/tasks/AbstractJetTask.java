package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.cmd.JetEdition;
import com.excelsiorjet.api.cmd.JetHome;
import com.excelsiorjet.api.cmd.JetHomeException;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.excelsiorjet.api.util.Txt.s;

public abstract class AbstractJetTask<T extends AbstractJetTaskConfig> {

    protected static final String LIB_DIR = "lib";

    protected final T config;

    protected ApplicationType appType;

    protected AbstractJetTask(T config) {
        this.config = config;
    }

    protected void mkdir(File dir) throws ExcelsiorJetApiException {
        if (!dir.exists() && !dir.mkdirs()) {
            if (!dir.exists()) {
                throw new ExcelsiorJetApiException(s("JetMojo.DirCreate.Error", dir.getAbsolutePath()));
            }
            config.log().warn(s("JetMojo.DirCreate.Warning", dir.getAbsolutePath()));
        }
    }

    protected JetHome checkPrerequisites() throws ExcelsiorJetApiException {
        Txt.log = config.log();

        // check jet home
        JetHome jetHomeObj;
        try {
            jetHomeObj = Utils.isEmpty(config.jetHome()) ? new JetHome() : new JetHome(config.jetHome());

        } catch (JetHomeException e) {
            throw new ExcelsiorJetApiException(e.getMessage());
        }

        switch (config.packaging().toLowerCase()) {
            case "jar":
                if (!config.mainJar().exists()) {
                    throw new ExcelsiorJetApiException(s("JetMojo.MainJarNotFound.Failure", config.mainJar().getAbsolutePath()));
                }
                // check main class
                if (Utils.isEmpty(config.mainClass())) {
                    throw new ExcelsiorJetApiException(s("JetMojo.MainNotSpecified.Failure"));
                }

                appType = ApplicationType.PLAIN;
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

                if (!config.mainWar().exists()) {
                    throw new ExcelsiorJetApiException(s("JetMojo.MainWarNotFound.Failure", config.mainWar().getAbsolutePath()));
                }

                config.tomcatConfiguration().fillDefaults();

                appType = ApplicationType.TOMCAT;
                break;
            default:
                throw new ExcelsiorJetApiException(s("JetMojo.BadPackaging.Failure", config.packaging()));
        }


        return jetHomeObj;
    }

    private void copyDependency(File from, File to, File buildDir, List<ClasspathEntry> dependencies, boolean isLib) {
        try {
            Utils.copyFile(from.toPath(), to.toPath());
            dependencies.add(new ClasspathEntry(buildDir.toPath().relativize(to.toPath()).toFile(), isLib));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies project dependencies.
     *
     * @return list of dependencies relative to buildDir
     */
    protected List<ClasspathEntry> copyDependencies(File buildDir, File mainJar) throws ExcelsiorJetApiException {
        File libDir = new File(buildDir, LIB_DIR);
        mkdir(libDir);
        ArrayList<ClasspathEntry> dependencies = new ArrayList<>();
        try {
            copyDependency(mainJar, new File(buildDir, mainJar.getName()), buildDir, dependencies, false);
            config.getArtifacts()
                    .filter(a -> a.getFile().isFile())
                    .forEach(a ->
                            copyDependency(a.getFile(), new File(libDir, a.getFile().getName()), buildDir, dependencies, a.isLib())
                    )
            ;
            return dependencies;
        } catch (Exception e) {
            throw new ExcelsiorJetApiException(s("JetMojo.ErrorCopyingDependency.Exception"), e);
        }
    }

    /**
     * Copies the master Tomcat server to the build directory and main project artifact (.war)
     * to the "webapps" folder of copied Tomcat.
     *
     * @throws ExcelsiorJetApiException
     */
    protected void copyTomcatAndWar() throws ExcelsiorJetApiException {
        try {
            Utils.copyDirectory(config.tomcatHome().toPath(), config.tomcatInBuildDir().toPath());
            String warName = (Utils.isEmpty(config.warDeployName())) ? config.mainWar().getName() : config.warDeployName();
            Utils.copyFile(config.mainWar().toPath(), new File(config.tomcatInBuildDir(), TomcatConfig.WEBAPPS_DIR + File.separator + warName).toPath());
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(s("JetMojo.ErrorCopyingTomcat.Exception"), e);
        }
    }

    protected File createBuildDir() throws ExcelsiorJetApiException {
        File buildDir = config.buildDir();
        mkdir(buildDir);
        return buildDir;
    }

    public enum ApplicationType {
        PLAIN,
        TOMCAT

    }
}
