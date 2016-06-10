package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.cmd.JetEdition;
import com.excelsiorjet.api.cmd.JetHome;
import com.excelsiorjet.api.cmd.JetHomeException;
import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.log.AbstractLog;
import com.excelsiorjet.api.tasks.config.*;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import static com.excelsiorjet.api.util.Txt.s;

public class JetTaskParams extends BaseJetTaskParams {

    //packaging types
    public static final String ZIP = "zip";
    private static final String NONE = "none";
    static final String EXCELSIOR_INSTALLER = "excelsior-installer";
    static final String OSX_APP_BUNDLE = "osx-app-bundle";
    private static final String NATIVE_BUNDLE = "native-bundle";

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

    JetTaskParams(File mainWar, String jetHome, String packaging, File mainJar, String mainClass, TomcatConfig tomcatConfiguration, Stream<ClasspathEntry> artifacts, String groupId, File buildDir, String finalName, File basedir, File packageFilesDir, File execProfilesDir, String execProfilesName, String[] jvmArgs, boolean addWindowsVersionInfo, String excelsiorJetPackaging, String vendor, String product, String artifactId, String winVIVersion, String winVICopyright, String inceptionYear, String winVIDescription, boolean globalOptimizer, SlimDownConfig javaRuntimeSlimDown, TrialVersionConfig trialVersion, ExcelsiorInstallerConfig excelsiorInstallerConfiguration, String version, OSXAppBundleConfig osxBundleConfiguration, String outputName, boolean multiApp, boolean profileStartup, boolean protectData, String cryptSeed, File icon, boolean hideConsole, int profileStartupTimeout, String[] optRtFiles, File jetOutputDir) {
        super(mainWar, jetHome, packaging, mainJar, mainClass, tomcatConfiguration, artifacts, groupId, buildDir, finalName, basedir, packageFilesDir, execProfilesDir, execProfilesName, jvmArgs);
        this.addWindowsVersionInfo = addWindowsVersionInfo;
        this.excelsiorJetPackaging = excelsiorJetPackaging;
        this.vendor = vendor;
        this.product = product;
        this.artifactId = artifactId;
        this.winVIVersion = winVIVersion;
        this.winVICopyright = winVICopyright;
        this.inceptionYear = inceptionYear;
        this.winVIDescription = winVIDescription;
        this.globalOptimizer = globalOptimizer;
        this.javaRuntimeSlimDown = javaRuntimeSlimDown;
        this.trialVersion = trialVersion;
        this.excelsiorInstallerConfiguration = excelsiorInstallerConfiguration;
        this.version = version;
        this.osxBundleConfiguration = osxBundleConfiguration;
        this.outputName = outputName;
        this.multiApp = multiApp;
        this.profileStartup = profileStartup;
        this.protectData = protectData;
        this.cryptSeed = cryptSeed;
        this.icon = icon;
        this.hideConsole = hideConsole;
        this.profileStartupTimeout = profileStartupTimeout;
        this.optRtFiles = optRtFiles;
        this.jetOutputDir = jetOutputDir;
    }

    boolean isAddWindowsVersionInfo() {
        return addWindowsVersionInfo;
    }

    private void setAddWindowsVersionInfo(boolean addWindowsVersionInfoFlag) {
        this.addWindowsVersionInfo = addWindowsVersionInfoFlag;
    }

    String excelsiorJetPackaging() {
        return excelsiorJetPackaging;
    }

    private void setExcelsiorJetPackaging(String excelsiorJetPackaging) {
        this.excelsiorJetPackaging = excelsiorJetPackaging;
    }

    public String vendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String product() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String artifactId() {
        return artifactId;
    }

    String winVIVersion() {
        return winVIVersion;
    }

    private void setWinVIVersion(String winVIVersion) {
        this.winVIVersion = winVIVersion;
    }

    String winVICopyright() {
        return winVICopyright;
    }

    private void setWinVICopyright(String winVICopyright) {
        this.winVICopyright = winVICopyright;
    }

    private String inceptionYear() {
        return inceptionYear;
    }

    String winVIDescription() {
        return winVIDescription;
    }

