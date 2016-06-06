package com.excelsiorjet.api;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.excelsiorjet.api.Txt.s;

public class JetTask extends AbstractJetTask<JetTaskConfig> {

    //packaging types
    private static final String ZIP = "zip";
    private static final String NONE = "none";
    private static final String EXCELSIOR_INSTALLER = "excelsior-installer";
    private static final String OSX_APP_BUNDLE = "osx-app-bundle";
    private static final String NATIVE_BUNDLE = "native-bundle";

    public JetTask(JetTaskConfig config) {
        super(config);
    }

    private static final String APP_DIR = "app";

    private void checkVersionInfo(JetHome jetHome) throws JetHomeException {
        if (!Utils.isWindows()) {
            config.setAddWindowsVersionInfo(false);
        }
        if (config.isAddWindowsVersionInfo() && (jetHome.getEdition() == JetEdition.STANDARD)) {
            config.log().warn(s("JetMojo.NoVersionInfoInStandard.Warning"));
            config.setAddWindowsVersionInfo(false);
        }
        if (config.isAddWindowsVersionInfo() || EXCELSIOR_INSTALLER.equals(config.excelsiorJetPackaging()) || OSX_APP_BUNDLE.equals(config.excelsiorJetPackaging())) {
            if (Utils.isEmpty(config.vendor())) {
                //no organization name. Get it from groupId that cannot be empty.
                String[] groupId = config.groupId().split("\\.");
                if (groupId.length >= 2) {
                    config.setVendor(groupId[1]);
                } else {
                    config.setVendor(groupId[0]);
                }
                config.setVendor(Character.toUpperCase(config.vendor().charAt(0)) + config.vendor().substring(1));
            }
            if (Utils.isEmpty(config.product())) {
                // no project name, get it from artifactId.
                config.setProduct(config.artifactId());
            }
        }
        if (config.isAddWindowsVersionInfo()) {
            //Coerce winVIVersion to v1.v2.v3.v4 format.
            String finalVersion = deriveFourDigitVersion(config.winVIVersion());
            if (!config.winVIVersion().equals(finalVersion)) {
                config.log().warn(s("JetMojo.NotCompatibleExeVersion.Warning", config.winVIVersion(), finalVersion));
                config.setWinVIVersion(finalVersion);
            }

            if (config.winVICopyright() == null) {
                String inceptionYear = config.inceptionYear();
                String curYear = new SimpleDateFormat("yyyy").format(new Date());
                String years = Utils.isEmpty(inceptionYear)? curYear : inceptionYear + "," + curYear;
                config.setWinVICopyright("Copyright \\x00a9 " + years + " " + config.vendor());
            }
            if (config.winVIDescription() == null) {
                config.setWinVIDescription(config.product());
            }
        }
    }

