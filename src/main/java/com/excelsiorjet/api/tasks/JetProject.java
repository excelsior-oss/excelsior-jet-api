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

import com.excelsiorjet.api.cmd.JetEdition;
import com.excelsiorjet.api.cmd.JetHome;
import com.excelsiorjet.api.cmd.JetHomeException;
import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.log.Log;
import com.excelsiorjet.api.tasks.config.*;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;
import static java.util.Objects.requireNonNull;

/**
 * <p>Collection of Excelsior JET compiler and packager parameters that can be configured by an external build tool,
 * such as Maven or Gradle. More precisely, it is assumed that a plugin into that build tool configures these parameters,
 * so there is a section specific to that plugin in a bigger project, such as {@code pom.xml} or {@code build.gradle},
 * mentioned below as the <em>enclosing</em> project.</p>
 *
 * <p>An instance of this class can be constructed with the build pattern methods and used by Excelsior JET tasks
 * such as {@link JetBuildTask}, {@link TestRunTask}.</p>
 *
 * <p>The class performs validation of the parameters via the {@link #validate()} method.
 * During validation, it sets default parameter values derived from other parameters,
 * so it is not necessary to set all the parameters.
 * That said, some parameters are required. For instance, {@link #mainClass} is required for plain Java SE applications.</p>
 *
 * @see JetBuildTask
 * @see TestRunTask
 */
public class JetProject {

    //packaging types
    public static final String NONE = "none";
    public static final String ZIP = "zip";
    public static final String EXCELSIOR_INSTALLER = "excelsior-installer";
    public static final String OSX_APP_BUNDLE = "osx-app-bundle";
    public static final String NATIVE_BUNDLE = "native-bundle";

    private static final String JET_OUTPUT_DIR = "jet";
    private static final String BUILD_DIR = "build";
    private static final String LIB_DIR = "lib";
    private static final String PACKAGE_FILES_DIR = "packagefiles";

    /**
     * Excelsior JET installation directory.
     * If unspecified, the plugin uses the following algorithm to set the value of this property:
     * <ul>
     *   <li> If the jet.home system property is set, use its value</li>
     *   <li> Otherwise, if the JET_HOME environment variable is set, use its value</li>
     *   <li> Otherwise scan the PATH environment variable for a suitable Excelsior JET installation</li>
     * </ul>
     */
    private String jetHome;

    /**
     * Name of the project. For Maven, project artifactId is used as project name by default.
     */
    private String projectName;

    /**
     * Project group id. Unique identifier that can be shared by multiple projects.
     * Usually reverse domain name is used for group id such as "com.example".
     */
    private String groupId;

    /**
     * Project version. Required for Excelsior Installer.
     * Note: To specify a different (more precise) version number for the Windows executable version-information resource,
     * use the {@link #winVIVersion} parameter.
     */
    private String version;

    /**
     * Application type. Currently, Plain Java SE Applications and Tomcat Web Applications are supported.
     *
     * @see ApplicationType#PLAIN
     * @see ApplicationType#TOMCAT
     */
    private ApplicationType appType;

    /**
     * Build (target) directory of the enclosing project where Java sources are built into class files
     * and a project artifact. It is assumed that main artifact (jar or war) is placed
     * to this directory before Excelsior JET build.
     */
    private File targetDir;

    /**
     * Directory that contains Excelsior JET specific resource files such as application icons, installer splash,  etc.
     * It is recommended to place the directory in the source root directory,
     * and plugins by default sets the directory to "jetresources" subfolder of the source root directory.
     */
    private File jetResourcesDir;

    /**
     * Directory for temporary files generated during the build process
     * and the target directory for the resulting package.
     * By defualt, it is placed at "jet" subdirectory of {@link #targetDir}
     */
    private File jetOutputDir;

    /**
     * Excelsior project build dir.
     *
     * The value is set to "build" subdirectory of {@link #jetOutputDir}.
     */
    private File jetBuildDir;

    /**
     * Directory containing additional package files - README, license, media, help files, native libraries, and the like.
     * The contents of the directory will be recursively copied to the final application package.
     *
     * By default, the value is set to "packageFiles" subfolder of {@link #jetResourcesDir}
     */
    private File packageFilesDir;

