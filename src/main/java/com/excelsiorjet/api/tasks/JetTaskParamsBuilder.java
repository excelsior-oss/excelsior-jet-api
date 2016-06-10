package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.*;

import java.io.File;
import java.util.stream.Stream;

public class JetTaskParamsBuilder {
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

    public JetTaskParamsBuilder setMainWar(File mainWar) {
        this.mainWar = mainWar;
        return this;
    }

    public JetTaskParamsBuilder setJetHome(String jetHome) {
        this.jetHome = jetHome;
        return this;
    }

    public JetTaskParamsBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public JetTaskParamsBuilder setMainJar(File mainJar) {
        this.mainJar = mainJar;
        return this;
    }

    public JetTaskParamsBuilder setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public JetTaskParamsBuilder setTomcatConfiguration(TomcatConfig tomcatConfiguration) {
        this.tomcatConfiguration = tomcatConfiguration;
        return this;
    }

    public JetTaskParamsBuilder setArtifacts(Stream<ClasspathEntry> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public JetTaskParamsBuilder setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public JetTaskParamsBuilder setBuildDir(File buildDir) {
        this.buildDir = buildDir;
        return this;
    }

    public JetTaskParamsBuilder setFinalName(String finalName) {
        this.finalName = finalName;
        return this;
    }

    public JetTaskParamsBuilder setBasedir(File basedir) {
        this.basedir = basedir;
        return this;
    }

    public JetTaskParamsBuilder setPackageFilesDir(File packageFilesDir) {
        this.packageFilesDir = packageFilesDir;
        return this;
    }

    public JetTaskParamsBuilder setExecProfilesDir(File execProfilesDir) {
        this.execProfilesDir = execProfilesDir;
        return this;
    }

    public JetTaskParamsBuilder setExecProfilesName(String execProfilesName) {
        this.execProfilesName = execProfilesName;
        return this;
    }

    public JetTaskParamsBuilder setJvmArgs(String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public JetTaskParamsBuilder setAddWindowsVersionInfo(boolean addWindowsVersionInfo) {
        this.addWindowsVersionInfo = addWindowsVersionInfo;
        return this;
    }

    public JetTaskParamsBuilder setExcelsiorJetPackaging(String excelsiorJetPackaging) {
        this.excelsiorJetPackaging = excelsiorJetPackaging;
        return this;
    }

    public JetTaskParamsBuilder setVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public JetTaskParamsBuilder setProduct(String product) {
        this.product = product;
        return this;
    }

    public JetTaskParamsBuilder setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public JetTaskParamsBuilder setWinVIVersion(String winVIVersion) {
        this.winVIVersion = winVIVersion;
        return this;
    }

    public JetTaskParamsBuilder setWinVICopyright(String winVICopyright) {
        this.winVICopyright = winVICopyright;
        return this;
    }

    public JetTaskParamsBuilder setInceptionYear(String inceptionYear) {
        this.inceptionYear = inceptionYear;
        return this;
    }

    public JetTaskParamsBuilder setWinVIDescription(String winVIDescription) {
        this.winVIDescription = winVIDescription;
        return this;
    }

    public JetTaskParamsBuilder setGlobalOptimizer(boolean globalOptimizer) {
        this.globalOptimizer = globalOptimizer;
        return this;
    }

    public JetTaskParamsBuilder setJavaRuntimeSlimDown(SlimDownConfig javaRuntimeSlimDown) {
        this.javaRuntimeSlimDown = javaRuntimeSlimDown;
        return this;
    }

    public JetTaskParamsBuilder setTrialVersion(TrialVersionConfig trialVersion) {
        this.trialVersion = trialVersion;
        return this;
    }

    public JetTaskParamsBuilder setExcelsiorInstallerConfiguration(ExcelsiorInstallerConfig excelsiorInstallerConfiguration) {
        this.excelsiorInstallerConfiguration = excelsiorInstallerConfiguration;
        return this;
    }

    public JetTaskParamsBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    public JetTaskParamsBuilder setOsxBundleConfiguration(OSXAppBundleConfig osxBundleConfiguration) {
        this.osxBundleConfiguration = osxBundleConfiguration;
        return this;
    }

    public JetTaskParamsBuilder setOutputName(String outputName) {
        this.outputName = outputName;
        return this;
    }

    public JetTaskParamsBuilder setMultiApp(boolean multiApp) {
        this.multiApp = multiApp;
        return this;
    }

    public JetTaskParamsBuilder setProfileStartup(boolean profileStartup) {
        this.profileStartup = profileStartup;
        return this;
    }

    public JetTaskParamsBuilder setProtectData(boolean protectData) {
        this.protectData = protectData;
        return this;
    }

    public JetTaskParamsBuilder setCryptSeed(String cryptSeed) {
        this.cryptSeed = cryptSeed;
        return this;
    }

    public JetTaskParamsBuilder setIcon(File icon) {
        this.icon = icon;
        return this;
    }

    public JetTaskParamsBuilder setHideConsole(boolean hideConsole) {
        this.hideConsole = hideConsole;
        return this;
    }

    public JetTaskParamsBuilder setProfileStartupTimeout(int profileStartupTimeout) {
        this.profileStartupTimeout = profileStartupTimeout;
        return this;
    }

    public JetTaskParamsBuilder setOptRtFiles(String[] optRtFiles) {
        this.optRtFiles = optRtFiles;
        return this;
    }

    public JetTaskParamsBuilder setJetOutputDir(File jetOutputDir) {
        this.jetOutputDir = jetOutputDir;
        return this;
    }

    public JetTaskParams createJetTaskConfig() {
        return new JetTaskParams(mainWar, jetHome, packaging, mainJar, mainClass, tomcatConfiguration, artifacts, groupId, buildDir, finalName, basedir, packageFilesDir, execProfilesDir, execProfilesName, jvmArgs, addWindowsVersionInfo, excelsiorJetPackaging, vendor, product, artifactId, winVIVersion, winVICopyright, inceptionYear, winVIDescription, globalOptimizer, javaRuntimeSlimDown, trialVersion, excelsiorInstallerConfiguration, version, osxBundleConfiguration, outputName, multiApp, profileStartup, protectData, cryptSeed, icon, hideConsole, profileStartupTimeout, optRtFiles, jetOutputDir);
    }
}