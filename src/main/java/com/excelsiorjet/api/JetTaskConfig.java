package com.excelsiorjet.api;

import java.io.File;

public interface JetTaskConfig extends AbstractJetTaskConfig {

    void setAddWindowsVersionInfo(boolean addWindowsVersionInfoFlag);

    boolean isAddWindowsVersionInfo();

    String excelsiorJetPackaging();

    void setExcelsiorJetPackaging(String excelsiorJetPackagin);

    String vendor();

    void setVendor(String vendor);

    String product();

    String artifactId();

    void setProduct(String product);

    String winVIVersion();

    void setWinVIVersion(String winVIVersion);

    String winVICopyright();

    void setWinVICopyright(String winVICopyrigth);

    String inceptionYear();

    String winVIDescription();

    void setWinVIDescription(String winVIDescription);

    boolean globalOptimizer();

    void setGlobalOptimizer(boolean globalOptimizer);

    SlimDownConfig javaRuntimeSlimDown();

    void setJavaRuntimeSlimDown(SlimDownConfig slimDownConfig);

    TrialVersionConfig trialVersion();

    void setTrialVersion(TrialVersionConfig trialVersionConfig);

    ExcelsiorInstallerConfig excelsiorInstallerConfiguration();

    String version();

    OSXAppBundleConfig osxBundleConfiguration();

    String outputName();

    void setOutputName(String outputName);

    boolean multiApp();

    void setMultiApp(boolean multiApp);

    boolean profileStartup();

    void setProfileStartup(boolean profileStartup);

    boolean protectData();

    String cryptSeed();

    void setCryptSeed(String cryptSeed);

    File icon();

    boolean hideConsole();

    int profileStartupTimeout();

    String[] optRtFiles();

    File jetOutputDir();
}