    /**
     * Name of the final artifact of the enclosing project. Used as the default value for {@link #mainJar} and {@link #mainWar},
     * and to derive the default names of final artifacts created by {@link JetBuildTask} such as zip file, installer, and so on.
     */
    private String artifactName;

    /**
     * The main application jar for plain Java SE applications.
     * By default, {@link #artifactName}.jar is used.
     *
     * @see ApplicationType#PLAIN
     */
    private File mainJar;

    /**
     * The main application class for plain Java SE applications.
     *
     * @see ApplicationType#PLAIN
     */
    private String mainClass;

    /**
     * The main web application archive for Tomcat Web applications.
     * By default, {@link #artifactName}.war is used.
     *
     * @see ApplicationType#TOMCAT
     */
    private File mainWar;

    /**
     * Tomcat web applications specific parameters.
     *
     * @see ApplicationType#TOMCAT
     * @see TomcatConfig#tomcatHome
     * @see TomcatConfig#warDeployName
     * @see TomcatConfig#hideConfig
     * @see TomcatConfig#genScripts
     */
    private TomcatConfig tomcatConfiguration;

    /**
     * Project dependencies.
     */
    private List<ClasspathEntry> dependencies;

    /**
     * The target location for application execution profiles gathered during Test Run.
     * It is recommended to commit the collected profiles (.usg, .startup) to VCS to enable the {@code {@link JetBuildTask}}
     * to re-use them during subsequent builds without performing a Test Run.
     *
     * By default, {@link #jetResourcesDir} is used for the directory.
     *
     * @see TestRunTask
     */
    private File execProfilesDir;

    /**
     * The base file name of execution profiles. By default, {@link #projectName} is used.
     */
    private String execProfilesName;

    /**
     * Defines system properties and JVM arguments to be passed to the Excelsior JET JVM at runtime, e.g.:
     * {@code -Dmy.prop1 -Dmy.prop2=value -ea -Xmx1G -Xss128M -Djet.gc.ratio=11}.
     * <p>
     * Please note that only some of the non-standard Oracle HotSpot JVM arguments
     * (those prefixed with {@code -X}) are recognized.
     * For instance, the {@code -Xms} argument setting the initial Java heap size on HotSpot
     * has no meaning for the Excelsior JET JVM, which has a completely different
     * memory management policy. At the same time, Excelsior JET provides its own system properties
     * for GC tuning, such as {@code -Djet.gc.ratio}.
     * </p>
     */
    private String[] jvmArgs;

    /**
     * (Windows) If set to {@code true}, a version-information resource will be added to the final executable.
     *
     * @see #vendor vendor
     * @see #product product
     * @see #winVIVersion winVIVersion
     * @see #winVICopyright winVICopyright
     * @see #winVIDescription winVIDescription
     */
    private boolean addWindowsVersionInfo;

    /**
     * Application packaging mode. Permitted values are:
     * <dl>
     * <dt>zip</dt>
     * <dd>zip archive with a self-contained application package (default)</dd>
     * <dt>excelsior-installer</dt>
     * <dd>self-extracting installer with standard GUI for Windows
     * and command-line interface for Linux</dd>
     * <dt>osx-app-bundle</dt>
     * <dd>OS X application bundle</dd>
     * <dt>native-bundle</dt>
     * <dd>Excelsior Installer setups for Windows and Linux, application bundle for OS X</dd>
     * <dt>none</dt>
     * <dd>skip packaging altogether</dd>
     * </dl>
     */
    private String excelsiorJetPackaging;

    /**
     * Application vendor name. Required for Windows version-information resource and Excelsior Installer.
     */
    private String vendor;

    /**
     * Product name. Required for Windows version-information resource and Excelsior Installer.
     */
    private String product;

    /**
     * (Windows) Version number string for the version-information resource.
     * (Both {@code ProductVersion} and {@code FileVersion} resource strings are set to the same value.)
     * Must have {@code v1.v2.v3.v4} format where {@code vi} is a number.
     * If not set, {@code ${project.version}} is used. If the value does not meet the required format,
     * it is coerced. For instance, "1.2.3-SNAPSHOT" becomes "1.2.3.0"
     *
     * @see #version version
     */
    private String winVIVersion;

