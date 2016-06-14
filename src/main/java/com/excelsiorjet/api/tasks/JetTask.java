package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.cmd.*;
import com.excelsiorjet.api.log.AbstractLog;
import com.excelsiorjet.api.util.Utils;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.excelsiorjet.api.util.Txt.s;

public class JetTask {

    private final JetTaskParams config;

    public JetTask(JetTaskParams config) {
        this.config = config;
    }

    private static final String APP_DIR = "app";

    /**
     * Invokes the Excelsior JET AOT compiler.
     */
    private void compile(JetHome jetHome, File buildDir, List<ClasspathEntry> dependencies) throws JetTaskFailureException, CmdLineToolException, FileNotFoundException {
        ArrayList<String> compilerArgs = new ArrayList<>();
        ArrayList<String> modules = new ArrayList<>();

        switch (config.appType()) {
            case PLAIN:
                compilerArgs.add("-main=" + config.mainClass());
                break;
            case TOMCAT:
                compilerArgs.add("-apptype=tomcat");
                compilerArgs.add("-appdir=" + config.tomcatInBuildDir());
                if (config.tomcatConfiguration().hideConfig) {
                    compilerArgs.add("-hideconfiguration+");
                }
                if (!config.tomcatConfiguration().genScripts) {
                    compilerArgs.add("-gentomcatscripts-");
                }
                break;
            default:
                throw new AssertionError("Unknown app type");
        }


        if (Utils.isWindows()) {
            if (config.icon().isFile()) {
                modules.add(config.icon().getAbsolutePath());
            }
            if (config.hideConsole()) {
                compilerArgs.add("-gui+");
            }
        }

        compilerArgs.add("-outputname=" + config.outputName());
        compilerArgs.add("-decor=ht");

        if (config.profileStartup()) {
            compilerArgs.add("-saprofmode=ALWAYS");
            compilerArgs.add("-saproftimeout=" + config.profileStartupTimeout());
        }

        if (config.isAddWindowsVersionInfo()) {
            compilerArgs.add("-versioninfocompanyname=" + config.vendor());
            compilerArgs.add("-versioninfoproductname=" + config.product());
            compilerArgs.add("-versioninfoproductversion=" + config.winVIVersion());
            compilerArgs.add("-versioninfolegalcopyright=" + config.winVICopyright());
            compilerArgs.add("-versioninfofiledescription=" + config.winVIDescription());
        }

        if (config.multiApp()) {
            compilerArgs.add("-multiapp+");
        }

        if (config.globalOptimizer()) {
            compilerArgs.add("-global+");
        }

        if (config.trialVersion() != null) {
            compilerArgs.add("-expire=" + config.trialVersion().getExpire());
            compilerArgs.add("-expiremsg=" + config.trialVersion().expireMessage);
        }

        if (config.protectData()) {
            compilerArgs.add("-cryptseed=" + config.cryptSeed());
        }

        TestRunExecProfiles execProfiles = new TestRunExecProfiles(config.execProfilesDir(), config.execProfilesName());
        if (execProfiles.getStartup().exists()) {
            compilerArgs.add("-startupprofile=" + execProfiles.getStartup().getAbsolutePath());
        }
        if (execProfiles.getUsg().exists()) {
            modules.add(execProfiles.getUsg().getAbsolutePath());
        }

        String jetVMPropOpt = "-jetvmprop=";
        if (config.jvmArgs() != null && config.jvmArgs().length > 0) {
            jetVMPropOpt = jetVMPropOpt + String.join(" ", (CharSequence[]) config.jvmArgs());

            // JVM args may contain $(Root) prefix for system property value
            // (that should expand to installation directory location).
            // However JET compiler replaces such occurrences with s value of "Root" equation if the "$(Root)" is
            // used in the project file.
            // So we need to pass jetvmprop as separate compiler argument as workaround.
            // We also write the equation in commented form to the project in order to see it in the technical support.
            compilerArgs.add("%" + jetVMPropOpt);
        }

        String prj = TaskUtils.createJetCompilerProject(buildDir, compilerArgs, dependencies, modules, config.outputName() + ".prj");

        if (new JetCompiler(jetHome, "=p", prj, jetVMPropOpt)
                .workingDirectory(buildDir).withLog(AbstractLog.instance()).execute() != 0) {
            throw new JetTaskFailureException(s("JetMojo.Build.Failure"));
        }
    }