    private String deriveFourDigitVersion(String version) {
        String[] versions = version.split("\\.");
        String[] finalVersions = new String[]{"0", "0", "0", "0"};
        for (int i = 0; i < Math.min(versions.length, 4); ++i) {
            try {
                finalVersions[i] = Integer.decode(versions[i]).toString();
            } catch (NumberFormatException e) {
                int minusPos = versions[i].indexOf('-');
                if (minusPos > 0) {
                    String v = versions[i].substring(0, minusPos);
                    try {
                        finalVersions[i] = Integer.decode(v).toString();
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return String.join(".", finalVersions);
    }

    private void checkGlobalAndSlimDownParameters(JetHome jetHome) throws JetHomeException, ExcelsiorJetApiException {
        if (config.globalOptimizer()) {
            if (jetHome.is64bit()) {
                config.log().warn(s("JetMojo.NoGlobalIn64Bit.Warning"));
                config.setGlobalOptimizer(false);
            } else if (jetHome.getEdition() == JetEdition.STANDARD) {
                config.log().warn(s("JetMojo.NoGlobalInStandard.Warning"));
                config.setGlobalOptimizer(false);
            }
        }

        if ((config.javaRuntimeSlimDown() != null) && !config.javaRuntimeSlimDown().isEnabled()) {
            config.setJavaRuntimeSlimDown(null);
        }

        if (config.javaRuntimeSlimDown() != null) {
            if (jetHome.is64bit()) {
                config.log().warn(s("JetMojo.NoSlimDownIn64Bit.Warning"));
                config.setJavaRuntimeSlimDown(null);
            } else if (jetHome.getEdition() == JetEdition.STANDARD) {
                config.log().warn(s("JetMojo.NoSlimDownInStandard.Warning"));
                config.setJavaRuntimeSlimDown(null);
            } else {
                if (config.javaRuntimeSlimDown().detachedBaseURL == null) {
                    throw new ExcelsiorJetApiException(s("JetMojo.DetachedBaseURLMandatory.Failure"));
                }

                if (config.javaRuntimeSlimDown().detachedPackage == null) {
                    config.javaRuntimeSlimDown().detachedPackage = config.finalName() + ".pkl";
                }

                config.setGlobalOptimizer(true);
            }

        }

        if (config.globalOptimizer()) {
            TestRunExecProfiles execProfiles = new TestRunExecProfiles(config.execProfilesDir(), config.execProfilesName());
            if (!execProfiles.getUsg().exists()) {
                throw new ExcelsiorJetApiException(s("JetMojo.NoTestRun.Failure"));
            }
        }
    }

    private void checkTrialVersionConfig(JetHome jetHome) throws ExcelsiorJetApiException, JetHomeException {
        if ((config.trialVersion() != null) && config.trialVersion().isEnabled()) {
            if ((config.trialVersion().expireInDays >= 0) && (config.trialVersion().expireDate != null)) {
                throw new ExcelsiorJetApiException(s("JetMojo.AmbiguousExpireSetting.Failure"));
            }
            if (config.trialVersion().expireMessage == null || config.trialVersion().expireMessage.isEmpty()) {
                throw new ExcelsiorJetApiException(s("JetMojo.NoExpireMessage.Failure"));
            }

            if (jetHome.getEdition() == JetEdition.STANDARD) {
                config.log().warn(s("JetMojo.NoTrialsInStandard.Warning"));
                config.setTrialVersion(null);
            }
        } else {
            config.setTrialVersion(null);
        }
    }

    private void checkExcelsiorInstallerConfig() throws ExcelsiorJetApiException {
        if (config.excelsiorJetPackaging().equals(EXCELSIOR_INSTALLER)) {
            config.excelsiorInstallerConfiguration().fillDefaults(config);
        }
    }

    private void checkOSXBundleConfig() {
        if (config.excelsiorJetPackaging().equals(OSX_APP_BUNDLE)) {
            String fourDigitVersion = deriveFourDigitVersion(config.version());
            config.osxBundleConfiguration().fillDefaults(config, config.outputName(), config.product(),
                    deriveFourDigitVersion(config.version()),
                    deriveFourDigitVersion(fourDigitVersion.substring(0, fourDigitVersion.lastIndexOf('.'))));
            if (!config.osxBundleConfiguration().icon.exists()) {
                config.log().warn(s("JetMojo.NoIconForOSXAppBundle.Warning"));
            }
        }

    }

    @Override
    protected JetHome checkPrerequisites() throws ExcelsiorJetApiException {
        JetHome jetHomeObj = super.checkPrerequisites();

        switch (appType) {
            case PLAIN:
                //normalize main and set outputName
                config.setMainClass(config.mainClass().replace('.', '/'));
                if (config.outputName() == null) {
                    int lastSlash = config.mainClass().lastIndexOf('/');
                    config.setOutputName(lastSlash < 0 ? config.mainClass() : config.mainClass().substring(lastSlash + 1));
                }
                break;
            case TOMCAT:
                if (config.outputName() == null) {
                    config.setOutputName(config.artifactId());
                }
                break;
            default:
                throw new AssertionError("Unknown application type");
        }

        //check packaging type
        switch (config.excelsiorJetPackaging()) {
            case ZIP: case NONE: break;
            case EXCELSIOR_INSTALLER:
                if (Utils.isOSX()) {
                    config.log().warn(s("JetMojo.NoExcelsiorInstallerOnOSX.Warning"));
                    config.setExcelsiorJetPackaging(ZIP);
                }
                break;
            case OSX_APP_BUNDLE:
                if (!Utils.isOSX()) {
                    config.log().warn(s("JetMojo.OSXBundleOnNotOSX.Warning"));
                    config.setExcelsiorJetPackaging(ZIP);
                }
                break;

            case NATIVE_BUNDLE:
                if (Utils.isOSX()) {
                    config.setExcelsiorJetPackaging(OSX_APP_BUNDLE);
                } else {
                    config.setExcelsiorJetPackaging(EXCELSIOR_INSTALLER);
                }
                break;

            default: throw new ExcelsiorJetApiException(s("JetMojo.UnknownPackagingMode.Failure", config.excelsiorJetPackaging()));
        }

        // check version info
        try {
            checkVersionInfo(jetHomeObj);

            if (config.multiApp() && (jetHomeObj.getEdition() == JetEdition.STANDARD)) {
                config.log().warn(s("JetMojo.NoMultiappInStandard.Warning"));
                config.setMultiApp(false);
            }

            if (config.profileStartup()) {
                if (jetHomeObj.getEdition() == JetEdition.STANDARD) {
                    config.log().warn(s("JetMojo.NoStartupAcceleratorInStandard.Warning"));
                    config.setProfileStartup(false);
                } else if (Utils.isOSX()) {
                    config.log().warn(s("JetMojo.NoStartupAcceleratorOnOSX.Warning"));
                    config.setProfileStartup(false);
                }
            }

            if (config.protectData()) {
                if (jetHomeObj.getEdition() == JetEdition.STANDARD) {
                    throw new ExcelsiorJetApiException(s("JetMojo.NoDataProtectionInStandard.Failure"));
                } else {
                    if (config.cryptSeed() == null) {
                        config.setCryptSeed(Utils.randomAlphanumeric(64));
                    }
                }
            }

            checkTrialVersionConfig(jetHomeObj);

            checkGlobalAndSlimDownParameters(jetHomeObj);

            checkExcelsiorInstallerConfig();

            checkOSXBundleConfig();

        } catch (JetHomeException e) {
            throw new ExcelsiorJetApiException(e.getMessage());
        }

        return jetHomeObj;
    }

    private String createJetCompilerProject(File buildDir, ArrayList<String> compilerArgs, List<Dependency> dependencies, ArrayList<String> modules) throws ExcelsiorJetApiException {
        String prj = config.outputName() + ".prj";
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File (buildDir, prj)))))
        {
            compilerArgs.forEach(out::println);
            for (Dependency dep: dependencies) {
                out.println("!classpathentry " + dep.dependency);
                out.println("  -optimize=" + (dep.isLib?"autodetect":"all"));
                out.println("  -protect=" + (dep.isLib?"nomatter":"all"));
                out.println("!end");
            }
            for(String mod: modules) {
                out.println("!module " + mod);
            }
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(e.getMessage());
        }
        return prj;
    }

