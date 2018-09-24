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
package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.cmd.CmdLineToolException;
import com.excelsiorjet.api.tasks.config.compiler.ExecProfilesConfig;
import com.excelsiorjet.api.tasks.config.packagefile.PackageFile;
import com.excelsiorjet.api.tasks.config.ApplicationType;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.excelsiorjet.api.log.Log.logger;
import static java.util.Arrays.asList;

/**
 * Task for performing a Test Run before building the application.
 * Running your Java application before optimization helps Excelsior JET:
 * <ul>
 *  <li>
 *      Verify that your application can run on the Excelsior JET JVM flawlessly
 *      (i.e. it has no implicit dependencies on the Oracle JVM implementation
 *      and your project has no configuration issues specific to Excelsior JET).
 *  </li>
 *  <li>
 *      Collect profile information to optimize your app more effectively.
 *  </li>
 *  <li>
 *      Enable application startup time optimization.
 *      Performing a Test Run can reduce the startup time by a factor of up to two.
 *  </li>
 * </ul>
 * <p>
 * It is recommended to commit the collected profiles (.usg, .startup) to VCS so as to
 * enable the {@code JetBuildTask} to re-use them during subsequent builds without performing the Test Run.
 * </p>
 *
 *  Note: During a Test Run, the application is executed in a special profiling mode,
 *        so disregard its modest start-up time and performance.
 *
 * @author Nikita Lipsky
 * @author Aleksey Zhidkov
 */
public class TestRunTask {

    private static final String BOOTSTRAP_JAR = "bootstrap.jar";
    private static final String TOMCAT_JULI_JAR = "tomcat-juli.jar";

    private final ExcelsiorJet excelsiorJet;
    private final JetProject project;

    public TestRunTask(ExcelsiorJet excelsiorJet, JetProject project) throws JetTaskFailureException {
        this.excelsiorJet = excelsiorJet;
        this.project = project;
    }

    private String getTomcatClassPath(File tomcatBin) throws JetTaskFailureException, IOException {
        File f = new File(tomcatBin, BOOTSTRAP_JAR);
        if (!f.exists()) {
            throw new JetTaskFailureException(Txt.s("TestRunTask.Tomcat.NoBootstrapJar.Failure", tomcatBin.getAbsolutePath()));
        }

        Manifest bootManifest;
        try {
            bootManifest = new JarFile(f).getManifest();
        } catch (IOException e) {
            throw new IOException(Txt.s("TestRunTask.Tomcat.FailedToReadBootstrapJar.Failure", tomcatBin.getAbsolutePath(), e.getMessage()), e);
        }

        ArrayList<String> classPath = new ArrayList<>();
        classPath.add(BOOTSTRAP_JAR);

        String bootstrapJarCP = bootManifest.getMainAttributes().getValue("CLASS-PATH");
        if (bootstrapJarCP != null) {
            classPath.addAll(asList(bootstrapJarCP.split("\\s+")));
        }

        if (!classPath.contains(TOMCAT_JULI_JAR) && new File(tomcatBin, TOMCAT_JULI_JAR).exists()) {
            classPath.add(TOMCAT_JULI_JAR);
        }

        classPath.add(excelsiorJet.getJetHome() + File.separator + "lib" + File.separator + "tomcat" + File.separator + "TomcatSupport.jar");
        return String.join(File.pathSeparator, classPath);
    }

    public List<String> getTomcatVMArgs() {
        String tomcatDir = project.tomcatInBuildDir().getAbsolutePath();
        return asList(
                "-Djet.classloader.id.provider=com/excelsior/jet/runtime/classload/customclassloaders/tomcat/TomcatCLIDProvider",
                "-Dcatalina.base=" + tomcatDir,
                "-Dcatalina.home=" + tomcatDir,
                "-Djava.io.tmpdir=" + tomcatDir + File.separator + "temp",
                "-Djava.util.logging.config.file=../conf/logging.properties",
                "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
        );
    }

