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

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.cmd.CmdLineTool;
import com.excelsiorjet.api.cmd.CmdLineToolException;
import com.excelsiorjet.api.util.Utils;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

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
    private final CompilerArgsGenerator compilerArgsGenerator;
    private final PackagerArgsGenerator packagerArgsGenerator;
    private final ExcelsiorJet excelsiorJet;

    private File buildDir;

    public JetBuildTask(ExcelsiorJet excelsiorJet, JetProject project) throws JetTaskFailureException {
        this.excelsiorJet = excelsiorJet;
        this.project = project;
        compilerArgsGenerator = new CompilerArgsGenerator(project);
        packagerArgsGenerator = new PackagerArgsGenerator(project);
    }

    private static final String APP_DIR = "app";

    /**
     * Generates Excelsior JET project file in {@code buildDir}
     *
     * @throws JetTaskFailureException if {@code buildDir} is not exists.
     */
    private String createJetCompilerProject() throws JetTaskFailureException {
        String prj = project.outputName() + ".prj";
        try (Writer writer = new BufferedWriter(new FileWriter(new File(buildDir, prj)))) {
            writer.write(compilerArgsGenerator.projectFileContent());
        } catch (IOException e) {
            throw new JetTaskFailureException(e.getMessage(), e);
        }
        return prj;
    }

    /**
     * Invokes the Excelsior JET AOT compiler.
     */
    private void compile(File buildDir) throws JetTaskFailureException, CmdLineToolException, IOException {
        String prj = createJetCompilerProject();
        if (excelsiorJet.compile(buildDir, "=p", prj, compilerArgsGenerator.jetVMPropOpt()) != 0) {
            throw new JetTaskFailureException(s("JetBuildTask.Build.Failure"));
        }
    }


    /**
     * Packages the generated executable and required Excelsior JET runtime files
     * as a self-contained directory
     */
    private void createAppDir(File buildDir, File appDir) throws CmdLineToolException, JetTaskFailureException {
        ArrayList<String> xpackArgs = packagerArgsGenerator.getCommonXPackArgs(appDir.getAbsolutePath());
        if (excelsiorJet.pack(buildDir, xpackArgs.toArray(new String[xpackArgs.size()])) != 0) {
            throw new JetTaskFailureException(s("JetBuildTask.Package.Failure"));
        }
    }

    /**
     * Packages the generated executable and required Excelsior JET runtime files
     * as a excelsior installer file.
     */
    private void packWithEI(File buildDir) throws CmdLineToolException, JetTaskFailureException, IOException {
        File target = new File(project.jetOutputDir(), Utils.mangleExeName(project.artifactName()));
        ArrayList<String> xpackArgs = packagerArgsGenerator.getCommonXPackArgs();
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
        if (excelsiorJet.pack(buildDir, xpackArgs.toArray(new String[xpackArgs.size()])) != 0) {
            throw new JetTaskFailureException(s("JetBuildTask.Package.Failure"));
        }
        logger.info(s("JetBuildTask.Build.Success"));
        logger.info(s("JetBuildTask.GetEI.Info", target.getAbsolutePath()));
    }

    private void createOSXAppBundle(File buildDir) throws JetTaskFailureException, CmdLineToolException, IOException {
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

        ArrayList<String> xpackArgs = packagerArgsGenerator.getCommonXPackArgs(contentsMacOs.getAbsolutePath());
        if (excelsiorJet.pack(buildDir, xpackArgs.toArray(new String[xpackArgs.size()])) != 0) {
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


    private void packageBuild(File buildDir, File packageDir) throws IOException, JetTaskFailureException, CmdLineToolException {
        switch (project.excelsiorJetPackaging()) {
            case ZIP:
                logger.info(s("JetBuildTask.ZipApp.Info"));
                File targetZip = new File(project.jetOutputDir(), project.artifactName() + ".zip");
                Utils.compressZipfile(packageDir, targetZip);
                logger.info(s("JetBuildTask.Build.Success"));
                logger.info(s("JetBuildTask.GetZip.Info", targetZip.getAbsolutePath()));
                break;
            case EXCELSIOR_INSTALLER:
                packWithEI(buildDir);
                break;
            case OSX_APP_BUNDLE:
                createOSXAppBundle(buildDir);
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
        project.validate(excelsiorJet, true);
        buildDir = project.createBuildDir();

        File appDir = new File(project.jetOutputDir(), APP_DIR);
        //cleanup packageDir
        try {
            Utils.cleanDirectory(appDir);
        } catch (IOException e) {
            throw new JetTaskFailureException(e.getMessage(), e);
        }

        switch (project.appType()) {
            case PLAIN:
                project.copyClasspathEntries();
                compile(buildDir);
                break;
            case TOMCAT:
                project.copyTomcatAndWar();
                compile(buildDir);
                break;
            default:
                throw new AssertionError("Unknown application type");
        }
        createAppDir(buildDir, appDir);

        packageBuild(buildDir, appDir);
    }

}