    private void setWinVIDescription(String winVIDescription) {
        this.winVIDescription = winVIDescription;
    }

    boolean globalOptimizer() {
        return globalOptimizer;
    }

    private void setGlobalOptimizer(boolean globalOptimizer) {
        this.globalOptimizer = globalOptimizer;
    }

    SlimDownConfig javaRuntimeSlimDown() {
        return javaRuntimeSlimDown;
    }

    private void setJavaRuntimeSlimDown(SlimDownConfig slimDownConfig) {
        this.javaRuntimeSlimDown = slimDownConfig;
    }

    TrialVersionConfig trialVersion() {
        return trialVersion;
    }

    private void setTrialVersion(TrialVersionConfig trialVersionConfig) {
        this.trialVersion = trialVersionConfig;
    }

    ExcelsiorInstallerConfig excelsiorInstallerConfiguration() {
        return excelsiorInstallerConfiguration;
    }

    public String version() {
        return version;
    }

    OSXAppBundleConfig osxBundleConfiguration() {
        return osxBundleConfiguration;
    }

    String outputName() {
        return outputName;
    }

    private void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    boolean multiApp() {
        return multiApp;
    }

    private void setMultiApp(boolean multiApp) {
        this.multiApp = multiApp;
    }

    public boolean profileStartup() {
        return profileStartup;
    }

    public void setProfileStartup(boolean profileStartup) {
        this.profileStartup = profileStartup;
    }

    boolean protectData() {
        return protectData;
    }

    String cryptSeed() {
        return cryptSeed;
    }

    private void setCryptSeed(String cryptSeed) {
        this.cryptSeed = cryptSeed;
    }

    public File icon() {
        return icon;
    }

    boolean hideConsole() {
        return hideConsole;
    }

    int profileStartupTimeout() {
        return profileStartupTimeout;
    }

    String[] optRtFiles() {
        return optRtFiles;
    }

    File jetOutputDir() {
        return jetOutputDir;
    }

    public JetHome validate() throws JetTaskFailureException {

        JetHome jetHomeObj = super.validate();//super.validate();

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
                throw new JetTaskFailureException(s("JetMojo.UnknownPackagingMode.Failure", excelsiorJetPackaging()));
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
                    throw new JetTaskFailureException(s("JetMojo.NoDataProtectionInStandard.Failure"));
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
            throw new JetTaskFailureException(e.getMessage());
        }

        return jetHomeObj;
    }

    private void checkVersionInfo(JetHome jetHome) throws JetHomeException {
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
        return String.join(".", (CharSequence[]) finalVersions);
    }

    private void checkGlobalAndSlimDownParameters(JetHome jetHome) throws JetHomeException, JetTaskFailureException {
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
                    throw new JetTaskFailureException(s("JetMojo.DetachedBaseURLMandatory.Failure"));
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
                throw new JetTaskFailureException(s("JetMojo.NoTestRun.Failure"));
            }
        }
    }

    private void checkTrialVersionConfig(JetHome jetHome) throws JetTaskFailureException, JetHomeException {
        if ((trialVersion() != null) && trialVersion().isEnabled()) {
            if ((trialVersion().expireInDays >= 0) && (trialVersion().expireDate != null)) {
                throw new JetTaskFailureException(s("JetMojo.AmbiguousExpireSetting.Failure"));
            }
            if (trialVersion().expireMessage == null || trialVersion().expireMessage.isEmpty()) {
                throw new JetTaskFailureException(s("JetMojo.NoExpireMessage.Failure"));
            }

            if (jetHome.getEdition() == JetEdition.STANDARD) {
                AbstractLog.instance().warn(s("JetMojo.NoTrialsInStandard.Warning"));
                setTrialVersion(null);
            }
        } else {
            setTrialVersion(null);
        }
    }

    private void checkExcelsiorInstallerConfig() throws JetTaskFailureException {
        if (excelsiorJetPackaging().equals(EXCELSIOR_INSTALLER)) {
            excelsiorInstallerConfiguration().fillDefaults(this);
        }
    }

    private void checkOSXBundleConfig() {
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
