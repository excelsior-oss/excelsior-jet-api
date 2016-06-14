package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.cmd.*;
import com.excelsiorjet.api.log.AbstractLog;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestRunTask {
    private static final String BOOTSTRAP_JAR = "bootstrap.jar";

    private final BaseJetTaskParams config;

    public TestRunTask(BaseJetTaskParams config) {
        this.config = config;
    }

    private String getTomcatClassPath(JetHome jetHome, File tomcatBin) throws JetTaskFailureException, IOException {
        File f = new File(tomcatBin, BOOTSTRAP_JAR);
        if (!f.exists()) {
            throw new JetTaskFailureException(Txt.s("TestRunMojo.Tomcat.NoBootstrapJar.Failure", tomcatBin.getAbsolutePath()));
        }

        Manifest bootManifest;
        try {
            bootManifest = new JarFile(f).getManifest();
        } catch (IOException e) {
            throw new IOException(Txt.s("TestRunMojo.Tomcat.FailedToReadBootstrapJar.Failure", tomcatBin.getAbsolutePath(), e.getMessage()), e);
        }

        ArrayList<String> classPath = new ArrayList<>();
        classPath.add(BOOTSTRAP_JAR);

        String bootstrapJarCP = bootManifest.getMainAttributes().getValue("CLASS-PATH");
        if (bootstrapJarCP != null) {
            classPath.addAll(Arrays.asList(bootstrapJarCP.split("\\s+")));
        }

        classPath.add(jetHome.getJetHome() + File.separator + "lib" + File.separator + "tomcat" + File.separator + "TomcatSupport.jar");
        return String.join(File.pathSeparator, classPath);
    }

    public List<String> getTomcatVMArgs() {
        String tomcatDir = config.tomcatInBuildDir().getAbsolutePath();
        return Arrays.asList(
                "-Djet.classloader.id.provider=com/excelsior/jet/runtime/classload/customclassloaders/tomcat/TomcatCLIDProvider",
                "-Dcatalina.base=" + tomcatDir,
                "-Dcatalina.home=" + tomcatDir,
                "-Djava.io.tmpdir=" + tomcatDir + File.separator + "temp",
                "-Djava.util.logging.config.file=../conf/logging.properties",
                "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
        );
    }

    public void execute() throws JetTaskFailureException, IOException, CmdLineToolException {
        JetHome jetHome = config.validate();

        // creating output dirs
        File buildDir = config.createBuildDir();

        String classpath;
        List<String> additionalVMArgs;
        File workingDirectory;
        switch (config.appType()) {
            case PLAIN:
                List<ClasspathEntry> dependencies = TaskUtils.copyDependencies(buildDir, config.mainJar(), config.getArtifacts());
                if (config.packageFilesDir().exists()) {
                    //application may access custom package files at runtime. So copy them as well.
                    TaskUtils.copyQuietly(config.packageFilesDir().toPath(), buildDir.toPath());
                }

                classpath = String.join(File.pathSeparator,
                        dependencies.stream().map(d -> d.getFile().toString()).collect(Collectors.toList()));
                additionalVMArgs = Collections.emptyList();
                workingDirectory = buildDir;
                break;
            case TOMCAT:
                config.copyTomcatAndWar();
                workingDirectory = new File(config.tomcatInBuildDir(), "bin");
                classpath = getTomcatClassPath(jetHome, workingDirectory);
                additionalVMArgs = getTomcatVMArgs();
                break;
            default:
                throw new AssertionError("Unknown app type");
        }

        Utils.mkdir(config.execProfilesDir());

        XJava xjava = new XJava(jetHome);
        try {
            xjava.addTestRunArgs(new TestRunExecProfiles(config.execProfilesDir(), config.execProfilesName()))
                    .withLog(AbstractLog.instance(),
                            config.appType() == ApplicationType.TOMCAT) // Tomcat outputs to std error, so to not confuse users,
                    // we  redirect its output to std out in test run
                    .workingDirectory(workingDirectory);
        } catch (JetHomeException e) {
            throw new JetTaskFailureException(e.getMessage());
        }

        xjava.addArgs(additionalVMArgs);

        //add jvm args substituting $(Root) occurences with buildDir
        xjava.addArgs(Stream.of(config.jvmArgs())
                .map(s -> s.replace("$(Root)", buildDir.getAbsolutePath()))
                .collect(Collectors.toList())
        );

        xjava.arg("-cp");
        xjava.arg(classpath);
        xjava.arg(config.mainClass());
        String cmdLine = xjava.getArgs().stream()
                .map(arg -> arg.contains(" ") ? '"' + arg + '"' : arg)
                .collect(Collectors.joining(" "));

        AbstractLog.instance().info(Txt.s("TestRunMojo.Start.Info", cmdLine));

        int errCode = xjava.execute();
        String finishText = Txt.s("TestRunMojo.Finish.Info", errCode);
        if (errCode != 0) {
            AbstractLog.instance().warn(finishText);
        } else {
            AbstractLog.instance().info(finishText);
        }
    }
}
