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
import com.excelsiorjet.api.JetEdition;
import com.excelsiorjet.api.JetHomeException;
import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.log.Log;
import com.excelsiorjet.api.tasks.config.*;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.tasks.PackagingType.*;
import static com.excelsiorjet.api.util.Txt.s;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * <p>Collection of Excelsior JET compiler and packager parameters that can be configured by an external build tool,
 * such as Maven or Gradle. More precisely, it is assumed that a plugin into that build tool configures these parameters,
 * so there is a section specific to that plugin in a bigger project, such as {@code pom.xml} or {@code build.gradle},
 * mentioned below as the <em>enclosing</em> project.</p>
 *
 * <p>An instance of this class can be constructed with the build pattern methods and used by Excelsior JET tasks
 * such as {@link JetBuildTask}, {@link TestRunTask}.</p>
 *
 * <p>The class performs validation of the parameters via the {@link #validate(ExcelsiorJet, boolean)} method.
 * During validation, it sets default parameter values derived from other parameters,
 * so it is not necessary to set all the parameters.
 * That said, some parameters are required. For instance, {@link #mainClass} is required for plain Java SE applications.</p>
 *
 * @see JetBuildTask
 * @see TestRunTask
 */
public class JetProject {

    private static final String JET_OUTPUT_DIR = "jet";
    private static final String BUILD_DIR = "build";
    private static final String PACKAGE_FILES_DIR = "packagefiles";

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
     * Application type. Currently, Plain Java SE Applications, Invocation Dynamic Libraries, Windows Services
     * and Tomcat Web Applications are supported.
     *
     * @see ApplicationType#PLAIN
     * @see ApplicationType#DYNAMIC_LIBRARY
     * @see ApplicationType#WINDOWS_SERVICE
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
     * List of managed (i.e. Maven or Gradle) dependencies specified by the user of an API client.
     */
    private List<ProjectDependency> projectDependencies;

    /**
     * List of {@code projectDependencies} settings and not managed dependencies specified by the user of an API client.
     */
    private List<DependencySettings> dependencies;

    /**
     * Internal representation of project dependencies calculated from {@code projectDependencies} and {@code dependencies}
     */
    private List<ClasspathEntry> classpathEntries;

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
     * (32-bit only)
     * Reduce the disk footprint of the application by including the supposedly unused Java SE API
     * classes in the resulting package in a compressed form.
     * Valid values are: {@code none},  {@code medium} (default),  {@code high-memory},  {@code high-disk}.
     * <p>
     * The feature is only available if {@link #globalOptimizer} is enabled.
     * In this mode, the Java SE classes that were not compiled into the resulting executable are placed
     * into the resulting package in bytecode form, possibly compressed depending on the mode:
     * </p>
     * <dl>
     * <dt>none</dt>
     * <dd>Disable compression</dd>
     * <dt>medium</dt>
     * <dd>Use a simple compression algorithm that has minimal run time overheads and permits
     * selective decompression.</dd>
     * <dt>high-memory</dt>
     * <dd>Compress all unused Java SE API classes as a whole. This results in more significant disk
     * footprint reduction compared to than medium compression. However, if one of the compressed classes
     * is needed at run time, the entire bundle must be decompressed to retrieve it.
     * In the {@code high-memory} reduction mode the bundle is decompressed 
     * onto the heap and can be garbage collected later.</dd>
     * <dt>high-disk</dt>
     * <dd>Same as {@code high-memory}, but decompress to the temp directory.</dd>
     * </dl>
     */
    private String diskFootprintReduction;

    /**
     * (32-bit only) Java Runtime Slim-Down configuration parameters.
     *
     * @see SlimDownConfig#detachedBaseURL
     * @see SlimDownConfig#detachComponents
     * @see SlimDownConfig#detachedPackage
     */
    private SlimDownConfig javaRuntimeSlimDown;

    /**
     * Java SE 8 defines three subsets of the standard Platform API called compact profiles.
     * Excelsior JET enables you to deploy your application with one of those subsets.
     * You may set this parameter to specify a particular profile.
     * Valid values are: {@code auto} (default),  {@code compact1},  {@code compact2},  {@code compact3}, {@code full}
     *  <p>
     * {@code auto} value (default) forces Excelsior JET to detect which parts of the Java SE Platform API
     * are referenced by the application and select the smallest compact profile that includes them all,
     * or the entire Platform API if there is no such profile.
     * </p>
     */
    private String profile;

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
     * Windows Service configuration parameters.
     *
     * @see WindowsServiceConfig#name
     * @see WindowsServiceConfig#displayName
     * @see WindowsServiceConfig#description
     * @see WindowsServiceConfig#arguments
     * @see WindowsServiceConfig#logOnType
     * @see WindowsServiceConfig#allowDesktopInteraction
     * @see WindowsServiceConfig#startupType
     * @see WindowsServiceConfig#startServiceAfterInstall
     * @see WindowsServiceConfig#dependencies
     */
    private WindowsServiceConfig windowsServiceConfiguration;

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
     * Splash image to display upon application start up.
     *
     * By default, the file "splash.png" from the {@link #jetResourcesDir} folder is used.
     * If it does not exist, but a splash image is specified in the manifest
     * of the application JAR file, that image will be used automatically.
     */
    private File splash;

    /**
     * The JET Runtime supports three modes of stack trace printing: {@code minimal}, {@code full}, and {@code none}.
     * <p>
     * In the {@code minimal} mode (default), line numbers and names of some methods are omitted in call stack entries,
     * but the class names are exact.
     * </p>
     * <p>
     * In the {@code full} mode, the stack trace info includes all line numbers and method names.
     * However, enabling the full stack trace has a side effect - substantial growth of the resulting
     * executable size, approximately by 30%.
     * </p>
     * <p>
     * In the {@code none} mode, Throwable.printStackTrace() methods print a few fake elements.
     * It may result in performance improvement if the application throws and catches exceptions repeatedly.
     * Note, however, that some third-party APIs may rely on stack trace printing. One example
     * is the Log4J API that provides logging services.
     * </p>
     */
    private String stackTraceSupport;

    /**
     * Controls the aggressiveness of method inlining.
     * Available values are:
     *   {@code aggressive} (default), {@code very-aggressive}, {@code medium}, {@code low}, {@code tiny-methods-only}.
     * <p>
     * If you need to reduce the size of the executable,
     * set the {@code low} or {@code tiny-methods-only} option. Note that it does not necessarily worsen application performance.
     * </p>
     */
    private String inlineExpansion;

    /**
     * (Windows) If set to {@code true}, the resulting executable file will not have a console upon startup.
     */
    private boolean hideConsole;

    /**
     * Add optional JET Runtime components to the package.
     * By default, only the {@code jce} component (Java Crypto Extension) is added.
     * You may pass a special value {@code all} to include all available optional components at once
     * or {@code none} to not include any of them.
     * Available optional components:
     * {@code runtime_utilities}, {@code fonts}, {@code awt_natives}, {@code api_classes}, {@code jce},
     * {@code accessibility}, {@code javafx}, {@code javafx-webkit}, {@code nashorn}, {@code cldr}
     */
    private String[] optRtFiles;

    /**
     * Add locales and charsets.
     * By default only {@code European} locales are added.
     * You may pass a special value {@code all} to include all available locales at once
     * or {@code none} to not include any additional locales.
     * Available locales and charsets:
     *    {@code European}, {@code Indonesian}, {@code Malay}, {@code Hebrew}, {@code Arabic},
     *    {@code Chinese}, {@code Japanese}, {@code Korean}, {@code Thai}, {@code Vietnamese}, {@code Hindi},
     *    {@code Extended_Chinese}, {@code Extended_Japanese}, {@code Extended_Korean}, {@code Extended_Thai},
     *    {@code Extended_IBM}, {@code Extended_Macintosh}, {@code Latin_3}
     */
    private String[] locales;
    /**
     * Additional compiler options and equations.
     * The commonly used compiler options and equations are mapped to the respective project parameters,
     * so usually there is no need to specify them with this parameter.
     * However, the compiler also has some advanced options and equations
     * that you may find in the Excelsior JET User's Guide, plus some troubleshooting settings
     * that the Excelsior JET Support team may suggest to you.
     * You may enumerate such options and equations with this parameter and they will be appended to the
     * Excelsior JET project generated by {@link JetBuildTask}.
     * <p>
     * Care must be taken when using this parameter to avoid conflicts with other project parameters.
     * </p>
     */
    private String[] compilerOptions;
    /**
     * Command line arguments that will be passed to the application during startup accelerator profiling and the test run.
     * You may also set the parameter via the {@code jet.runArgs} system property, where arguments
     * are comma separated (use "\" to escape commas inside arguments,
     * i.e. {@code -Djet.runArgs="arg1,Hello\, World"} will be passed to your application as {@code arg1 "Hello, World"})
     */
    private String[] runArgs;

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
     * @param validateForBuild if set to {@code false} the method does not validate parameters that are used only for project build
     */
    public void validate(ExcelsiorJet excelsiorJet, boolean validateForBuild) throws JetTaskFailureException {
        if ((Log.logger == null) || (Txt.log == null)) {
            throw new IllegalStateException("Please call JetProject.configureEnvironment() before using JetProject");
        }


        if (artifactName == null) {
            artifactName = projectName;
        }

        switch (appType) {
            case WINDOWS_SERVICE:
                if (!excelsiorJet.isWindowsServicesSupported()) {
                    throw new JetTaskFailureException(s("JetApi.WinServiceNotSupported.Failure", appType));
                }
                //fall through

            case PLAIN:
            case DYNAMIC_LIBRARY:
                if (mainJar == null) {
                    mainJar = new File(targetDir, artifactName + ".jar");
                }

                if (!mainJar.exists()) {
                    throw new JetTaskFailureException(s("JetApi.MainJarNotFound.Failure", mainJar.getAbsolutePath()));
                }

                break;
            case TOMCAT:
                if (!excelsiorJet.isTomcatSupported()) {
                    throw new JetTaskFailureException(s("JetApi.TomcatNotSupported.Failure"));
                }

                if (mainWar == null) {
                    mainWar = new File(targetDir, artifactName + ".war");
                }

                if (!mainWar.exists()) {
                    throw new JetTaskFailureException(s("JetApi.MainWarNotFound.Failure", mainWar.getAbsolutePath()));
                }

                if (!mainWar.getName().endsWith(TomcatConfig.WAR_EXT)) {
                    throw new JetTaskFailureException(s("JetApi.MainWarShouldEndWithWar.Failure", mainWar.getAbsolutePath()));
                }

                tomcatConfiguration.fillDefaults(mainWar.getName());

                break;
            default:
                throw new AssertionError("Unknown application type");
        }

        // check main class
        switch (appType) {
            case PLAIN:
            case WINDOWS_SERVICE:
                if (Utils.isEmpty(mainClass)) {
                    throw new JetTaskFailureException(s("JetApi.MainNotSpecified.Failure"));
                } else {
                    //normalize main
                    mainClass = mainClass.replace('.', '/');
                }
                break;
            case DYNAMIC_LIBRARY:
                //no need to check main here
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

        if (excelsiorJetPackaging == null) {
            excelsiorJetPackaging = ZIP.toString();
        }

        //check packaging type
        switch (excelsiorJetPackaging()) {
            case ZIP:
            case NONE:
                break;
            case EXCELSIOR_INSTALLER:
                if (!excelsiorJet.isExcelsiorInstallerSupported()) {
                    logger.warn(s("JetApi.NoExcelsiorInstaller.Warning"));
                    excelsiorJetPackaging = ZIP.toString();
                }
                break;
            case OSX_APP_BUNDLE:
                if (!excelsiorJet.getTargetOS().isOSX()) {
                    logger.warn(s("JetApi.OSXBundleOnNotOSX.Warning"));
                    excelsiorJetPackaging = ZIP.toString();
                }
                break;

            case NATIVE_BUNDLE:
                if (excelsiorJet.getTargetOS().isOSX()) {
                    excelsiorJetPackaging = OSX_APP_BUNDLE.toString();
                } else if (excelsiorJet.isExcelsiorInstallerSupported()){
                    excelsiorJetPackaging = EXCELSIOR_INSTALLER.toString();
                } else {
                    excelsiorJetPackaging = ZIP.toString();
                }
                break;

            default:
                throw new JetTaskFailureException(s("JetApi.UnknownPackagingMode.Failure", excelsiorJetPackaging));
        }

        if ((appType == ApplicationType.WINDOWS_SERVICE) && (excelsiorJetPackaging() == EXCELSIOR_INSTALLER) &&
                !excelsiorJet.isWindowsServicesInExcelsiorInstallerSupported()) {
            throw new JetTaskFailureException(s("JetApi.WinServiceInEINotSupported.Failure"));
        }

        if (execProfilesDir == null) {
            execProfilesDir = jetResourcesDir;
        }

        // Override run args from system property
        String runArgs = System.getProperty("jet.runArgs");
        if (runArgs != null) {
            this.runArgs = Utils.parseRunArgs(runArgs);
        }

        if (Utils.isEmpty(execProfilesName)) {
            execProfilesName = projectName;
        }
        if (validateForBuild) {
            validateForBuild(excelsiorJet);
        }

        processDependencies();
    }

    void processDependencies() throws JetTaskFailureException {
        for (DependencySettings dependency : dependencies) {
            if (dependency.path == null && dependency.groupId == null && dependency.artifactId == null) {
                throw new JetTaskFailureException(s("JetApi.DependencyIdRequired"));
            } else if (dependency.path != null && (dependency.groupId != null || dependency.artifactId != null || dependency.version != null)) {
                throw new JetTaskFailureException(s("JetApi.InvalidDependencySetting", dependency.idStr()));
            } else if (dependency.path != null && dependency.path.isDirectory()) {
                if (dependency.pack != null && !ClasspathEntry.PackType.NONE.userValue.equals(dependency.pack)) {
                    throw new JetTaskFailureException(s("JetApi.NotPackedDirectory", dependency.path));
                }
            }

            if (dependency.packagePath != null && dependency.disableCopyToPackage != null && dependency.disableCopyToPackage) {
                throw new JetTaskFailureException(s("JetApi.DependencySettingsCannotHavePackagePathAndDisabledCopyToPackage", dependency.idStr()));
            }
        }

        // allProjectDependencies = project dependencies + main artifact
        List<ProjectDependency> allProjectDependencies = new ArrayList<>(projectDependencies);
        ProjectDependency mainArtifactDep = new ProjectDependency(groupId, projectName, version, appType != ApplicationType.TOMCAT ? mainJar : mainWar, true);
        // in original implementation main artifact is preceded other dependencies
        allProjectDependencies.add(0, mainArtifactDep);

        List<DependencySettings> dependenciesSettings = new ArrayList<>();
        List<DependencySettings> externalDependencies = new ArrayList<>();

        Set<String> ids = new HashSet<>();
        for (DependencySettings dependencySettings : dependencies) {
            List<ProjectDependency> matchedDependencies = dependenciesMatchedBy(allProjectDependencies, dependencySettings);
            if (matchedDependencies.size() == 0){
                if (dependencySettings.hasPathOnly()) {
                    externalDependencies.add(dependencySettings);
                } else {
                    throw new JetTaskFailureException(s("JetApi.NoDependenciesForDependencySettings", dependencySettings.idStr()));
                }
            } else {
                dependenciesSettings.add(dependencySettings);
            }

            if (dependencySettings.isArtifactOnly() && matchedDependencies.size() > 1) {
                List<String> dependencyIds = matchedDependencies.stream().
                        map(d -> d.idStr(false)).
                        collect(toList());
                throw new JetTaskFailureException(s("JetApi.AmbiguousArtifactIdOnlyDependencySettings", dependencySettings.idStr(), String.join(", ", dependencyIds)));
            }

            if (ids.contains(dependencySettings.idStr())) {
                throw new JetTaskFailureException(s("JetApi.DuplicateDependencySettingsId", dependencySettings.idStr()));
            }
            ids.add(dependencySettings.idStr());
        }

        for (DependencySettings externalDependency : externalDependencies) {
            if (appType == ApplicationType.TOMCAT) {
                throw new JetTaskFailureException(s("JetApi.TomcatApplicationCannotHaveExternalDependencies", externalDependency.path));
            }

            if (!externalDependency.path.exists()) {
                throw new JetTaskFailureException(s("JetApi.ExternalDependencyDoesNotExist", externalDependency.path));
            }
        }

        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(groupId, dependenciesSettings);
        classpathEntries = new ArrayList<>();
        switch (appType()) {
            case PLAIN:
            case DYNAMIC_LIBRARY:
            case WINDOWS_SERVICE:
            {
                // Values of the seenDeps HashMap can either be of type ProjectDependevcy or DependencySettings.
                // DependencySettings are put there while processing external dependencies.
                HashMap<String, Object> seenDeps = new HashMap<>();
                for (ProjectDependency prjDep : allProjectDependencies) {
                    ClasspathEntry cpEntry = dependencySettingsResolver.resolve(prjDep);
                    String packagePath = toPathRelativeToJetBuildDir(cpEntry).toString();
                    ProjectDependency oldDep = (ProjectDependency) seenDeps.put(packagePath, prjDep);
                    if (oldDep != null) {
                        throw new JetTaskFailureException(s("JetApi.OverlappedDependency", prjDep, oldDep));
                    } else {
                        classpathEntries.add(cpEntry);
                    }
                }
                for (DependencySettings extDep : externalDependencies) {
                    ClasspathEntry cpEntry = new ClasspathEntry(extDep, false);
                    Object oldDep = seenDeps.put(toPathRelativeToJetBuildDir(cpEntry).toString(), extDep);
                    if (oldDep != null) {
                        throw new JetTaskFailureException(s("JetApi.OverlappedExternalDependency", extDep.path, oldDep));
                    }
                    classpathEntries.add(cpEntry);
                }
            }
                break;
            case TOMCAT:
                HashMap<String, ProjectDependency> seenDeps = new HashMap<>();
                for (ProjectDependency prjDep : allProjectDependencies) {
                    if (!prjDep.isMainArtifact || dependencySettingsResolver.hasSettingsFor(prjDep)) {
                        ClasspathEntry cpEntry = dependencySettingsResolver.resolve(prjDep);

                        ProjectDependency oldDep = null;
                        if (!prjDep.isMainArtifact) {
                            String depName = cpEntry.path.getName();
                            oldDep = seenDeps.put(depName, prjDep);
                        }
                        if (oldDep != null) {
                            throw new JetTaskFailureException(s("JetApi.OverlappedTomcatDependency", prjDep, oldDep));
                        } else {
                            classpathEntries.add(cpEntry);
                        }
                    }
                }
                break;
            default:
                throw new AssertionError("Unknown application type: " + appType());
        }
    }

    private List<ProjectDependency> dependenciesMatchedBy(List<ProjectDependency> projectDependencies, DependencySettings dependencySetting) {
        return projectDependencies.stream().
                filter(dependencySetting::matches).
                collect(toList());
    }

    private void validateForBuild(ExcelsiorJet excelsiorJet) throws JetTaskFailureException {

        if (icon == null) {
            icon = new File(jetResourcesDir, "icon.ico");
        }

        if (splash == null) {
            splash = new File(jetResourcesDir, "splash.png");
        }

        if (outputName == null) {
            outputName = projectName;
        }

        if (stackTraceSupport == null) {
            stackTraceSupport = StackTraceSupportType.MINIMAL.toString();
        } else if (stackTraceSupport() == null) {
            throw new JetTaskFailureException(s("JetApi.UnknownStackTraceSupportValue.Failure", stackTraceSupport));
        }

        if (inlineExpansion == null) {
            inlineExpansion = InlineExpansionType.AGGRESSIVE.toString();
        } else if (inlineExpansion() == null) {
            throw new JetTaskFailureException(s("JetApi.UnknownInlineExpansionValue.Failure", inlineExpansion));
        }

        // check version info
        try {
            checkVersionInfo(excelsiorJet);

            if (multiApp && !excelsiorJet.isMultiAppSupported()) {
                logger.warn(s("JetApi.NoMultiappInStandard.Warning"));
                multiApp = false;
            }

            if (profileStartup) {
                if (!excelsiorJet.isStartupAcceleratorSupported()) {
                    // startup accelerator is enabled by default,
                    // so if it is not supported, no warn about it.
                    profileStartup = false;
                }
            }

            if (protectData) {
                if (!excelsiorJet.isDataProtectionSupported()) {
                    throw new JetTaskFailureException(s("JetApi.NoDataProtectionInStandard.Failure"));
                } else {
                    if (cryptSeed == null) {
                        cryptSeed = Utils.randomAlphanumeric(64);
                    }
                }
            }
            if (excelsiorJet.isCompactProfilesSupported()) {
                if (profile == null) {
                    profile = CompactProfileType.AUTO.toString();
                } else if (compactProfile() == null) {
                    throw new JetTaskFailureException(s("JetApi.UnknownProfileType.Failure", profile));
                }
            } else if (profile != null) {
                switch (compactProfile()) {
                    case COMPACT1: case COMPACT2: case COMPACT3:
                        throw new JetTaskFailureException(s("JetApi.CompactProfilesNotSupported.Failure", profile));
                    case AUTO: case FULL:
                        break;
                    default:  throw new AssertionError("Unknown compact profile: " + compactProfile());
                }
            }

            checkTrialVersionConfig(excelsiorJet);

            checkGlobalAndRelatedParameters(excelsiorJet);

            checkExcelsiorInstallerConfig();

            checkWindowsServiceConfig();

            checkOSXBundleConfig();

        } catch (JetHomeException e) {
            throw new JetTaskFailureException(e.getMessage());
        }
    }

    private void checkVersionInfo(ExcelsiorJet excelsiorJet) throws JetHomeException {
        if (!excelsiorJet.getTargetOS().isWindows()) {
            addWindowsVersionInfo = false;
        }
        if (addWindowsVersionInfo && (excelsiorJet.getEdition() == JetEdition.STANDARD)) {
            logger.warn(s("JetApi.NoVersionInfoInStandard.Warning"));
            addWindowsVersionInfo = false;
        }
        if (addWindowsVersionInfo || excelsiorJetPackaging().isNativeBundle()) {
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

    TestRunExecProfiles testRunExecProfiles() {
        return new TestRunExecProfiles(execProfilesDir, execProfilesName);
    }

    private void checkGlobalAndRelatedParameters(ExcelsiorJet excelsiorJet) throws JetHomeException, JetTaskFailureException {
        if (globalOptimizer) {
            if (!excelsiorJet.isGlobalOptimizerSupported()) {
                logger.warn(s("JetApi.NoGlobal.Warning"));
                globalOptimizer = false;
            }
        }

        if ((javaRuntimeSlimDown != null) && !javaRuntimeSlimDown.isEnabled()) {
            javaRuntimeSlimDown = null;
        }

        if (javaRuntimeSlimDown != null) {
            if (!excelsiorJet.isSlimDownSupported()) {
                logger.warn(s("JetApi.NoSlimDown.Warning"));
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

        if (diskFootprintReduction != null) {
            if (diskFootprintReduction() == null) {
                throw new JetTaskFailureException(s("JetApi.UnknownDiskFootprintReductionType.Failure", profile));
            }
            if (!excelsiorJet.isDiskFootprintReductionSupported()) {
                logger.warn(s("JetApi.NoDiskFootprintReduction.Warning"));
                diskFootprintReduction = null;
            } else if (!globalOptimizer) {
                logger.warn(s("JetApi.DiskFootprintReductionForGlobalOnly.Warning"));
                diskFootprintReduction = null;
            }
        }

        if (globalOptimizer) {
            TestRunExecProfiles execProfiles = testRunExecProfiles();
            if (!execProfiles.getUsg().exists()) {
                throw new JetTaskFailureException(s("JetApi.NoTestRun.Failure"));
            }
        }
    }

    private void checkTrialVersionConfig(ExcelsiorJet excelsiorJet) throws JetTaskFailureException, JetHomeException {
        if ((trialVersion != null) && trialVersion.isEnabled()) {
            if ((trialVersion.expireInDays >= 0) && (trialVersion.expireDate != null)) {
                throw new JetTaskFailureException(s("JetApi.AmbiguousExpireSetting.Failure"));
            }
            if (trialVersion.expireMessage == null || trialVersion.expireMessage.isEmpty()) {
                throw new JetTaskFailureException(s("JetApi.NoExpireMessage.Failure"));
            }

            if (!excelsiorJet.isTrialSupported()) {
                logger.warn(s("JetApi.NoTrialsInStandard.Warning"));
                trialVersion = null;
            }
        } else {
            trialVersion = null;
        }
    }

    private void checkExcelsiorInstallerConfig() throws JetTaskFailureException {
        if (excelsiorJetPackaging() == EXCELSIOR_INSTALLER) {
            excelsiorInstallerConfiguration.fillDefaults(this);
        }
    }

    private void checkWindowsServiceConfig() throws JetTaskFailureException {
        if ((appType() == ApplicationType.WINDOWS_SERVICE) ||
                (appType == ApplicationType.TOMCAT) &&
                        (excelsiorJetPackaging() == EXCELSIOR_INSTALLER) &&
                        tomcatConfiguration.installWindowsService
                )
        {
            windowsServiceConfiguration.fillDefaults(this);
        }
    }

    private void checkOSXBundleConfig() {
        if (excelsiorJetPackaging() == OSX_APP_BUNDLE) {
            String fourDigitVersion = deriveFourDigitVersion(version);
            osxBundleConfiguration.fillDefaults(this, outputName, product,
                    deriveFourDigitVersion(version),
                    deriveFourDigitVersion(fourDigitVersion.substring(0, fourDigitVersion.lastIndexOf('.'))));
            if (!osxBundleConfiguration.icon.exists()) {
                logger.warn(s("JetApi.NoIconForOSXAppBundle.Warning"));
            }
        }

    }

    private void copyClasspathEntry(ClasspathEntry classpathEntry, File to) throws JetTaskWrappedException {
        try {
            Utils.mkdir(to.getParentFile());
            if (classpathEntry.path.isFile()) {
                Utils.copyFile(classpathEntry.path.toPath(), to.toPath());
            } else {
                Utils.copyDirectory(classpathEntry.path.toPath(), to.toPath());
            }
        } catch (IOException | JetTaskFailureException e) {
            // this method is called from lambda so wrap IOException into RuntimeException for conveniences
            throw new JetTaskWrappedException(e);
        }
    }

    /**
     * Copies project dependencies.
     *
     * @return list of dependencies relative to buildDir
     */
    List<ClasspathEntry> copyClasspathEntries() throws JetTaskFailureException, IOException {
        try {
            classpathEntries.forEach(a -> {
                Path pathInJetBuildDir = toPathRelativeToJetBuildDir(a);
                File dst = jetBuildDir.toPath().resolve(pathInJetBuildDir).toFile();
                copyClasspathEntry(a, dst);
            });

            return classpathEntries;
        } catch (JetTaskWrappedException e) {
            // catch and unwrap io exception thrown by copyClasspathEntry in forEach lambda
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
            String warName = tomcatConfiguration.warDeployName;
            Utils.copyFile(mainWar.toPath(), new File(tomcatInBuildDir(), TomcatConfig.WEBAPPS_DIR + File.separator + warName).toPath());
        } catch (IOException e) {
            throw new IOException(s("JetApi.ErrorCopyingTomcat.Exception", tomcatConfiguration.tomcatHome), e);
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

    public List<ClasspathEntry> classpathEntries() {
        return classpathEntries;
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

    PackagingType excelsiorJetPackaging() {
        return PackagingType.fromString(excelsiorJetPackaging);
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

    CompactProfileType compactProfile() {
        return CompactProfileType.fromString(profile);
    }

    DiskFootprintReductionType diskFootprintReduction() {
        return DiskFootprintReductionType.fromString(diskFootprintReduction);
    }


    TrialVersionConfig trialVersion() {
        return trialVersion;
    }

    ExcelsiorInstallerConfig excelsiorInstallerConfiguration() {
        return excelsiorInstallerConfiguration;
    }

    WindowsServiceConfig windowsServiceConfiguration() {
        return windowsServiceConfiguration;
    }

    String version() {
        return version;
    }

    OSXAppBundleConfig osxBundleConfiguration() {
        return osxBundleConfiguration;
    }

    public String outputName() {
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

    File splash() {
        return splash;
    }

    StackTraceSupportType stackTraceSupport() {
        return StackTraceSupportType.fromString(stackTraceSupport);
    }

    InlineExpansionType inlineExpansion() {
        return InlineExpansionType.fromString(inlineExpansion);
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

    public ApplicationType appType()  {
        return appType;
    }

    String[] compilerOptions() {
        return compilerOptions;
    }

    String[] locales() {
        return locales;
    }

    public String[] runArgs() {
        return runArgs;
    }

////////// Builder methods ////////////////////

    public JetProject mainWar(File mainWar) {
        this.mainWar = mainWar;
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

    public JetProject projectDependencies(List<ProjectDependency> projectDependencies) {
        this.projectDependencies = projectDependencies;
        return this;
    }

    public JetProject dependencies(List<DependencySettings> dependencies) {
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

    public JetProject compactProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public JetProject diskFootprintReduction(String diskFootprintReduction) {
        this.diskFootprintReduction = diskFootprintReduction;
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

    public JetProject windowsServiceConfiguration(WindowsServiceConfig windowsServiceConfiguration) {
        this.windowsServiceConfiguration = windowsServiceConfiguration;
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

    public JetProject splash(File splash) {
        this.splash = splash;
        return this;
    }

    public JetProject stackTraceSupport(String stackTraceSupport) {
        this.stackTraceSupport = stackTraceSupport;
        return this;
    }

    public JetProject inlineExpansion(String inlineExpansion) {
        this.inlineExpansion = inlineExpansion;
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

    public JetProject jetBuildDir(File jetBuildDir) {
        this.jetBuildDir = jetBuildDir;
        return this;
    }

    public JetProject compilerOptions(String[] compilerOptions) {
        this.compilerOptions = compilerOptions;
        return this;
    }

    public JetProject locales(String[] locales) {
        this.locales = locales;
        return this;
    }

    public JetProject runArgs(String[] runArgs) {
        this.runArgs = runArgs;
        return this;
    }

    public File jetBuildDir() {
        return jetBuildDir;
    }

    public File mainJar() {
        return mainJar;
    }

    public static ApplicationType checkAndGetAppType(String appType) throws JetTaskFailureException {
        ApplicationType applicationType = ApplicationType.fromString(appType);
        if (applicationType == null) {
            throw new JetTaskFailureException(s("JetApi.UnknownAppType.Failure", appType));
        }
        return applicationType;
    }

    Path toPathRelativeToJetBuildDir(ClasspathEntry classpathEntry) {
        Path libPath;
        Path jetBuildDir = jetBuildDir().toPath();
        Path jetLibDir = jetBuildDir.resolve("lib");
        if (classpathEntry.packagePath == null) {
            if (classpathEntry.path.isFile()) {
                libPath = jetLibDir.resolve(classpathEntry.path.getName());
            } else {
                libPath = jetBuildDir.resolve(classpathEntry.path.getName());
            }
        } else {
            libPath = jetBuildDir.resolve(classpathEntry.packagePath).resolve(classpathEntry.path.getName());
        }
        return jetBuildDir.relativize(libPath);
    }
}
