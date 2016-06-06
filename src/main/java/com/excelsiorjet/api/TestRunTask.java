package com.excelsiorjet.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestRunTask extends AbstractJetTask<AbstractJetTaskConfig> {

    private static final String TOMCAT_MAIN_CLASS = "org/apache/catalina/startup/Bootstrap";
    private static final String BOOTSTRAP_JAR = "bootstrap.jar";

    public TestRunTask(AbstractJetTaskConfig config) {
        super(config);
    }

    private void copyExtraPackageFiles(File buildDir) {
        // We could just use Maven FileUtils.copyDirectory method but it copies a directory as a whole
        // while here we copy only those files that were changed from previous build.
        Path target = buildDir.toPath();
        Path source = config.packageFilesDir().toPath();
        try {
            Utils.copyDirectory(source, target);
        } catch (IOException e) {
            config.log().warn(Txt.s("TestRunMojo.ErrorWhileCopying.Warning", source.toString(), target.toString(), e.getMessage()), e);
        }
    }

    public String getTomcatClassPath(JetHome jetHome, File tomcatBin) throws ExcelsiorJetApiException {
        File f = new File(tomcatBin, BOOTSTRAP_JAR);
        if (!f.exists()) {
            throw new ExcelsiorJetApiException(Txt.s("TestRunMojo.Tomcat.NoBootstrapJar.Failure", tomcatBin.getAbsolutePath()));
        }

        Manifest bootManifest;
        try {
            bootManifest = new JarFile(f).getManifest();
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(Txt.s("TestRunMojo.Tomcat.FailedToReadBootstrapJar.Failure", tomcatBin.getAbsolutePath(), e.getMessage()), e);
        }

        ArrayList<String> classPath = new ArrayList<String>();
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
                "-Djava.io.tmpdir="+ tomcatDir + File.separator + "temp",
                "-Djava.util.logging.config.file=../conf/logging.properties",
                "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
        );
    }

    public void execute() throws ExcelsiorJetApiException {
        JetHome jetHome = checkPrerequisites();

        // creating output dirs
        File buildDir = createBuildDir();

        String classpath;
        List<String> additionalVMArgs;
        File workingDirectory;
        switch (appType) {
            case PLAIN:
                List<Dependency> dependencies = copyDependencies(buildDir, config.mainJar());
                if (config.packageFilesDir().exists()) {
                    //application may access custom package files at runtime. So copy them as well.
                    copyExtraPackageFiles(buildDir);
                }

                classpath = String.join(File.pathSeparator,
                        dependencies.stream().map(d -> d.dependency).collect(Collectors.toList()));
                additionalVMArgs = Collections.emptyList();
                workingDirectory = buildDir;
                break;
            case TOMCAT:
                copyTomcatAndWar();
                workingDirectory = new File(config.tomcatInBuildDir(), "bin");
                classpath = getTomcatClassPath(jetHome, workingDirectory);
                additionalVMArgs = getTomcatVMArgs();
                config.setMainClass(TOMCAT_MAIN_CLASS);
                break;
            default:
                throw new AssertionError("Unknown app type");
        }

        mkdir(config.execProfilesDir());

        XJava xjava = new XJava(jetHome);
        try {
            xjava.addTestRunArgs(new TestRunExecProfiles(config.execProfilesDir(), config.execProfilesName()))
                    .withLog(config.log(),
                            appType == ApplicationType.TOMCAT) // Tomcat outputs to std error, so to not confuse users,
                    // we  redirect its output to std out in test run
                    .workingDirectory(workingDirectory);
        } catch (JetHomeException e) {
            throw new ExcelsiorJetApiException(e.getMessage());
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
        try {
            String cmdLine = xjava.getArgs().stream()
                    .map(arg -> arg.contains(" ") ? '"' + arg + '"' : arg)
                    .collect(Collectors.joining(" "));

            config.log().info(Txt.s("TestRunMojo.Start.Info", cmdLine));

            int errCode = xjava.execute();
            String finishText = Txt.s("TestRunMojo.Finish.Info", errCode);
            if (errCode != 0) {
                config.log().warn(finishText);
            } else {
                config.log().info(finishText);
            }
        } catch (CmdLineToolException e) {
            throw new ExcelsiorJetApiException(e.getMessage());
        }
    }
}