    /**
     * Invokes the Excelsior JET AOT compiler.
     */
    private void compile(JetHome jetHome, File buildDir, List<Dependency> dependencies) throws ExcelsiorJetApiException, CmdLineToolException {
        ArrayList<String> compilerArgs = new ArrayList<>();
        ArrayList<String> modules = new ArrayList<>();

        switch (appType) {
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
            default: throw new AssertionError("Unknown app type");
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
            jetVMPropOpt = jetVMPropOpt + String.join(" ", config.jvmArgs());

            // JVM args may contain $(Root) prefix for system property value
            // (that should expand to installation directory location).
            // However JET compiler replaces such occurrences with s value of "Root" equation if the "$(Root)" is
            // used in the project file.
            // So we need to pass jetvmprop as separate compiler argument as workaround.
            // We also write the equation in commented form to the project in order to see it in the technical support.
            compilerArgs.add("%"+jetVMPropOpt);
        }

        String prj = createJetCompilerProject(buildDir, compilerArgs, dependencies, modules);

        if (new JetCompiler(jetHome, "=p", prj, jetVMPropOpt)
                .workingDirectory(buildDir).withLog(AbstractLog.instance()).execute() != 0) {
            throw new ExcelsiorJetApiException(s("JetMojo.Build.Failure"));
        }
    }

    private ArrayList<String> getCommonXPackArgs() {
        ArrayList<String> xpackArgs = new ArrayList<>();

        switch (appType) {
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
                    config.log().warn(s("JetMojo.PackageFilesIgnoredForTomcat.Warning"));
                }
                break;
            default: throw new AssertionError("Unknown app type");
        }

        if (config.optRtFiles() != null && config.optRtFiles().length > 0) {
            xpackArgs.add("-add-opt-rt-files");
            xpackArgs.add(String.join(",", config.optRtFiles()));
        }