    /**
     * (Windows) Legal copyright notice string for the version-information resource.
     * By default, {@code "Copyright © {$project.inceptionYear},[curYear] [vendor]"} is used.
     */
    private String winVICopyright;

    /**
     * Inception year of this project.
     *
     * Used to construct default value of {@link #winVICopyright}.
     */
    private String inceptionYear;

    /**
     * (Windows) File description string for the version-information resource.
     * The value of {@link #product} is used by default.
     */
    private String winVIDescription;

    /**
     * (32-bit only) If set to {@code true}, the Global Optimizer is enabled,
     * providing higher performance and lower memory usage for the compiled application.
     * Performing a Test Run is mandatory when the Global Optimizer is enabled.
     * The Global Optimizer is enabled automatically when you enable Java Runtime Slim-Down.
     *
     * @see TestRunTask
     * @see #javaRuntimeSlimDown
     */
    private boolean globalOptimizer;

    /**
     * (32-bit only) Java Runtime Slim-Down configuration parameters.
     *
     * @see SlimDownConfig#detachedBaseURL
     * @see SlimDownConfig#detachComponents
     * @see SlimDownConfig#detachedPackage
     */
    private SlimDownConfig javaRuntimeSlimDown;

    /**
     * Trial version configuration parameters.
     *
     * @see TrialVersionConfig#expireInDays
     * @see TrialVersionConfig#expireDate
     * @see TrialVersionConfig#expireMessage
     */
    private TrialVersionConfig trialVersion;

    /**
     * Excelsior Installer configuration parameters.
     *
     * @see ExcelsiorInstallerConfig#eula
     * @see ExcelsiorInstallerConfig#eulaEncoding
     * @see ExcelsiorInstallerConfig#installerSplash
     */
    private ExcelsiorInstallerConfig excelsiorInstallerConfiguration;

    /**
     * OS X Application Bundle configuration parameters.
     *
     * @see OSXAppBundleConfig#fileName
     * @see OSXAppBundleConfig#bundleName
     * @see OSXAppBundleConfig#identifier
     * @see OSXAppBundleConfig#shortVersion
     * @see OSXAppBundleConfig#icon
     * @see OSXAppBundleConfig#developerId
     * @see OSXAppBundleConfig#publisherId
     */
    private OSXAppBundleConfig osxBundleConfiguration;

    /**
     * Target executable name. If not set, the main class name is used.
     */
    private String outputName;

    /**
     * If set to {@code true}, the multi-app mode is enabled for the resulting executable
     * (it mimicks the command line syntax of the conventional {@code java} launcher).
     */
    private boolean multiApp;

    /**
     * Enable/disable startup accelerator.
     * If enabled, the compiled application will run after build
     * for {@link #profileStartupTimeout} seconds for collecting a startup profile.
     */
    private boolean profileStartup;

    /**
     * The duration of the after-build profiling session in seconds. Upon exhaustion,
     * the application will be automatically terminated.
     */
    private int profileStartupTimeout;

    /**
     * If set to {@code true}, enables protection of application data - reflection information,
     * string literals, and resource files packed into the executable, if any.
     *
     * @see #cryptSeed
     */
    private boolean protectData;

    /**
     * Sets a seed string that will be used by the Excelsior JET compiler to generate a key for
     * scrambling the data that the executable contains.
     * If data protection is enabled, but {@code cryptSeed} is not set explicitly, a random value is used.
     * <p>
     * You may want to set a {@code cryptSeed} value if you need the data to be protected in a stable way.
     * </p>
     *
     * @see #protectData
     */
    private String cryptSeed;

    /**
     * (Windows) .ico file to associate with the resulting executable file.
     *
     * By default "icon.ico" of {@link #jetResourcesDir} folder is used.
     */
    private File icon;

    /**
     * (Windows) If set to {@code true}, the resulting executable file will not have a console upon startup.
     */
    private boolean hideConsole;

    /**
     * Add optional JET Runtime components to the package. Available optional components:
     * {@code runtime_utilities}, {@code fonts}, {@code awt_natives}, {@code api_classes}, {@code jce},
     * {@code accessibility}, {@code javafx}, {@code javafx-webkit}, {@code nashorn}, {@code cldr}
     */
    private String[] optRtFiles;

