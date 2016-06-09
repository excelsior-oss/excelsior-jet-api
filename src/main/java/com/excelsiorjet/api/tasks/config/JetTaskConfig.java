package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.cmd.JetEdition;
import com.excelsiorjet.api.cmd.JetHome;
import com.excelsiorjet.api.cmd.JetHomeException;
import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.log.AbstractLog;
import com.excelsiorjet.api.tasks.ExcelsiorJetApiException;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.excelsiorjet.api.util.Txt.s;

public interface JetTaskConfig extends AbstractJetTaskConfig {

    //packaging types
    String ZIP = "zip";
    String NONE = "none";
    String EXCELSIOR_INSTALLER = "excelsior-installer";
    String OSX_APP_BUNDLE = "osx-app-bundle";
    String NATIVE_BUNDLE = "native-bundle";

    boolean isAddWindowsVersionInfo();

    void setAddWindowsVersionInfo(boolean addWindowsVersionInfoFlag);

    String excelsiorJetPackaging();

    void setExcelsiorJetPackaging(String excelsiorJetPackagin);

    String vendor();

    void setVendor(String vendor);

    String product();

    void setProduct(String product);

    String artifactId();

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

    default JetHome validate() throws ExcelsiorJetApiException {

        JetHome jetHomeObj = AbstractJetTaskConfig.super.validate();//super.validate();

        switch (appType()) {
            case PLAIN:
                if (outputName() == null) {
                    int lastSlash = mainClass().lastIndexOf('/');
                    setOutputName(lastSlash < 0 ? mainClass() : mainClass().substring(lastSlash + 1));
                }
                break;
            case TOMCAT:
                if (outputName() == null) {
                    setOutputName(artifactId());
                }
                break;
            default:
                throw new AssertionError("Unknown application type");
        }

        //check packaging type
        switch (excelsiorJetPackaging()) {
            case ZIP:
            case NONE:
                break;
            case EXCELSIOR_INSTALLER:
                if (Utils.isOSX()) {
                    AbstractLog.instance().warn(s("JetMojo.NoExcelsiorInstallerOnOSX.Warning"));
                    setExcelsiorJetPackaging(ZIP);
                }
                break;
            case OSX_APP_BUNDLE:
                if (!Utils.isOSX()) {
                    AbstractLog.instance().warn(s("JetMojo.OSXBundleOnNotOSX.Warning"));
                    setExcelsiorJetPackaging(ZIP);
                }
                break;

            case NATIVE_BUNDLE:
                if (Utils.isOSX()) {
                    setExcelsiorJetPackaging(OSX_APP_BUNDLE);
                } else {
                    setExcelsiorJetPackaging(EXCELSIOR_INSTALLER);
                }
                break;

            default:
                throw new ExcelsiorJetApiException(s("JetMojo.UnknownPackagingMode.Failure", excelsiorJetPackaging()));
        }

        // check version info
        try {
            checkVersionInfo(jetHomeObj);

            if (multiApp() && (jetHomeObj.getEdition() == JetEdition.STANDARD)) {
                AbstractLog.instance().warn(s("JetMojo.NoMultiappInStandard.Warning"));
                setMultiApp(false);
            }

            if (profileStartup()) {
                if (jetHomeObj.getEdition() == JetEdition.STANDARD) {
                    AbstractLog.instance().warn(s("JetMojo.NoStartupAcceleratorInStandard.Warning"));
                    setProfileStartup(false);
                } else if (Utils.isOSX()) {
                    AbstractLog.instance().warn(s("JetMojo.NoStartupAcceleratorOnOSX.Warning"));
                    setProfileStartup(false);
                }
            }

            if (protectData()) {
                if (jetHomeObj.getEdition() == JetEdition.STANDARD) {
                    throw new ExcelsiorJetApiException(s("JetMojo.NoDataProtectionInStandard.Failure"));
                } else {
                    if (cryptSeed() == null) {
                        setCryptSeed(Utils.randomAlphanumeric(64));
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

    default void checkVersionInfo(JetHome jetHome) throws JetHomeException {
        if (!Utils.isWindows()) {
            setAddWindowsVersionInfo(false);
        }
        if (isAddWindowsVersionInfo() && (jetHome.getEdition() == JetEdition.STANDARD)) {
            AbstractLog.instance().warn(s("JetMojo.NoVersionInfoInStandard.Warning"));
            setAddWindowsVersionInfo(false);
        }
        if (isAddWindowsVersionInfo() || EXCELSIOR_INSTALLER.equals(excelsiorJetPackaging()) || OSX_APP_BUNDLE.equals(excelsiorJetPackaging())) {
            if (Utils.isEmpty(vendor())) {
                //no organization name. Get it from groupId that cannot be empty.
                String[] groupId = groupId().split("\\.");
                if (groupId.length >= 2) {
                    setVendor(groupId[1]);
                } else {
                    setVendor(groupId[0]);
                }
                setVendor(Character.toUpperCase(vendor().charAt(0)) + vendor().substring(1));
            }
            if (Utils.isEmpty(product())) {
                // no project name, get it from artifactId.
                setProduct(artifactId());
            }
        }
        if (isAddWindowsVersionInfo()) {
            //Coerce winVIVersion to v1.v2.v3.v4 format.
            String finalVersion = deriveFourDigitVersion(winVIVersion());
            if (!winVIVersion().equals(finalVersion)) {
                AbstractLog.instance().warn(s("JetMojo.NotCompatibleExeVersion.Warning", winVIVersion(), finalVersion));
                setWinVIVersion(finalVersion);
            }

            if (winVICopyright() == null) {
                String inceptionYear = inceptionYear();
                String curYear = new SimpleDateFormat("yyyy").format(new Date());
                String years = Utils.isEmpty(inceptionYear) ? curYear : inceptionYear + "," + curYear;
                setWinVICopyright("Copyright \\x00a9 " + years + " " + vendor());
            }
            if (winVIDescription() == null) {
                setWinVIDescription(product());
            }
        }
    }

    default String deriveFourDigitVersion(String version) {
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

    default void checkGlobalAndSlimDownParameters(JetHome jetHome) throws JetHomeException, ExcelsiorJetApiException {
        if (globalOptimizer()) {
            if (jetHome.is64bit()) {
                AbstractLog.instance().warn(s("JetMojo.NoGlobalIn64Bit.Warning"));
                setGlobalOptimizer(false);
            } else if (jetHome.getEdition() == JetEdition.STANDARD) {
                AbstractLog.instance().warn(s("JetMojo.NoGlobalInStandard.Warning"));
                setGlobalOptimizer(false);
            }
        }

        if ((javaRuntimeSlimDown() != null) && !javaRuntimeSlimDown().isEnabled()) {
            setJavaRuntimeSlimDown(null);
        }

        if (javaRuntimeSlimDown() != null) {
            if (jetHome.is64bit()) {
                AbstractLog.instance().warn(s("JetMojo.NoSlimDownIn64Bit.Warning"));
                setJavaRuntimeSlimDown(null);
            } else if (jetHome.getEdition() == JetEdition.STANDARD) {
                AbstractLog.instance().warn(s("JetMojo.NoSlimDownInStandard.Warning"));
                setJavaRuntimeSlimDown(null);
            } else {
                if (javaRuntimeSlimDown().detachedBaseURL == null) {
                    throw new ExcelsiorJetApiException(s("JetMojo.DetachedBaseURLMandatory.Failure"));
                }

                if (javaRuntimeSlimDown().detachedPackage == null) {
                    javaRuntimeSlimDown().detachedPackage = finalName() + ".pkl";
                }

                setGlobalOptimizer(true);
            }

        }

        if (globalOptimizer()) {
            TestRunExecProfiles execProfiles = new TestRunExecProfiles(execProfilesDir(), execProfilesName());
            if (!execProfiles.getUsg().exists()) {
                throw new ExcelsiorJetApiException(s("JetMojo.NoTestRun.Failure"));
            }
        }
    }

    default void checkTrialVersionConfig(JetHome jetHome) throws ExcelsiorJetApiException, JetHomeException {
        if ((trialVersion() != null) && trialVersion().isEnabled()) {
            if ((trialVersion().expireInDays >= 0) && (trialVersion().expireDate != null)) {
                throw new ExcelsiorJetApiException(s("JetMojo.AmbiguousExpireSetting.Failure"));
            }
            if (trialVersion().expireMessage == null || trialVersion().expireMessage.isEmpty()) {
                throw new ExcelsiorJetApiException(s("JetMojo.NoExpireMessage.Failure"));
            }

            if (jetHome.getEdition() == JetEdition.STANDARD) {
                AbstractLog.instance().warn(s("JetMojo.NoTrialsInStandard.Warning"));
                setTrialVersion(null);
            }
        } else {
            setTrialVersion(null);
        }
    }

    default void checkExcelsiorInstallerConfig() throws ExcelsiorJetApiException {
        if (excelsiorJetPackaging().equals(EXCELSIOR_INSTALLER)) {
            excelsiorInstallerConfiguration().fillDefaults(this);
        }
    }

    default void checkOSXBundleConfig() {
        if (excelsiorJetPackaging().equals(OSX_APP_BUNDLE)) {
            String fourDigitVersion = deriveFourDigitVersion(version());
            osxBundleConfiguration().fillDefaults(this, outputName(), product(),
                    deriveFourDigitVersion(version()),
                    deriveFourDigitVersion(fourDigitVersion.substring(0, fourDigitVersion.lastIndexOf('.'))));
            if (!osxBundleConfiguration().icon.exists()) {
                AbstractLog.instance().warn(s("JetMojo.NoIconForOSXAppBundle.Warning"));
            }
        }

    }
}