        if (config.javaRuntimeSlimDown() != null) {

            xpackArgs.addAll(Arrays.asList(
                    "-detached-base-url", config.javaRuntimeSlimDown().detachedBaseURL,
                    "-detach-components",
                    (config.javaRuntimeSlimDown().detachComponents != null && config.javaRuntimeSlimDown().detachComponents.length > 0)?
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
    private void createAppDir(JetHome jetHome, File buildDir, File appDir) throws CmdLineToolException, ExcelsiorJetApiException {
        ArrayList<String> xpackArgs = getCommonXPackArgs();
        xpackArgs.addAll(Arrays.asList(
                "-target", appDir.getAbsolutePath()
        ));
        if (new JetPackager(jetHome, xpackArgs.toArray(new String[xpackArgs.size()]))
                .workingDirectory(buildDir).withLog(AbstractLog.instance()).execute() != 0) {
            throw new ExcelsiorJetApiException(s("JetMojo.Package.Failure"));
        }
    }

    /**
     * Packages the generated executable and required Excelsior JET runtime files
     * as a excelsior installer file.
     */
    private void packWithEI(JetHome jetHome, File buildDir) throws CmdLineToolException, ExcelsiorJetApiException {
        File target = new File(config.jetOutputDir(), Utils.mangleExeName(config.finalName()));
        ArrayList<String> xpackArgs = getCommonXPackArgs();
        if (config.excelsiorInstallerConfiguration().eula.exists()) {
            xpackArgs.add(config.excelsiorInstallerConfiguration().eulaFlag());
            xpackArgs.add(config.excelsiorInstallerConfiguration().eula.getAbsolutePath());
        }
        if (Utils.isWindows() && config.excelsiorInstallerConfiguration().installerSplash.exists()) {
            xpackArgs.add("-splash"); xpackArgs.add(config.excelsiorInstallerConfiguration().installerSplash.getAbsolutePath());
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
            throw new ExcelsiorJetApiException(s("JetMojo.Package.Failure"));
        }
        config.log().info(s("JetMojo.Build.Success"));
        config.log().info(s("JetMojo.GetEI.Info", target.getAbsolutePath()));
    }


    static void compressZipfile(File sourceDir, File outputFile) throws IOException {
        ZipOutputStream zipFile = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile)));
        compressDirectoryToZipfile(sourceDir.getAbsolutePath(), sourceDir.getAbsolutePath(), zipFile);
        Utils.closeQuietly(zipFile);
    }

