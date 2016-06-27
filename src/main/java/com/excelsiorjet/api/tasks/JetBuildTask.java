/*
 * Copyright (c) 2016, Excelsior LLC.
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

import com.excelsiorjet.api.cmd.*;
import com.excelsiorjet.api.util.Utils;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Task for building Java (JVM) applications with Excelsior JET.
 *
 * @author Nikita Lipsky
 * @author Aleksey Zhidkov
 */
public class JetBuildTask {

    private final JetProject project;

    public JetBuildTask(JetProject project) {
        this.project = project;
    }

    private static final String APP_DIR = "app";

    /**
     * Generates Excelsior JET project file in {@code buildDir}
     *
     * @param buildDir     directory where project file should be placed
     * @param compilerArgs project compiler args
     * @param dependencies project dependencies
     * @param modules      project modules
     * @param prj name for project file
     * @throws JetTaskFailureException if {@code buildDir} is not exists.
     */
    private String createJetCompilerProject(File buildDir, ArrayList<String> compilerArgs, List<ClasspathEntry> dependencies, ArrayList<String> modules, String prj) throws JetTaskFailureException {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(buildDir, prj))))) {
            compilerArgs.forEach(out::println);
            if (project.compilerOptions() != null) {
                for (String option : project.compilerOptions()) {
                    out.println(option);
                }
            }
            for (ClasspathEntry dep : dependencies) {
                out.println("!classpathentry " + dep.getFile().toString());
                out.println("  -optimize=" + (dep.isLib() ? "autodetect" : "all"));
                out.println("  -protect=" + (dep.isLib() ? "nomatter" : "all"));
                out.println("!end");
            }
            for (String mod : modules) {
                out.println("!module " + mod);
            }
        } catch (FileNotFoundException e) {
            throw new JetTaskFailureException(e.getMessage());
        }
        return prj;
    }

    /**
     * Invokes the Excelsior JET AOT compiler.
     */
    private void compile(JetHome jetHome, File buildDir, List<ClasspathEntry> dependencies) throws JetTaskFailureException, CmdLineToolException, FileNotFoundException {
        ArrayList<String> compilerArgs = new ArrayList<>();
        ArrayList<String> modules = new ArrayList<>();

        switch (project.appType()) {
            case PLAIN:
                compilerArgs.add("-main=" + project.mainClass());
                break;
            case TOMCAT:
                compilerArgs.add("-apptype=tomcat");
                compilerArgs.add("-appdir=" + project.tomcatInBuildDir());
                if (project.tomcatConfiguration().hideConfig) {
                    compilerArgs.add("-hideconfiguration+");
                }
                if (!project.tomcatConfiguration().genScripts) {
                    compilerArgs.add("-gentomcatscripts-");
                }
                break;
            default:
                throw new AssertionError("Unknown app type");
        }


        if (Utils.isWindows()) {
            if (project.icon().isFile()) {
                modules.add(project.icon().getAbsolutePath());
            }
            if (project.hideConsole()) {
                compilerArgs.add("-gui+");
            }
        }

        compilerArgs.add("-outputname=" + project.outputName());
        compilerArgs.add("-decor=ht");

        if (project.profileStartup()) {
            compilerArgs.add("-saprofmode=ALWAYS");
            compilerArgs.add("-saproftimeout=" + project.profileStartupTimeout());
        }

        if (project.isAddWindowsVersionInfo()) {
            compilerArgs.add("-versioninfocompanyname=" + project.vendor());
            compilerArgs.add("-versioninfoproductname=" + project.product());
            compilerArgs.add("-versioninfoproductversion=" + project.winVIVersion());
            compilerArgs.add("-versioninfolegalcopyright=" + project.winVICopyright());
            compilerArgs.add("-versioninfofiledescription=" + project.winVIDescription());
        }

        if (project.multiApp()) {
            compilerArgs.add("-multiapp+");
        }

        if (project.globalOptimizer()) {
            compilerArgs.add("-global+");
        }

        if (project.trialVersion() != null) {
            compilerArgs.add("-expire=" + project.trialVersion().getExpire());
            compilerArgs.add("-expiremsg=" + project.trialVersion().expireMessage);
        }

        if (project.protectData()) {
            compilerArgs.add("-cryptseed=" + project.cryptSeed());
        }

        TestRunExecProfiles execProfiles = new TestRunExecProfiles(project.execProfilesDir(), project.execProfilesName());
        if (execProfiles.getStartup().exists()) {
            compilerArgs.add("-startupprofile=" + execProfiles.getStartup().getAbsolutePath());
        }
        if (execProfiles.getUsg().exists()) {
            modules.add(execProfiles.getUsg().getAbsolutePath());
        }

        String jetVMPropOpt = "-jetvmprop=";
        if (project.jvmArgs() != null && project.jvmArgs().length > 0) {
            jetVMPropOpt = jetVMPropOpt + String.join(" ", (CharSequence[]) project.jvmArgs());

            // JVM args may contain $(Root) prefix for system property value
            // (that should expand to installation directory location).
            // However JET compiler replaces such occurrences with s value of "Root" equation if the "$(Root)" is
            // used in the project file.
            // So we need to pass jetvmprop as separate compiler argument as workaround.
            // We also write the equation in commented form to the project in order to see it in the technical support.
            compilerArgs.add("%" + jetVMPropOpt);
        }

        String prj = createJetCompilerProject(buildDir, compilerArgs, dependencies, modules, project.outputName() + ".prj");

        if (new JetCompiler(jetHome, "=p", prj, jetVMPropOpt)
                .workingDirectory(buildDir).withLog(logger).execute() != 0) {
            throw new JetTaskFailureException(s("JetBuildTask.Build.Failure"));
        }
    }

    private ArrayList<String> getCommonXPackArgs() throws JetTaskFailureException {
        ArrayList<String> xpackArgs = new ArrayList<>();

        switch (project.appType()) {
            case PLAIN:
                if (project.packageFilesDir().exists()) {
                    xpackArgs.add("-source");
                    xpackArgs.add(project.packageFilesDir().getAbsolutePath());
                }

                xpackArgs.addAll(Arrays.asList(
                        "-add-file", Utils.mangleExeName(project.outputName()), "/"
                ));
                break;
            case TOMCAT:
                xpackArgs.add("-source");
                xpackArgs.add(project.tomcatInBuildDir().getAbsolutePath());
                if (project.packageFilesDir().exists()) {
                    logger.warn(s("TestRunTask.PackageFilesIgnoredForTomcat.Warning"));
                }
                break;
            default:
                throw new AssertionError("Unknown app type");
        }

        if (project.optRtFiles() != null && project.optRtFiles().length > 0) {
            xpackArgs.add("-add-opt-rt-files");
            xpackArgs.add(String.join(",", project.optRtFiles()));
        }

        if (project.locales().length > 0) {
            xpackArgs.add("-add-locales");
            xpackArgs.add(String.join(",", project.locales()));
        } else {
            xpackArgs.add("-remove-locales");
            xpackArgs.add("all");
        }

        if (project.javaRuntimeSlimDown() != null) {

            xpackArgs.addAll(Arrays.asList(
                    "-detached-base-url", project.javaRuntimeSlimDown().detachedBaseURL,
                    "-detach-components",
                    (project.javaRuntimeSlimDown().detachComponents != null && project.javaRuntimeSlimDown().detachComponents.length > 0) ?
                            String.join(",", project.javaRuntimeSlimDown().detachComponents) : "auto",
                    "-detached-package", new File(project.jetOutputDir(), project.javaRuntimeSlimDown().detachedPackage).getAbsolutePath()
            ));
        }

        return xpackArgs;
    }

    /**
     * Packages the generated executable and required Excelsior JET runtime files
     * as a self-contained directory
     */
    private void createAppDir(JetHome jetHome, File buildDir, File appDir) throws CmdLineToolException, JetTaskFailureException {
        ArrayList<String> xpackArgs = getCommonXPackArgs();
        xpackArgs.addAll(Arrays.asList(
                "-target", appDir.getAbsolutePath()
        ));
        if (new JetPackager(jetHome, xpackArgs.toArray(new String[xpackArgs.size()]))
                .workingDirectory(buildDir).withLog(logger).execute() != 0) {
            throw new JetTaskFailureException(s("JetBuildTask.Package.Failure"));
        }
    }

    /**
     * Packages the generated executable and required Excelsior JET runtime files
     * as a excelsior installer file.
     */
    private void packWithEI(JetHome jetHome, File buildDir) throws CmdLineToolException, JetTaskFailureException, IOException {
        File target = new File(project.jetOutputDir(), Utils.mangleExeName(project.artifactName()));
        ArrayList<String> xpackArgs = getCommonXPackArgs();
        if (project.excelsiorInstallerConfiguration().eula.exists()) {
            xpackArgs.add(project.excelsiorInstallerConfiguration().eulaFlag());
            xpackArgs.add(project.excelsiorInstallerConfiguration().eula.getAbsolutePath());
        }
        if (Utils.isWindows() && project.excelsiorInstallerConfiguration().installerSplash.exists()) {
            xpackArgs.add("-splash");
            xpackArgs.add(project.excelsiorInstallerConfiguration().installerSplash.getAbsolutePath());
        }
        xpackArgs.addAll(Arrays.asList(
                "-backend", "excelsior-installer",
                "-company", project.vendor(),
                "-product", project.product(),
                "-version", project.version(),
                "-target", target.getAbsolutePath())
        );
        if (new JetPackager(jetHome, xpackArgs.toArray(new String[xpackArgs.size()]))
                .workingDirectory(buildDir).withLog(logger).execute() != 0) {
            throw new JetTaskFailureException(s("JetBuildTask.Package.Failure"));
        }
        logger.info(s("JetBuildTask.Build.Success"));
        logger.info(s("JetBuildTask.GetEI.Info", target.getAbsolutePath()));
    }

    private void createOSXAppBundle(JetHome jetHome, File buildDir) throws JetTaskFailureException, CmdLineToolException, IOException {
        File appBundle = new File(project.jetOutputDir(), project.osxBundleConfiguration().fileName + ".app");
        Utils.mkdir(appBundle);
        try {
            Utils.cleanDirectory(appBundle);
        } catch (IOException e) {
            throw new JetTaskFailureException(e.getMessage(), e);
        }
        File contents = new File(appBundle, "Contents");
        Utils.mkdir(contents);
        File contentsMacOs = new File(contents, "MacOS");
        Utils.mkdir(contentsMacOs);
        File contentsResources = new File(contents, "Resources");
        Utils.mkdir(contentsResources);

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(new File(contents, "Info.plist")), "UTF-8"))) {
            out.print(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                            "<plist version=\"1.0\">\n" +
                            "<dict>\n" +
                            "  <key>CFBundlePackageType</key>\n" +
                            "  <string>APPL</string>\n" +
                            "  <key>CFBundleExecutable</key>\n" +
                            "  <string>" + project.outputName() + "</string>\n" +
                            "  <key>CFBundleName</key>\n" +
                            "  <string>" + project.osxBundleConfiguration().bundleName + "</string>\n" +
                            "  <key>CFBundleIdentifier</key>\n" +
                            "  <string>" + project.osxBundleConfiguration().identifier + "</string>\n" +
                            "  <key>CFBundleVersionString</key>\n" +
                            "  <string>" + project.osxBundleConfiguration().version + "</string>\n" +
                            "  <key>CFBundleShortVersionString</key>\n" +
                            "  <string>" + project.osxBundleConfiguration().shortVersion + "</string>\n" +
                            (project.osxBundleConfiguration().icon.exists() ?
                                    "  <key>CFBundleIconFile</key>\n" +
                                            "  <string>" + project.osxBundleConfiguration().icon.getName() + "</string>\n" : "") +
                            (project.osxBundleConfiguration().highResolutionCapable ?
                                    "  <key>NSHighResolutionCapable</key>\n" +
                                            "  <true/>" : "") +
                            "</dict>\n" +
                            "</plist>\n");
        }

        ArrayList<String> xpackArgs = getCommonXPackArgs();
        xpackArgs.addAll(Arrays.asList(
                "-target", contentsMacOs.getAbsolutePath()
        ));
        if (new JetPackager(jetHome, xpackArgs.toArray(new String[xpackArgs.size()]))
                .workingDirectory(buildDir).withLog(logger).execute() != 0) {
            throw new JetTaskFailureException(s("JetBuildTask.Package.Failure"));
        }

        if (project.osxBundleConfiguration().icon.exists()) {
            Files.copy(project.osxBundleConfiguration().icon.toPath(),
                    new File(contentsResources, project.osxBundleConfiguration().icon.getName()).toPath());
        }

        File appPkg = null;
        if (project.osxBundleConfiguration().developerId != null) {
            logger.info(s("JetBuildTask.SigningOSXBundle.Info"));
            if (new CmdLineTool("codesign", "--verbose", "--force", "--deep", "--sign",
                    project.osxBundleConfiguration().developerId, appBundle.getAbsolutePath()).withLog(logger).execute() != 0) {
                throw new JetTaskFailureException(s("JetBuildTask.OSX.CodeSign.Failure"));
            }
            logger.info(s("JetBuildTask.CreatingOSXInstaller.Info"));
            if (project.osxBundleConfiguration().publisherId != null) {
                appPkg = new File(project.jetOutputDir(), project.artifactName() + ".pkg");
                if (new CmdLineTool("productbuild", "--sign", project.osxBundleConfiguration().publisherId,
                        "--component", appBundle.getAbsolutePath(), project.osxBundleConfiguration().installPath,
                        appPkg.getAbsolutePath())
                        .withLog(logger).execute() != 0) {
                    throw new JetTaskFailureException(s("JetBuildTask.OSX.Packaging.Failure"));
                }
            } else {
                logger.warn(s("JetBuildTask.NoPublisherId.Warning"));
            }
        } else {
            logger.warn(s("JetBuildTask.NoDeveloperId.Warning"));
        }
        logger.info(s("JetBuildTask.Build.Success"));
        if (appPkg != null) {
            logger.info(s("JetBuildTask.GetOSXPackage.Info", appPkg.getAbsolutePath()));
        } else {
            logger.info(s("JetBuildTask.GetOSXBundle.Info", appBundle.getAbsolutePath()));
        }

    }


    private void packageBuild(JetHome jetHome, File buildDir, File packageDir) throws IOException, JetTaskFailureException, CmdLineToolException {
        switch (project.excelsiorJetPackaging()) {
            case JetProject.ZIP:
                logger.info(s("JetBuildTask.ZipApp.Info"));
                File targetZip = new File(project.jetOutputDir(), project.artifactName() + ".zip");
                Utils.compressZipfile(packageDir, targetZip);
                logger.info(s("JetBuildTask.Build.Success"));
                logger.info(s("JetBuildTask.GetZip.Info", targetZip.getAbsolutePath()));
                break;
            case JetProject.EXCELSIOR_INSTALLER:
                packWithEI(jetHome, buildDir);
                break;
            case JetProject.OSX_APP_BUNDLE:
                createOSXAppBundle(jetHome, buildDir);
                break;
            default:
                logger.info(s("JetBuildTask.Build.Success"));
                logger.info(s("JetBuildTask.GetDir.Info", packageDir.getAbsolutePath()));
        }

        if (project.javaRuntimeSlimDown() != null) {
            logger.info(s("JetBuildTask.SlimDown.Info", new File(project.jetOutputDir(), project.javaRuntimeSlimDown().detachedPackage),
                    project.javaRuntimeSlimDown().detachedBaseURL));
        }
    }

    /**
     * Builds project, that was specified in constructor
     *
     * @throws JetTaskFailureException if any task specific error conditions occurs
     * @throws IOException if any I/O error occurs
     * @throws CmdLineToolException if any error occurs while cmd line tool calls
     */
    public void execute() throws JetTaskFailureException, IOException, CmdLineToolException {
        JetHome jetHome = project.validate(true);

        // creating output dirs
        File buildDir = project.createBuildDir();

        File appDir = new File(project.jetOutputDir(), APP_DIR);
        //cleanup packageDir
        try {
            Utils.cleanDirectory(appDir);
        } catch (IOException e) {
            throw new JetTaskFailureException(e.getMessage(), e);
        }

        switch (project.appType()) {
            case PLAIN:
                compile(jetHome, buildDir, project.copyDependencies());
                break;
            case TOMCAT:
                project.copyTomcatAndWar();
                compile(jetHome, buildDir, Collections.emptyList());
                break;
            default:
                throw new AssertionError("Unknown application type");
        }
        createAppDir(jetHome, buildDir, appDir);

        packageBuild(jetHome, buildDir, appDir);
    }

}