    /**
     * Sets a build tool specific logger and build tool specific messages overriding common ones
     * that should be shown to a user.
     */
    public static void configureEnvironment(Log log, ResourceBundle messages) {
        Log.logger = log;
        Txt.log = log;
        Txt.setAdditionalMessages(messages);
    }

    /**
     * Constructor with required parameters of the project.
     * Usually they can be derived from the enclosing project(pom.xml, build.gradle).
     *
     * @param projectName project name
     * @param groupId project groupId
     * @param version project version
     * @param appType application type
     * @param targetDir target directory of the enclosing project
     * @param jetResourcesDir directory with jet specific resources
     */
    public JetProject(String projectName, String groupId, String version, ApplicationType appType, File targetDir, File jetResourcesDir) {
        this.projectName = requireNonNull(projectName, "projectName cannot be null");
        this.groupId = requireNonNull(groupId, "groupId cannot be null");
        this.version = requireNonNull(version, "version cannot be null");
        this.appType = requireNonNull(appType, "appType cannot be null");
        this.targetDir = requireNonNull(targetDir, "targetDir cannot be null");
        this.jetResourcesDir = requireNonNull(jetResourcesDir, "jetResourcesDir cannot be null");
    }

    ///////////////// Validation ////////////////

    /**
     * Validates project parameters and sets the default values derived from other parameters.
     */
    public JetHome validate() throws JetTaskFailureException {
        if ((Log.logger == null) || (Txt.log == null)) {
            throw new IllegalStateException("Please call JetProject.configureEnvironment() before using JetProject");
        }

        // check jet home
        JetHome jetHomeObj;
        try {
            jetHomeObj = Utils.isEmpty(jetHome) ? new JetHome() : new JetHome(jetHome);

        } catch (JetHomeException e) {
            throw new JetTaskFailureException(e.getMessage());
        }

        if (artifactName == null) {
            artifactName = projectName;
        }

        switch (appType) {
            case PLAIN:
                if (mainJar == null) {
                    mainJar = new File(targetDir, artifactName + ".jar");
                }

                if (!mainJar.exists()) {
                    throw new JetTaskFailureException(s("JetApi.MainJarNotFound.Failure", mainJar.getAbsolutePath()));
                }
                // check main class
                if (Utils.isEmpty(mainClass)) {
                    throw new JetTaskFailureException(s("JetApi.MainNotSpecified.Failure"));
                }

                break;
            case TOMCAT:
                JetEdition edition;
                try {
                    edition = jetHomeObj.getEdition();
                } catch (JetHomeException e) {
                    throw new JetTaskFailureException(e.getMessage());
                }
                if ((edition != JetEdition.EVALUATION) && (edition != JetEdition.ENTERPRISE)) {
                    throw new JetTaskFailureException(s("JetApi.TomcatNotSupported.Failure"));
                }

                if (mainWar == null) {
                    mainWar = new File(targetDir, artifactName + ".war");
                }

                if (!mainWar.exists()) {
                    throw new JetTaskFailureException(s("JetApi.MainWarNotFound.Failure", mainWar.getAbsolutePath()));
                }

                tomcatConfiguration.fillDefaults();

                break;
            default:
                throw new JetTaskFailureException(s("JetApi.BadPackaging.Failure", appType));
        }

        switch (appType) {
            case PLAIN:
                //normalize main and set outputName
                mainClass = mainClass.replace('.', '/');
                break;
            case TOMCAT:
                mainClass = "org/apache/catalina/startup/Bootstrap";
                break;
            default:
                throw new AssertionError("Unknown application type");
        }

        if (jetOutputDir == null) {
            jetOutputDir = new File(targetDir, JET_OUTPUT_DIR);
        }

        if (jetBuildDir == null) {
            jetBuildDir = new File(jetOutputDir, BUILD_DIR);
        }

        if (packageFilesDir == null) {
            packageFilesDir = new File(jetResourcesDir, PACKAGE_FILES_DIR);
        }

        if (execProfilesDir == null) {
            execProfilesDir = jetResourcesDir;
        }

        if (Utils.isEmpty(execProfilesName)) {
            execProfilesName = projectName;
        }

        if (icon == null) {
            icon = new File(jetResourcesDir, "icon.ico");
        }

        switch (appType) {
            case PLAIN:
                if (outputName == null) {
                    int lastSlash = mainClass.lastIndexOf('/');
                    outputName = lastSlash < 0 ? mainClass : mainClass.substring(lastSlash + 1);
                }
                break;
            case TOMCAT:
                if (outputName == null) {
                    outputName = projectName;
                }
                break;
            default:
                throw new AssertionError("Unknown application type");
        }

        if (excelsiorJetPackaging == null) {
            excelsiorJetPackaging = ZIP;
        }

        //check packaging type
        switch (excelsiorJetPackaging) {
            case ZIP:
            case NONE:
                break;
            case EXCELSIOR_INSTALLER:
                if (Utils.isOSX()) {
                    logger.warn(s("JetApi.NoExcelsiorInstallerOnOSX.Warning"));
                    excelsiorJetPackaging = ZIP;
                }
                break;
            case OSX_APP_BUNDLE:
                if (!Utils.isOSX()) {
                    logger.warn(s("JetApi.OSXBundleOnNotOSX.Warning"));
                    excelsiorJetPackaging = ZIP;
                }
                break;

            case NATIVE_BUNDLE:
                if (Utils.isOSX()) {
                    excelsiorJetPackaging = OSX_APP_BUNDLE;
                } else {
                    excelsiorJetPackaging = EXCELSIOR_INSTALLER;
                }
                break;

            default:
                throw new JetTaskFailureException(s("JetApi.UnknownPackagingMode.Failure", excelsiorJetPackaging));
        }

        // check version info
        try {
            checkVersionInfo(jetHomeObj);

            if (multiApp && (jetHomeObj.getEdition() == JetEdition.STANDARD)) {
                logger.warn(s("JetApi.NoMultiappInStandard.Warning"));
                multiApp = false;
            }

            if (profileStartup) {
                if (jetHomeObj.getEdition() == JetEdition.STANDARD) {
                    logger.warn(s("JetApi.NoStartupAcceleratorInStandard.Warning"));
                    profileStartup = false;
                } else if (Utils.isOSX()) {
                    logger.warn(s("JetApi.NoStartupAcceleratorOnOSX.Warning"));
                    profileStartup = false;
                }
            }

            if (protectData) {
                if (jetHomeObj.getEdition() == JetEdition.STANDARD) {
                    throw new JetTaskFailureException(s("JetApi.NoDataProtectionInStandard.Failure"));
                } else {
                    if (cryptSeed == null) {
                        cryptSeed = Utils.randomAlphanumeric(64);
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
            addWindowsVersionInfo = false;
        }
        if (addWindowsVersionInfo && (jetHome.getEdition() == JetEdition.STANDARD)) {
            logger.warn(s("JetApi.NoVersionInfoInStandard.Warning"));
            addWindowsVersionInfo = false;
        }
        if (addWindowsVersionInfo || EXCELSIOR_INSTALLER.equals(excelsiorJetPackaging) || OSX_APP_BUNDLE.equals(excelsiorJetPackaging)) {
            if (Utils.isEmpty(vendor)) {
                //no organization name. Get it from groupId that cannot be empty.
                String[] groupId = groupId().split("\\.");
                if (groupId.length >= 2) {
                    vendor = groupId[1];
                } else {
                    vendor = groupId[0];
                }
                vendor = Character.toUpperCase(vendor.charAt(0)) + vendor.substring(1);
            }
            if (Utils.isEmpty(product)) {
                // no project name, get it from artifactId.
                product = projectName;
            }
        }
        if (addWindowsVersionInfo) {
            if (winVIVersion == null) {
                winVIVersion = version;
            }

            //Coerce winVIVersion to v1.v2.v3.v4 format.
            String finalVersion = deriveFourDigitVersion(winVIVersion);
            if (!winVIVersion.equals(finalVersion)) {
                logger.warn(s("JetApi.NotCompatibleExeVersion.Warning", winVIVersion, finalVersion));
                winVIVersion = finalVersion;
            }

            if (winVICopyright == null) {
                String inceptionYear = this.inceptionYear;
                String curYear = new SimpleDateFormat("yyyy").format(new Date());
                String years = Utils.isEmpty(inceptionYear) ? curYear : inceptionYear + "," + curYear;
                winVICopyright = "Copyright \\x00a9 " + years + " " + vendor;
            }
            if (winVIDescription == null) {
                winVIDescription = product;
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
        if (globalOptimizer) {
            if (jetHome.is64bit()) {
                logger.warn(s("JetApi.NoGlobalIn64Bit.Warning"));
                globalOptimizer = false;
            } else if (jetHome.getEdition() == JetEdition.STANDARD) {
                logger.warn(s("JetApi.NoGlobalInStandard.Warning"));
                globalOptimizer = false;
            }
        }

        if ((javaRuntimeSlimDown != null) && !javaRuntimeSlimDown.isEnabled()) {
            javaRuntimeSlimDown = null;
        }

        if (javaRuntimeSlimDown != null) {
            if (jetHome.is64bit()) {
                logger.warn(s("JetApi.NoSlimDownIn64Bit.Warning"));
                javaRuntimeSlimDown = null;
            } else if (jetHome.getEdition() == JetEdition.STANDARD) {
                logger.warn(s("JetApi.NoSlimDownInStandard.Warning"));
                javaRuntimeSlimDown = null;
            } else {
                if (javaRuntimeSlimDown.detachedBaseURL == null) {
                    throw new JetTaskFailureException(s("JetApi.DetachedBaseURLMandatory.Failure"));
                }

                if (javaRuntimeSlimDown.detachedPackage == null) {
                    javaRuntimeSlimDown.detachedPackage = artifactName + ".pkl";
                }

                globalOptimizer = true;
            }

        }

        if (globalOptimizer) {
            TestRunExecProfiles execProfiles = new TestRunExecProfiles(execProfilesDir, execProfilesName);
            if (!execProfiles.getUsg().exists()) {
                throw new JetTaskFailureException(s("JetApi.NoTestRun.Failure"));
            }
        }
    }

    private void checkTrialVersionConfig(JetHome jetHome) throws JetTaskFailureException, JetHomeException {
        if ((trialVersion != null) && trialVersion.isEnabled()) {
            if ((trialVersion.expireInDays >= 0) && (trialVersion.expireDate != null)) {
                throw new JetTaskFailureException(s("JetApi.AmbiguousExpireSetting.Failure"));
            }
            if (trialVersion.expireMessage == null || trialVersion.expireMessage.isEmpty()) {
                throw new JetTaskFailureException(s("JetApi.NoExpireMessage.Failure"));
            }

            if (jetHome.getEdition() == JetEdition.STANDARD) {
                logger.warn(s("JetApi.NoTrialsInStandard.Warning"));
                trialVersion = null;
            }
        } else {
            trialVersion = null;
        }
    }

    private void checkExcelsiorInstallerConfig() throws JetTaskFailureException {
        if (excelsiorJetPackaging.equals(EXCELSIOR_INSTALLER)) {
            excelsiorInstallerConfiguration.fillDefaults(this);
        }
    }

    private void checkOSXBundleConfig() {
        if (excelsiorJetPackaging.equals(OSX_APP_BUNDLE)) {
            String fourDigitVersion = deriveFourDigitVersion(version);
            osxBundleConfiguration.fillDefaults(this, outputName, product,
                    deriveFourDigitVersion(version),
                    deriveFourDigitVersion(fourDigitVersion.substring(0, fourDigitVersion.lastIndexOf('.'))));
            if (!osxBundleConfiguration.icon.exists()) {
                logger.warn(s("JetApi.NoIconForOSXAppBundle.Warning"));
            }
        }

    }

    private void copyDependency(File from, File to, List<ClasspathEntry> dependencies, boolean isLib) throws JetTaskIOException {
        try {
            Utils.copyFile(from.toPath(), to.toPath());
            dependencies.add(new ClasspathEntry(jetBuildDir.toPath().relativize(to.toPath()).toFile(), isLib));
        } catch (IOException e) {
            // this method is called from lambda so wrap IOException into RuntimeException for conveniences
            throw new JetTaskIOException(e);
        }
    }

    /**
     * Copies project dependencies.
     *
     * @return list of dependencies relative to buildDir
     */
    List<ClasspathEntry> copyDependencies() throws JetTaskFailureException, IOException {
        File libDir = new File(jetBuildDir, LIB_DIR);
        Utils.mkdir(libDir);
        ArrayList<ClasspathEntry> classpathEntries = new ArrayList<>();
        try {
            copyDependency(mainJar, new File(jetBuildDir, mainJar.getName()), classpathEntries, false);
            dependencies.stream()
                    .filter(a -> a.getFile().isFile())
                    .forEach(a ->
                            copyDependency(a.getFile(), new File(libDir, a.getFile().getName()), classpathEntries, a.isLib())
                    )
            ;
            return classpathEntries;
        } catch (JetTaskIOException e) {
            // catch and unwrap io exception thrown by copyDependency in forEach lambda
            throw new IOException(s("JetApi.ErrorCopyingDependency.Exception"), e.getCause());
        }
    }

    /**
     * Copies the master Tomcat server to the build directory and main project artifact (.war)
     * to the "webapps" folder of copied Tomcat.
     *
     * @throws IOException
     */
    void copyTomcatAndWar() throws IOException {
        try {
            Utils.copyDirectory(Paths.get(tomcatConfiguration.tomcatHome), tomcatInBuildDir().toPath());
            String warName = (Utils.isEmpty(tomcatConfiguration.warDeployName)) ? mainWar.getName() : tomcatConfiguration.warDeployName;
            Utils.copyFile(mainWar.toPath(), new File(tomcatInBuildDir(), TomcatConfig.WEBAPPS_DIR + File.separator + warName).toPath());
        } catch (IOException e) {
            throw new IOException(s("JetMojo.ErrorCopyingTomcat.Exception"), e);
        }
    }

    File createBuildDir() throws JetTaskFailureException {
        File buildDir = this.jetBuildDir;
        Utils.mkdir(buildDir);
        return buildDir;
    }

    File tomcatInBuildDir() {
        return new File(jetBuildDir, new File(tomcatConfiguration.tomcatHome).getName());
    }

    ////////// Getters //////////////

    public String groupId() {
        return groupId;
    }

    public String artifactName() {
        return artifactName;
    }

    public File jetResourcesDir() {
        return jetResourcesDir;
    }

    public String vendor() {
        return vendor;
    }

    public String product() {
        return product;
    }

    String mainClass() {
        return mainClass;
    }

    TomcatConfig tomcatConfiguration() {
        return tomcatConfiguration;
    }

    List<ClasspathEntry> getDependencies() {
        return dependencies;
    }

    File packageFilesDir() {
        return packageFilesDir;
    }

    File execProfilesDir() {
        return execProfilesDir;
    }

    String execProfilesName() {
        return execProfilesName;
    }

    String[] jvmArgs() {
        return jvmArgs;
    }

    boolean isAddWindowsVersionInfo() {
        return addWindowsVersionInfo;
    }

    String excelsiorJetPackaging() {
        return excelsiorJetPackaging;
    }

    String winVIVersion() {
        return winVIVersion;
    }

    String winVICopyright() {
        return winVICopyright;
    }

    String winVIDescription() {
        return winVIDescription;
    }

    boolean globalOptimizer() {
        return globalOptimizer;
    }

    SlimDownConfig javaRuntimeSlimDown() {
        return javaRuntimeSlimDown;
    }

    TrialVersionConfig trialVersion() {
        return trialVersion;
    }


    ExcelsiorInstallerConfig excelsiorInstallerConfiguration() {
        return excelsiorInstallerConfiguration;
    }

    String version() {
        return version;
    }

    OSXAppBundleConfig osxBundleConfiguration() {
        return osxBundleConfiguration;
    }

    String outputName() {
        return outputName;
    }

    boolean multiApp() {
        return multiApp;
    }

    boolean profileStartup() {
        return profileStartup;
    }

    boolean protectData() {
        return protectData;
    }

    String cryptSeed() {
        return cryptSeed;
    }

    File icon() {
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

    ApplicationType appType()  {
        return appType;
    }

    ////////// Builder methods ////////////////////

    public JetProject mainWar(File mainWar) {
        this.mainWar = mainWar;
        return this;
    }

    public JetProject jetHome(String jetHome) {
        this.jetHome = jetHome;
        return this;
    }

    public JetProject mainJar(File mainJar) {
        this.mainJar = mainJar;
        return this;
    }

    public JetProject mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public JetProject tomcatConfiguration(TomcatConfig tomcatConfiguration) {
        this.tomcatConfiguration = tomcatConfiguration;
        return this;
    }

    public JetProject dependencies(List<ClasspathEntry> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JetProject artifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    public JetProject packageFilesDir(File packageFilesDir) {
        this.packageFilesDir = packageFilesDir;
        return this;
    }

    public JetProject execProfilesDir(File execProfilesDir) {
        this.execProfilesDir = execProfilesDir;
        return this;
    }

    public JetProject execProfilesName(String execProfilesName) {
        this.execProfilesName = execProfilesName;
        return this;
    }

    public JetProject jvmArgs(String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public JetProject addWindowsVersionInfo(boolean addWindowsVersionInfo) {
        this.addWindowsVersionInfo = addWindowsVersionInfo;
        return this;
    }

    public JetProject excelsiorJetPackaging(String excelsiorJetPackaging) {
        this.excelsiorJetPackaging = excelsiorJetPackaging;
        return this;
    }

    public JetProject vendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public JetProject product(String product) {
        this.product = product;
        return this;
    }

    public JetProject winVIVersion(String winVIVersion) {
        this.winVIVersion = winVIVersion;
        return this;
    }

    public JetProject winVICopyright(String winVICopyright) {
        this.winVICopyright = winVICopyright;
        return this;
    }

    public JetProject inceptionYear(String inceptionYear) {
        this.inceptionYear = inceptionYear;
        return this;
    }

    public JetProject winVIDescription(String winVIDescription) {
        this.winVIDescription = winVIDescription;
        return this;
    }

    public JetProject globalOptimizer(boolean globalOptimizer) {
        this.globalOptimizer = globalOptimizer;
        return this;
    }

    public JetProject javaRuntimeSlimDown(SlimDownConfig javaRuntimeSlimDown) {
        this.javaRuntimeSlimDown = javaRuntimeSlimDown;
        return this;
    }

    public JetProject trialVersion(TrialVersionConfig trialVersion) {
        this.trialVersion = trialVersion;
        return this;
    }

    public JetProject excelsiorInstallerConfiguration(ExcelsiorInstallerConfig excelsiorInstallerConfiguration) {
        this.excelsiorInstallerConfiguration = excelsiorInstallerConfiguration;
        return this;
    }

    public JetProject version(String version) {
        this.version = version;
        return this;
    }

    public JetProject osxBundleConfiguration(OSXAppBundleConfig osxBundleConfiguration) {
        this.osxBundleConfiguration = osxBundleConfiguration;
        return this;
    }

    public JetProject outputName(String outputName) {
        this.outputName = outputName;
        return this;
    }

    public JetProject multiApp(boolean multiApp) {
        this.multiApp = multiApp;
        return this;
    }

    public JetProject profileStartup(boolean profileStartup) {
        this.profileStartup = profileStartup;
        return this;
    }

    public JetProject protectData(boolean protectData) {
        this.protectData = protectData;
        return this;
    }

    public JetProject cryptSeed(String cryptSeed) {
        this.cryptSeed = cryptSeed;
        return this;
    }

    public JetProject icon(File icon) {
        this.icon = icon;
        return this;
    }

    public JetProject hideConsole(boolean hideConsole) {
        this.hideConsole = hideConsole;
        return this;
    }

    public JetProject profileStartupTimeout(int profileStartupTimeout) {
        this.profileStartupTimeout = profileStartupTimeout;
        return this;
    }

    public JetProject optRtFiles(String[] optRtFiles) {
        this.optRtFiles = optRtFiles;
        return this;
    }

    public JetProject jetOutputDir(File jetOutputDir) {
        this.jetOutputDir = jetOutputDir;
        return this;
    }
}