    private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws IOException {
        File[] files = new File(sourceDir).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                compressDirectoryToZipfile(rootDir, sourceDir + File.separator + file.getName(), out);
            } else {
                ZipEntry entry = new ZipEntry(file.getAbsolutePath().substring(rootDir.length()+1));
                if (Utils.isUnix() && file.canExecute()) {
                    // todo: entry.setUnixMode(0100777);
                }
                out.putNextEntry(entry);
                InputStream in = new BufferedInputStream(new FileInputStream(sourceDir + File.separator +  file.getName()));
                Utils.copy(in, out);
                Utils.closeQuietly(in);
                //out.close();
            }
        }
        out.close();
    }

    private void createOSXAppBundle(JetHome jetHome, File buildDir) throws ExcelsiorJetApiException, CmdLineToolException {
        File appBundle = new File(config.jetOutputDir(), config.osxBundleConfiguration().fileName + ".app");
        mkdir(appBundle);
        try {
            Utils.cleanDirectory(appBundle);
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(e.getMessage(), e);
        }
        File contents = new File (appBundle, "Contents");
        mkdir(contents);
        File contentsMacOs = new File(contents, "MacOS");
        mkdir(contentsMacOs);
        File contentsResources = new File (contents, "Resources");
        mkdir(contentsResources);

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(new File (contents, "Info.plist")), "UTF-8")))
        {
            out.print (
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
                            "  <string>" + config.osxBundleConfiguration().identifier +"</string>\n" +
                            "  <key>CFBundleVersionString</key>\n" +
                            "  <string>"+ config.osxBundleConfiguration().version + "</string>\n" +
                            "  <key>CFBundleShortVersionString</key>\n" +
                            "  <string>"+ config.osxBundleConfiguration().shortVersion + "</string>\n" +
                            (config.osxBundleConfiguration().icon.exists()?
                                    "  <key>CFBundleIconFile</key>\n" +
                                            "  <string>" + config.osxBundleConfiguration().icon.getName() + "</string>\n" : "") +
                            (config.osxBundleConfiguration().highResolutionCapable?
                                    "  <key>NSHighResolutionCapable</key>\n" +
                                            "  <true/>" : "") +
                            "</dict>\n" +
                            "</plist>\n");
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(e.getMessage());
        }

        ArrayList<String> xpackArgs = getCommonXPackArgs();
        xpackArgs.addAll(Arrays.asList(
                "-target", contentsMacOs.getAbsolutePath()
        ));
        if (new JetPackager(jetHome, xpackArgs.toArray(new String[xpackArgs.size()]))
                .workingDirectory(buildDir).withLog(AbstractLog.instance()).execute() != 0) {
            throw new ExcelsiorJetApiException(s("JetMojo.Package.Failure"));
        }

        if (config.osxBundleConfiguration().icon.exists()) {
            try {
                Files.copy(config.osxBundleConfiguration().icon.toPath(),
                        new File(contentsResources, config.osxBundleConfiguration().icon.getName()).toPath());
            } catch (IOException e) {
                throw new ExcelsiorJetApiException(e.getMessage(), e);
            }
        }

        File appPkg = null;
        if (config.osxBundleConfiguration().developerId != null) {
            config.log().info(s("JetMojo.SigningOSXBundle.Info"));
            if (new CmdLineTool("codesign", "--verbose", "--force", "--deep", "--sign",
                    config.osxBundleConfiguration().developerId, appBundle.getAbsolutePath()).withLog(AbstractLog.instance()).execute() != 0) {
                throw new ExcelsiorJetApiException(s("JetMojo.OSX.CodeSign.Failure"));
            }
            config.log().info(s("JetMojo.CreatingOSXInstaller.Info"));
            if (config.osxBundleConfiguration().publisherId != null) {
                appPkg = new File(config.jetOutputDir(), config.finalName() + ".pkg");
                if (new CmdLineTool("productbuild", "--sign", config.osxBundleConfiguration().publisherId,
                        "--component", appBundle.getAbsolutePath(), config.osxBundleConfiguration().installPath,
                        appPkg.getAbsolutePath())
                        .withLog(AbstractLog.instance()).execute() != 0) {
                    throw new ExcelsiorJetApiException(s("JetMojo.OSX.Packaging.Failure"));
                }
            } else {
                config.log().warn(s("JetMojo.NoPublisherId.Warning"));
            }
        } else {
            config.log().warn(s("JetMojo.NoDeveloperId.Warning"));
        }
        config.log().info(s("JetMojo.Build.Success"));
        if (appPkg != null) {
            config.log().info(s("JetMojo.GetOSXPackage.Info", appPkg.getAbsolutePath()));
        } else {
            config.log().info(s("JetMojo.GetOSXBundle.Info", appBundle.getAbsolutePath()));
        }

    }

    private void packageBuild(JetHome jetHome, File buildDir, File packageDir) throws IOException, ExcelsiorJetApiException, CmdLineToolException {
        switch (config.excelsiorJetPackaging()){
            case ZIP:
                config.log().info(s("JetMojo.ZipApp.Info"));
                File targetZip = new File(config.jetOutputDir(), config.finalName() + ".zip");
                compressZipfile(packageDir, targetZip);
                config.log().info(s("JetMojo.Build.Success"));
                config.log().info(s("JetMojo.GetZip.Info", targetZip.getAbsolutePath()));
                break;
            case EXCELSIOR_INSTALLER:
                packWithEI(jetHome, buildDir);
                break;
            case OSX_APP_BUNDLE:
                createOSXAppBundle(jetHome, buildDir);
                break;
            default:
                config.log().info(s("JetMojo.Build.Success"));
                config.log().info(s("JetMojo.GetDir.Info", packageDir.getAbsolutePath()));
        }

        if (config.javaRuntimeSlimDown() != null) {
            config.log().info(s("JetMojo.SlimDown.Info", new File(config.jetOutputDir(), config.javaRuntimeSlimDown().detachedPackage),
                    config.javaRuntimeSlimDown().detachedBaseURL));
        }
    }

    public void execute() throws ExcelsiorJetApiException {
        JetHome jetHome = checkPrerequisites();

        // creating output dirs
        File buildDir = createBuildDir();

        File appDir = new File(config.jetOutputDir(), APP_DIR);
        //cleanup packageDir
        try {
            Utils.cleanDirectory(appDir);
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(e.getMessage(), e);
        }

        try {
            switch (appType) {
                case PLAIN:
                    compile(jetHome, buildDir, copyDependencies(buildDir, config.mainJar()));
                    break;
                case TOMCAT:
                    copyTomcatAndWar();
                    compile(jetHome, buildDir, Collections.emptyList());
                    break;
                default:
                    throw new AssertionError("Unknown application type");
            }
            createAppDir(jetHome, buildDir, appDir);

            packageBuild(jetHome, buildDir, appDir);

        } catch (Exception e) {
            e.printStackTrace();
            config.log().error(e.getMessage());
            throw new ExcelsiorJetApiException(s("JetMojo.Unexpected.Error"), e);
        }
    }
}