    private ArrayList<String> getCommonXPackArgs() throws JetTaskFailureException {
        ArrayList<String> xpackArgs = new ArrayList<>();

        switch (config.appType()) {
            case PLAIN:
                if (config.packageFilesDir().exists()) {
                    xpackArgs.add("-source");
                    xpackArgs.add(config.packageFilesDir().getAbsolutePath());
                }

                xpackArgs.addAll(Arrays.asList(
                        "-add-file", Utils.mangleExeName(config.outputName()), "/"
                ));
                break;
            case TOMCAT:
                xpackArgs.add("-source");
                xpackArgs.add(config.tomcatInBuildDir().getAbsolutePath());
                if (config.packageFilesDir().exists()) {
                    AbstractLog.instance().warn(s("JetMojo.PackageFilesIgnoredForTomcat.Warning"));
                }
                break;
            default:
                throw new AssertionError("Unknown app type");
        }

        if (config.optRtFiles() != null && config.optRtFiles().length > 0) {
            xpackArgs.add("-add-opt-rt-files");
            xpackArgs.add(String.join(",", (CharSequence[]) config.optRtFiles()));
        }

        if (config.javaRuntimeSlimDown() != null) {

            xpackArgs.addAll(Arrays.asList(
                    "-detached-base-url", config.javaRuntimeSlimDown().detachedBaseURL,
                    "-detach-components",
                    (config.javaRuntimeSlimDown().detachComponents != null && config.javaRuntimeSlimDown().detachComponents.length > 0) ?
                            String.join(",", config.javaRuntimeSlimDown().detachComponents) : "auto",
                    "-detached-package", new File(config.jetOutputDir(), config.javaRuntimeSlimDown().detachedPackage).getAbsolutePath()
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
                .workingDirectory(buildDir).withLog(AbstractLog.instance()).execute() != 0) {
            throw new JetTaskFailureException(s("JetMojo.Package.Failure"));
        }
    }

    /**
     * Packages the generated executable and required Excelsior JET runtime files
     * as a excelsior installer file.
     */
    private void packWithEI(JetHome jetHome, File buildDir) throws CmdLineToolException, JetTaskFailureException, IOException {
        File target = new File(config.jetOutputDir(), Utils.mangleExeName(config.finalName()));
        ArrayList<String> xpackArgs = getCommonXPackArgs();
        if (config.excelsiorInstallerConfiguration().eula.exists()) {
            xpackArgs.add(config.excelsiorInstallerConfiguration().eulaFlag());
            xpackArgs.add(config.excelsiorInstallerConfiguration().eula.getAbsolutePath());
        }
        if (Utils.isWindows() && config.excelsiorInstallerConfiguration().installerSplash.exists()) {
            xpackArgs.add("-splash");
            xpackArgs.add(config.excelsiorInstallerConfiguration().installerSplash.getAbsolutePath());
        }
        xpackArgs.addAll(Arrays.asList(
                "-backend", "excelsior-installer",
                "-company", config.vendor(),
                "-product", config.product(),
                "-version", config.version(),
                "-target", target.getAbsolutePath())
        );
        if (new JetPackager(jetHome, xpackArgs.toArray(new String[xpackArgs.size()]))
                .workingDirectory(buildDir).withLog(AbstractLog.instance()).execute() != 0) {
            throw new JetTaskFailureException(s("JetMojo.Package.Failure"));
        }
        AbstractLog.instance().info(s("JetMojo.Build.Success"));
        AbstractLog.instance().info(s("JetMojo.GetEI.Info", target.getAbsolutePath()));
    }


    private void createOSXAppBundle(JetHome jetHome, File buildDir) throws JetTaskFailureException, CmdLineToolException, IOException {
        File appBundle = new File(config.jetOutputDir(), config.osxBundleConfiguration().fileName + ".app");
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
                            "  <string>" + config.outputName() + "</string>\n" +
                            "  <key>CFBundleName</key>\n" +
                            "  <string>" + config.osxBundleConfiguration().bundleName + "</string>\n" +
                            "  <key>CFBundleIdentifier</key>\n" +
                            "  <string>" + config.osxBundleConfiguration().identifier + "</string>\n" +
                            "  <key>CFBundleVersionString</key>\n" +
                            "  <string>" + config.osxBundleConfiguration().version + "</string>\n" +
                            "  <key>CFBundleShortVersionString</key>\n" +
                            "  <string>" + config.osxBundleConfiguration().shortVersion + "</string>\n" +
                            (config.osxBundleConfiguration().icon.exists() ?
                                    "  <key>CFBundleIconFile</key>\n" +
                                            "  <string>" + config.osxBundleConfiguration().icon.getName() + "</string>\n" : "") +
                            (config.osxBundleConfiguration().highResolutionCapable ?
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
                .workingDirectory(buildDir).withLog(AbstractLog.instance()).execute() != 0) {
            throw new JetTaskFailureException(s("JetMojo.Package.Failure"));
        }

        if (config.osxBundleConfiguration().icon.exists()) {
            Files.copy(config.osxBundleConfiguration().icon.toPath(),
                    new File(contentsResources, config.osxBundleConfiguration().icon.getName()).toPath());
        }

        File appPkg = null;
        if (config.osxBundleConfiguration().developerId != null) {
            AbstractLog.instance().info(s("JetMojo.SigningOSXBundle.Info"));
            if (new CmdLineTool("codesign", "--verbose", "--force", "--deep", "--sign",
                    config.osxBundleConfiguration().developerId, appBundle.getAbsolutePath()).withLog(AbstractLog.instance()).execute() != 0) {
                throw new JetTaskFailureException(s("JetMojo.OSX.CodeSign.Failure"));
            }
            AbstractLog.instance().info(s("JetMojo.CreatingOSXInstaller.Info"));
            if (config.osxBundleConfiguration().publisherId != null) {
                appPkg = new File(config.jetOutputDir(), config.finalName() + ".pkg");
                if (new CmdLineTool("productbuild", "--sign", config.osxBundleConfiguration().publisherId,
                        "--component", appBundle.getAbsolutePath(), config.osxBundleConfiguration().installPath,
                        appPkg.getAbsolutePath())
                        .withLog(AbstractLog.instance()).execute() != 0) {
                    throw new JetTaskFailureException(s("JetMojo.OSX.Packaging.Failure"));
                }
            } else {
                AbstractLog.instance().warn(s("JetMojo.NoPublisherId.Warning"));
            }
        } else {
            AbstractLog.instance().warn(s("JetMojo.NoDeveloperId.Warning"));
        }
        AbstractLog.instance().info(s("JetMojo.Build.Success"));
        if (appPkg != null) {
            AbstractLog.instance().info(s("JetMojo.GetOSXPackage.Info", appPkg.getAbsolutePath()));
        } else {
            AbstractLog.instance().info(s("JetMojo.GetOSXBundle.Info", appBundle.getAbsolutePath()));
        }

    }

    private void packageBuild(JetHome jetHome, File buildDir, File packageDir) throws IOException, JetTaskFailureException, CmdLineToolException {
        switch (config.excelsiorJetPackaging()) {
            case JetTaskParams.ZIP:
                AbstractLog.instance().info(s("JetMojo.ZipApp.Info"));
                File targetZip = new File(config.jetOutputDir(), config.finalName() + ".zip");
                TaskUtils.compressZipfile(packageDir, targetZip);
                AbstractLog.instance().info(s("JetMojo.Build.Success"));
                AbstractLog.instance().info(s("JetMojo.GetZip.Info", targetZip.getAbsolutePath()));
                break;
            case JetTaskParams.EXCELSIOR_INSTALLER:
                packWithEI(jetHome, buildDir);
                break;
            case JetTaskParams.OSX_APP_BUNDLE:
                createOSXAppBundle(jetHome, buildDir);
                break;
            default:
                AbstractLog.instance().info(s("JetMojo.Build.Success"));
                AbstractLog.instance().info(s("JetMojo.GetDir.Info", packageDir.getAbsolutePath()));
        }

        if (config.javaRuntimeSlimDown() != null) {
            AbstractLog.instance().info(s("JetMojo.SlimDown.Info", new File(config.jetOutputDir(), config.javaRuntimeSlimDown().detachedPackage),
                    config.javaRuntimeSlimDown().detachedBaseURL));
        }
    }

    public void execute() throws JetTaskFailureException, IOException, CmdLineToolException {
        JetHome jetHome = config.validate();

        // creating output dirs
        File buildDir = config.createBuildDir();

        File appDir = new File(config.jetOutputDir(), APP_DIR);
        //cleanup packageDir
        try {
            Utils.cleanDirectory(appDir);
        } catch (IOException e) {
            throw new JetTaskFailureException(e.getMessage(), e);
        }

        switch (config.appType()) {
            case PLAIN:
                compile(jetHome, buildDir, TaskUtils.copyDependencies(buildDir, config.mainJar(), config.getArtifacts()));
                break;
            case TOMCAT:
                config.copyTomcatAndWar();
                compile(jetHome, buildDir, Collections.emptyList());
                break;
            default:
                throw new AssertionError("Unknown application type");
        }
        createAppDir(jetHome, buildDir, appDir);

        packageBuild(jetHome, buildDir, appDir);
    }
}