    public void execute() throws JetTaskFailureException, IOException, CmdLineToolException {
        if (!excelsiorJet.isTestRunSupported()) {
            throw new JetTaskFailureException(Txt.s("TestRunTask.NoTestRunForCrossCompilation.Error"));
        }
        project.validate(excelsiorJet, false);
        if ((project.appType() == ApplicationType.DYNAMIC_LIBRARY) && Utils.isEmpty(project.mainClass())) {
            throw new JetTaskFailureException(Txt.s("TestRunTask.ForInvocationDLL.Error"));
        }

        // creating output dirs
        File buildDir = project.createBuildDir();

        String classpath;
        List<String> additionalVMArgs;
        File workingDirectory;
        switch (project.appType()) {
            case PLAIN:
            case WINDOWS_SERVICE:
            case DYNAMIC_LIBRARY:
                List<ClasspathEntry> dependencies = project.copyClasspathEntries();
                if (project.packageFilesDir() != null) {
                    //application may access custom package files at runtime. So copy them as well.
                    Utils.copyQuietly(project.packageFilesDir().toPath(), buildDir.toPath());
                }

                for (PackageFile pFile : project.packageFiles()) {
                    String packPath = pFile.packagePath.replace('/', File.separatorChar);
                    while (packPath.startsWith(File.separator)) {
                        //strip leading slashes
                        packPath = packPath.substring(1);
                    }
                    if (packPath.isEmpty()) {
                        packPath = ".";
                    }
                    Path packagePath = buildDir.toPath().resolve(packPath);
                    packagePath.toFile().mkdirs();
                    if (pFile.path.isDirectory()) {
                        Utils.copyDirectory(pFile.path.toPath(), packagePath.resolve(pFile.path.getName()));
                    } else {
                        Utils.copyFile(pFile.path.toPath(), packagePath.resolve(pFile.path.getName()));
                    }
                }

                classpath = String.join(File.pathSeparator,
                        dependencies.stream().map(d -> d.path.toString()).collect(Collectors.toList()));
                additionalVMArgs = Collections.emptyList();
                workingDirectory = buildDir;
                break;
            case TOMCAT:
                project.copyTomcatAndWar();
                workingDirectory = new File(project.tomcatInBuildDir(), "bin");
                classpath = getTomcatClassPath(workingDirectory);
                additionalVMArgs = getTomcatVMArgs();
                break;
            case SPRING_BOOT:
                project.copySpringBootArtifact();
                workingDirectory = buildDir;
                classpath = project.mainArtifact().getName();
                additionalVMArgs = Collections.singletonList("-Djet.classloader.id.provider=com/excelsior/jet/runtime/classload/customclassloaders/springboot/SpringBootCLIDProvider");
                break;
            default:
                throw new AssertionError("Unknown app type");
        }

        Utils.mkdir(project.execProfiles().outputDir);

        RunStopSupport runStopSupport = new RunStopSupport(project.jetOutputDir(), false);

        List<String> args = xjavaArgs(buildDir, classpath, additionalVMArgs, runStopSupport);
        String cmdLine = args.stream()
                .map(Utils::quoteCmdLineArgument)
                .collect(Collectors.joining(" "));

        logger.info(Txt.s("TestRunTask.Start.Info", cmdLine));

        // Tomcat outputs to std error, so to not confuse users,
        // we  redirect its output to std out in test run
        boolean errToOut = project.appType() != ApplicationType.TOMCAT;
        int errCode = excelsiorJet.testRun(workingDirectory, logger, errToOut, args.toArray(new String[args.size()]));
        runStopSupport.taskFinished();
        String finishText = Txt.s("TestRunTask.Finish.Info", errCode);
        if (errCode != 0) {
            logger.warn(finishText);
        } else {
            logger.info(finishText);
        }
    }

    private List<String> xjavaArgs(File buildDir, String classpath, List<String> additionalVMArgs, RunStopSupport runStopSupport) throws JetTaskFailureException {
        List<String> args = new ArrayList<>();
        ExecProfilesConfig execProfiles = project.execProfiles();
        if (excelsiorJet.isStartupProfileGenerationSupported()) {
            args.add("-Djet.jit.profile.startup=" + execProfiles.getStartup().getAbsolutePath());
        }
        if (excelsiorJet.isUsageListGenerationSupported()) {
            args.add("-Djet.usage.list=" + execProfiles.getUsg().getAbsolutePath());
        }

        args.add(project.getTerminationVMProp(runStopSupport.prepareToRunTask()));

        args.addAll(additionalVMArgs);

        //add jvm args substituting $(Root) occurences with buildDir
        args.addAll(Stream.of(project.jvmArgs())
                .map(s -> s.replace("$(Root)", buildDir.getAbsolutePath()))
                .collect(Collectors.toList())
        );

        args.addAll(asList("-cp", classpath, project.mainClass()));
        args.addAll(asList(project.runArgs()));
        return args;
    }
}
