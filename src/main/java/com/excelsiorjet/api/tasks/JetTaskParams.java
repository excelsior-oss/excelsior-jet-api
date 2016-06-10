package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.*;

import java.io.File;
import java.util.stream.Stream;

public class JetTaskParams {
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
    private boolean addWindowsVersionInfo;
    private String excelsiorJetPackaging;
    private String vendor;
    private String product;
    private String artifactId;
    private String winVIVersion;
    private String winVICopyright;
    private String inceptionYear;
    private String winVIDescription;
    private boolean globalOptimizer;
    private SlimDownConfig javaRuntimeSlimDown;
    private TrialVersionConfig trialVersion;
    private ExcelsiorInstallerConfig excelsiorInstallerConfiguration;
    private String version;
    private OSXAppBundleConfig osxBundleConfiguration;
    private String outputName;
    private boolean multiApp;
    private boolean profileStartup;
    private boolean protectData;
    private String cryptSeed;
    private File icon;
    private boolean hideConsole;
    private int profileStartupTimeout;
    private String[] optRtFiles;
    private File jetOutputDir;

    public JetTaskParams setMainWar(File mainWar) {
        this.mainWar = mainWar;
        return this;
    }

    public JetTaskParams setJetHome(String jetHome) {
        this.jetHome = jetHome;
        return this;
    }

    public JetTaskParams setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public JetTaskParams setMainJar(File mainJar) {
        this.mainJar = mainJar;
        return this;
    }

    public JetTaskParams setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public JetTaskParams setTomcatConfiguration(TomcatConfig tomcatConfiguration) {
        this.tomcatConfiguration = tomcatConfiguration;
        return this;
    }

    public JetTaskParams setArtifacts(Stream<ClasspathEntry> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public JetTaskParams setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public JetTaskParams setBuildDir(File buildDir) {
        this.buildDir = buildDir;
        return this;
    }

    public JetTaskParams setFinalName(String finalName) {
        this.finalName = finalName;
        return this;
    }

    public JetTaskParams setBasedir(File basedir) {
        this.basedir = basedir;
        return this;
    }

    public JetTaskParams setPackageFilesDir(File packageFilesDir) {
        this.packageFilesDir = packageFilesDir;
        return this;
    }

    public JetTaskParams setExecProfilesDir(File execProfilesDir) {
        this.execProfilesDir = execProfilesDir;
        return this;
    }

    public JetTaskParams setExecProfilesName(String execProfilesName) {
        this.execProfilesName = execProfilesName;
        return this;
    }

    public JetTaskParams setJvmArgs(String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public JetTaskParams setAddWindowsVersionInfo(boolean addWindowsVersionInfo) {
        this.addWindowsVersionInfo = addWindowsVersionInfo;
        return this;
    }

    public JetTaskParams setExcelsiorJetPackaging(String excelsiorJetPackaging) {
        this.excelsiorJetPackaging = excelsiorJetPackaging;
        return this;
    }

    public JetTaskParams setVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public JetTaskParams setProduct(String product) {
        this.product = product;
        return this;
    }

    public JetTaskParams setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public JetTaskParams setWinVIVersion(String winVIVersion) {
        this.winVIVersion = winVIVersion;
        return this;
    }

    public JetTaskParams setWinVICopyright(String winVICopyright) {
        this.winVICopyright = winVICopyright;
        return this;
    }

    public JetTaskParams setInceptionYear(String inceptionYear) {
        this.inceptionYear = inceptionYear;
        return this;
    }

    public JetTaskParams setWinVIDescription(String winVIDescription) {
        this.winVIDescription = winVIDescription;
        return this;
    }

    public JetTaskParams setGlobalOptimizer(boolean globalOptimizer) {
        this.globalOptimizer = globalOptimizer;
        return this;
    }

    public JetTaskParams setJavaRuntimeSlimDown(SlimDownConfig javaRuntimeSlimDown) {
        this.javaRuntimeSlimDown = javaRuntimeSlimDown;
        return this;
    }

    public JetTaskParams setTrialVersion(TrialVersionConfig trialVersion) {
        this.trialVersion = trialVersion;
        return this;
    }

    public JetTaskParams setExcelsiorInstallerConfiguration(ExcelsiorInstallerConfig excelsiorInstallerConfiguration) {
        this.excelsiorInstallerConfiguration = excelsiorInstallerConfiguration;
        return this;
    }

    public JetTaskParams setVersion(String version) {
        this.version = version;
        return this;
    }

    public JetTaskParams setOsxBundleConfiguration(OSXAppBundleConfig osxBundleConfiguration) {
        this.osxBundleConfiguration = osxBundleConfiguration;
        return this;
    }

    public JetTaskParams setOutputName(String outputName) {
        this.outputName = outputName;
        return this;
    }

    public JetTaskParams setMultiApp(boolean multiApp) {
        this.multiApp = multiApp;
        return this;
    }

    public JetTaskParams setProfileStartup(boolean profileStartup) {
        this.profileStartup = profileStartup;
        return this;
    }

    public JetTaskParams setProtectData(boolean protectData) {
        this.protectData = protectData;
        return this;
    }

    public JetTaskParams setCryptSeed(String cryptSeed) {
        this.cryptSeed = cryptSeed;
        return this;
    }

    public JetTaskParams setIcon(File icon) {
        this.icon = icon;
        return this;
    }

    public JetTaskParams setHideConsole(boolean hideConsole) {
        this.hideConsole = hideConsole;
        return this;
    }

    public JetTaskParams setProfileStartupTimeout(int profileStartupTimeout) {
        this.profileStartupTimeout = profileStartupTimeout;
        return this;
    }

    public JetTaskParams setOptRtFiles(String[] optRtFiles) {
        this.optRtFiles = optRtFiles;
        return this;
    }

    public JetTaskParams setJetOutputDir(File jetOutputDir) {
        this.jetOutputDir = jetOutputDir;
        return this;
    }

    public JetTaskConfig createJetTaskConfig() {
        return new JetTaskConfig(mainWar, jetHome, packaging, mainJar, mainClass, tomcatConfiguration, artifacts, groupId, buildDir, finalName, basedir, packageFilesDir, execProfilesDir, execProfilesName, jvmArgs, addWindowsVersionInfo, excelsiorJetPackaging, vendor, product, artifactId, winVIVersion, winVICopyright, inceptionYear, winVIDescription, globalOptimizer, javaRuntimeSlimDown, trialVersion, excelsiorInstallerConfiguration, version, osxBundleConfiguration, outputName, multiApp, profileStartup, protectData, cryptSeed, icon, hideConsole, profileStartupTimeout, optRtFiles, jetOutputDir);
    }
}