/*
 * Copyright (c) 2016-2017, Excelsior LLC.
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
import com.excelsiorjet.api.JetHomeException;
import com.excelsiorjet.api.log.Log;
import com.excelsiorjet.api.platform.Host;
import com.excelsiorjet.api.tasks.config.*;
import com.excelsiorjet.api.tasks.config.compiler.*;
import com.excelsiorjet.api.tasks.config.dependencies.DependencySettings;
import com.excelsiorjet.api.tasks.config.dependencies.OptimizationPreset;
import com.excelsiorjet.api.tasks.config.dependencies.ProjectDependency;
import com.excelsiorjet.api.tasks.config.excelsiorinstaller.ExcelsiorInstallerConfig;
import com.excelsiorjet.api.tasks.config.packagefile.PackageFile;
import com.excelsiorjet.api.tasks.config.runtime.RuntimeConfig;
import com.excelsiorjet.api.tasks.config.windowsservice.WindowsServiceConfig;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.tasks.config.PackagingType.*;
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
    private static final String APP_DIR = "app";
    private static final String SPRING_BOOT_JAR_MAIN_CLASS = "org.springframework.boot.loader.JarLauncher";
    private static final String SPRING_BOOT_WAR_MAIN_CLASS = "org.springframework.boot.loader.WarLauncher";
    private static final String SPRING_BOOT_VERSION_ATTR = "Spring-Boot-Version";
    private static final String SPRING_BOOT_START_CLASS_ATTR = "Start-Class";

    /**
     * Name and version of the plugin that created this project.
     */
    private String creatorPlugin;

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
     * use the {@link WindowsVersionInfoConfig#version} parameter.
     */
    private String version;

    /**
     * Application type. Currently, Plain Java SE Applications, Invocation Dynamic Libraries, Windows Services,
     * Tomcat and Spring Boot Applications are supported.
     *
     * @see ApplicationType#PLAIN
     * @see ApplicationType#DYNAMIC_LIBRARY
     * @see ApplicationType#WINDOWS_SERVICE
     * @see ApplicationType#TOMCAT
     * @see ApplicationType#SPRING_BOOT
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
     * The default is the "jet" subdirectory of {@link #targetDir}.
     */
    private File jetOutputDir;

    /**
     * Excelsior JET project build directory.
     *
     * The value is set to "build" subdirectory of {@link #jetOutputDir}.
     */
    private File jetBuildDir;

    /**
     * Target directory where the plugin places the executable, the required Excelsior JET Runtime files and
     * package files you configured with {@link #packageFiles} and {@link #packageFilesDir}.
     *
     * The value is set to "app" subdirectory of {@link #jetOutputDir}.
     */
    private File jetAppDir;

    /**
     * Directory containing additional package files - README, license, media, help files, native libraries, and the like.
     * The contents of the directory will be recursively copied to the final application package.
     *
     * By default, the value is set to "packageFiles" subfolder of {@link #jetResourcesDir}
     *
     * @see #packageFiles
     */
    private File packageFilesDir;

    /**
     * If you only need to add a few additional package files,
     * it may be more convenient to specify them separately rather than prepare a {@link #packageFilesDir} directory.
     */
    private List<PackageFile> packageFiles;

    /**
     * Name of the final artifact of the enclosing project. Used as the default value for {@link #mainJar} and {@link #mainWar},
     * and to derive the default names of final artifacts created by {@link JetBuildTask} such as zip file, installer, and so on.
     */
    private String artifactName;

    /**
     * The main application jar for plain Java SE and Spring Boot applications.
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
     * The main web application archive for Tomcat and Spring Boot applications.
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
     * Execution profiles configuration parameters.
     * You can configure the filesystem location and base name of all application execution profiles,
     * whether they can be collected locally, i.e. on the machine where the build takes place,
     * and the maximum profile age when they are considered outdated.
     *
     * @see ExecProfilesConfig#outputDir
     * @see ExecProfilesConfig#outputName
     * @see ExecProfilesConfig#profileLocally
     * @see ExecProfilesConfig#daysToWarnAboutOutdatedProfiles
     * @see ExecProfilesConfig#checkExistence
     * @see ExecProfilesConfig#testRunTimeout
     * @see ExecProfilesConfig#profileRunTimeout
     */
    private ExecProfilesConfig execProfilesConfiguration;

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
     * Application packaging mode. Permitted values are:
     * <dl>
     * <dt>zip</dt>
     * <dd>zip archive with a self-contained application package (default)</dd>
     * <dt>tar-gz</dt>
     * <dd>tar.gz archive with a self-contained application package</dd>
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
     * The inception year of this project.
     *
     * Used to construct the default value of {@link WindowsVersionInfoConfig#copyright}.
     */
    private String inceptionYear;

    /**
     * (Windows) If set to {@code true}, a version-information resource will be added to the final executable.
     *
     * @see WindowsVersionInfoConfig#company
     * @see WindowsVersionInfoConfig#product
     * @see WindowsVersionInfoConfig#version
     * @see WindowsVersionInfoConfig#copyright
     * @see WindowsVersionInfoConfig#description
     */
    private Boolean addWindowsVersionInfo;

    /**
     * Windows version-information resource description.
     */
    private WindowsVersionInfoConfig windowsVersionInfoConfiguration;

    /**
     * Optimization presets define the default optimization mode for application dependencies.
     * There are two optimization presets available: {@code typical} and {@code smart}.
     *
     * <dl>
     * <dt>{@code typical} (default)</dt>
     * <dd>
     * Compile all classes from all dependencies to optimized native code.
     * </dd>
     * <dt>{@code smart}</dt>
     * <dd>
     * Use heuristics to determine which of the project dependencies are libraries and
     * compile them selectively, leaving the supposedly unused classes in bytecode form.
     * </dd>
     * </dl>
     * <p>
     * For details, refer to the Excelsior JET User's Guide, Chapter "JET Control Panel",
     * section "Step 3: Selecing a compilation mode / Classpath Grid / Selective Optimization".
     * </p>
     * <p>
     * <strong>Note:</strong> Unlike the identically named preset of the JET Control Panal,
     * selecting the {@code smart} preset does NOT automatically enable the Global Optimizer.
     * </p>
     *
     * @see #dependencies
     * @see DependencySettings
     * @see #globalOptimizer
     */
    private String optimizationPreset;


    /**
     * If set to {@code true}, the Global Optimizer is enabled,
     * providing higher performance and lower memory usage for the compiled application.
     *
     * <p>
     * The Global Optimizer detects the Java Platform API classes that your application actually uses,
     * and compiles them together with your application classes into a single executable.
     * The remaining classes are kept in the bytecode form. For details, refer to the Chapter
     * "Global Optimizer" in the Excelsior JET User's Guide.
     * </p>
     * <p>
     * <strong>Note:</strong> Performing a Test Run is mandatory when the Global Optimizer is enabled.
     * The Global Optimizer is enabled automatically when you enable Java Runtime Slim-Down.
     * </p>
     * @see TestRunTask
     * @see RuntimeConfig#slimDown
     */
    private boolean globalOptimizer;

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
     * Runtime configuration parameters.
     *
     * @see RuntimeConfig#flavor
     * @see RuntimeConfig#profile
     * @see RuntimeConfig#components
     * @see RuntimeConfig#locales
     * @see RuntimeConfig#diskFootprintReduction
     * @see RuntimeConfig#location
     */
    private RuntimeConfig runtimeConfiguration;

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
     * Allocate on the stack the Java objects that do not escape the scope
     * of the allocating method. By default, the parameter is set to {@code true}.
     *
     * This optimization may increase the consumption of stack memory
     * by application threads, so you may wish to disable it if your application runs
     * thousands of threads simultaneously.
     */
    private boolean stackAllocation;

    /**
     * (Windows) If set to {@code true}, the resulting executable file will not have a console upon startup.
     */
    private boolean hideConsole;

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
     * Command line arguments passed to the application during a Test Run, startup profiling,
     * execution profiling and normal run.
     * You may also set the parameter via the {@code jet.runArgs} system property, where arguments
     * are comma separated (use "\" to escape commas inside arguments,
     * i.e. {@code -Djet.runArgs="arg1,Hello\, World"} will be passed to your application as
     * {@code arg1 "Hello, World"}).
     */
    private String[] runArgs;

    /**
     * Command-line parameters for multi-app executables. If set, overrides the {@link #runArgs} parameter.
     * <p>]
     * If you set {@link #multiApp} to {@code true}, the resulting executable expects its command line
     * arguments to be in the respective format:
     * <p>
     * {@code [VM-options] main-class [arguments]} or<br>
     * {@code [VM-options] -args [arguments]} (use default main class)
     * </p>
     * <p>
     * So if you need to alter the main class and/or VM properties during startup profiling,
     * execution profiling, or normal run, set this parameter. 
     * </p>
     * <p>
     * You may also set the parameter via the {@code jet.multiAppRunArgs} system property, where arguments
     * are comma separated (use "\" to escape commas inside arguments,
     * i.e. {@code -Djet.multiAppRunArgs="-args,arg1,Hello\, World"} will be passed to your application
     * as {@code -args arg1 "Hello, World"})
     * </p>
     */
    private String[] multiAppRunArgs;

    /**
     * Project Database placement configuration.
     *
     * @see PDBConfig
     */
    private PDBConfig pdbConfiguration;

    /**
     * Termination policy for {@link StopTask}. Permitted values are:
     * <dl>
     * <dt>ctrl-c</dt>
     * <dd>Send Ctrl-C event to a running application</dd>
     * <dt>halt</dt>
     * <dd>call java.lang.Shutdown.halt() (System.exit()) within a running application</dd>
     * </dl>
     *
     * Applications may perform some shutdown actions upon termination (e.g. close a database).
     * Some applications do not terminate well on System.exit() call such as Tomcat and Spring Boot applications.
     * So by default, we use Ctrl-C termination policy for such applications to terminate properly.
     */
    private String terminationPolicy;

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
     * @param creatorPlugin name and version of the plugin that creates this project.
     * @param projectName project name
     * @param groupId project groupId
     * @param version project version
     * @param appType application type
     * @param targetDir target directory of the enclosing project
     * @param jetResourcesDir directory with jet specific resources
     */
    public JetProject(String creatorPlugin, String projectName, String groupId, String version, ApplicationType appType, File targetDir, File jetResourcesDir) {
        this.creatorPlugin = requireNonNull(creatorPlugin, "plugin cannot be null");
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

        if ((mainWar != null) && (mainJar != null)) {
            throw new JetTaskFailureException(s("JetApi.BothMainJarAndWarSet.Failure"));
        }

        switch (appType) {
            case WINDOWS_SERVICE:
                if (!excelsiorJet.isWindowsServicesSupported()) {
                    throw new JetTaskFailureException(s("JetApi.WinServiceNotSupported.Failure"));
                }
                checkMainJar();
                break;

            case SPRING_BOOT:
                if (!excelsiorJet.isSpringBootSupported()) {
                    throw new JetTaskFailureException(s("JetApi.SpringBootNotSupported.Failure"));
                }

                if (!checkSpringBootArtifact()) {
                    throw new JetTaskFailureException(s("JetApi.SpringBoot.ArchiveIsNotSpringBootArchive.Failure", mainJar.getAbsolutePath()));
                }
                break;

            case PLAIN:
            case DYNAMIC_LIBRARY:
                checkMainJar();
                break;

            case TOMCAT:
                if (!excelsiorJet.isTomcatSupported()) {
                    throw new JetTaskFailureException(s("JetApi.TomcatNotSupported.Failure"));
                }

                checkMainWar();

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
                if ((appType == ApplicationType.PLAIN) &&
                        mainClass.equals(SPRING_BOOT_JAR_MAIN_CLASS.replace('.', '/')) &&
                        checkSpringBootArtifact(mainJar, true, false)) {
                    if (!excelsiorJet.isSpringBootSupported()) {
                        throw new JetTaskFailureException(s("JetApi.SpringBootNotSupported.Failure"));
                    }
                    appType = ApplicationType.SPRING_BOOT;
                }
                break;
            case DYNAMIC_LIBRARY:
                //no need to check main here
                break;
            case TOMCAT:
                mainClass = "org/apache/catalina/startup/Bootstrap";
                break;
            case SPRING_BOOT:
                String springBootMainClass = isMainArtifactJar() ? SPRING_BOOT_JAR_MAIN_CLASS : SPRING_BOOT_WAR_MAIN_CLASS;
                mainClass = springBootMainClass.replace('.', '/');
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

        if (jetAppDir == null) {
            jetAppDir = new File(jetOutputDir, APP_DIR);
        }

        packageFilesDir = checkFileWithDefault(packageFilesDir, PACKAGE_FILES_DIR, "packageFilesDir");

        if (excelsiorJetPackaging == null) {
            excelsiorJetPackaging = ZIP.toString();
        }

        //check packaging type
        switch (PackagingType.validate(excelsiorJetPackaging)) {
            case ZIP:
            case NONE:
                break;
            case TAR_GZ:
                if (excelsiorJet.isCrossCompilation() && Host.isWindows()) {
                    // Cannot pack to tar.gz on Windows for Linux target
                    // because we do not know what files should have executable Unix mode
                    // in the resulting tar.gz archive.
                    // Should be supported in xpack.
                    throw new JetTaskFailureException(s("JetApi.TarGZOnWindowsHostLinuxTarget.NotSupported"));
                }
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
                throw new AssertionError("Unknown packaging type: " + excelsiorJetPackaging);
        }

        if ((appType == ApplicationType.WINDOWS_SERVICE) && (excelsiorJetPackaging() == EXCELSIOR_INSTALLER) &&
                !excelsiorJet.isWindowsServicesInExcelsiorInstallerSupported()) {
            throw new JetTaskFailureException(s("JetApi.WinServiceInEINotSupported.Failure"));
        }

        execProfilesConfiguration.fillDefaults(this, excelsiorJet);

        // Override run args from system property
        String runArgs = System.getProperty("jet.runArgs");
        if (runArgs != null) {
            this.runArgs = Utils.parseRunArgs(runArgs);
        }

        if ((packageFilesDir() != null) && appType == ApplicationType.TOMCAT) {
            throw new JetTaskFailureException(s("JetApi.PackageFilesForTomcat.Error", "packageFilesDir"));
        }

        if (packageFiles.size() > 0) {
            if (appType == ApplicationType.TOMCAT) {
                throw new JetTaskFailureException(s("JetApi.PackageFilesForTomcat.Error", "packageFiles"));
            }
            for (PackageFile pFile: packageFiles) {
                if (pFile.path == null) {
                    throw new JetTaskFailureException(s("JetApi.PathNotSetForPackageFile.Error"));
                }
                pFile.validate("JetApi.PackageFileDoesNotExist.Error");
            }
        }

        if (terminationPolicy == null) {
            terminationPolicy = TerminationPolicy.CTRL_C.toString();
        } else {
            TerminationPolicy.validate(terminationPolicy);
        }


        if (validateForBuild) {
            validateForBuild(excelsiorJet);
        }

        processDependencies();
    }

    private void checkMainJar() throws JetTaskFailureException {
        if (mainJar == null) {
            mainJar = new File(targetDir, artifactName + ".jar");
        }

        if (!mainJar.exists()) {
            throw new JetTaskFailureException(s("JetApi.MainJarNotFound.Failure", mainJar.getAbsolutePath()));
        }
    }

    private void checkMainWar() throws JetTaskFailureException {
        if (mainWar == null) {
            mainWar = new File(targetDir, artifactName + ".war");
        }

        if (!mainWar.exists()) {
            throw new JetTaskFailureException(s("JetApi.MainWarNotFound.Failure", mainWar.getAbsolutePath()));
        }

        if (!mainWar.getName().endsWith(TomcatConfig.WAR_EXT)) {
            throw new JetTaskFailureException(s("JetApi.MainWarShouldEndWithWar.Failure", mainWar.getAbsolutePath()));
        }
    }

    void processDependencies() throws JetTaskFailureException {
        if (optimizationPreset == null) {
            optimizationPreset = OptimizationPreset.TYPICAL.toString();
        } else {
            OptimizationPreset.validate(optimizationPreset);
        }

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
        ProjectDependency mainArtifactDep = new ProjectDependency(groupId, projectName, version, mainArtifact(), true);
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
                throw new JetTaskFailureException(s("JetApi.ApplicationCannotHaveExternalDependencies", externalDependency.path, "Tomcat web"));
            } else if (appType == ApplicationType.SPRING_BOOT) {
                throw new JetTaskFailureException(s("JetApi.ApplicationCannotHaveExternalDependencies", externalDependency.path, "Spring Boot"));
            }

            if (!externalDependency.path.exists()) {
                throw new JetTaskFailureException(s("JetApi.ExternalDependencyDoesNotExist", externalDependency.path));
            }
        }

        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(optimizationPreset(), groupId, dependenciesSettings);
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
            case SPRING_BOOT:
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
                            throw new JetTaskFailureException(s("JetApi.OverlappedTomcatOrSpringBootDependency",
                                    prjDep, oldDep, isMainArtifactJar() ? "jar": "war"));
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

        icon = checkFileWithDefault(icon, "icon.ico", "icon");

        splash = checkFileWithDefault(splash, "splash.png", "splash");

        if (outputName == null) {
            if ((appType() == ApplicationType.DYNAMIC_LIBRARY) && excelsiorJet.getTargetOS().isUnix()) {
                //add lib prefix to output name
                outputName = "lib" + projectName;
            } else {
                outputName = projectName;
            }
        }

        if (stackTraceSupport == null) {
            stackTraceSupport = StackTraceSupportType.MINIMAL.toString();
        } else {
            StackTraceSupportType.validate(stackTraceSupport);
        }

        if (inlineExpansion == null) {
            inlineExpansion = InlineExpansionType.AGGRESSIVE.toString();
        } else {
            InlineExpansionType.validate(inlineExpansion);
        }

        // check version info
        try {
            checkVersionInfo(excelsiorJet);

            if (multiApp && !excelsiorJet.isMultiAppSupported()) {
                throw new JetTaskFailureException(s("JetApi.NoMultiappInStandard.Failure"));
            }

            // Override multiApp run args from system property
            String multiAppRunArgs = System.getProperty("jet.multiAppRunArgs");
            if (multiAppRunArgs != null) {
                this.multiAppRunArgs = Utils.parseRunArgs(multiAppRunArgs);
            }
            if (multiApp || appType() == ApplicationType.TOMCAT) { //tomcat is always multiApp
                if (Utils.isEmpty(this.multiAppRunArgs) && !Utils.isEmpty(this.runArgs)) {
                    this.multiAppRunArgs = Utils.prepend("-args", this.runArgs);
                }
            } else if (!Utils.isEmpty(this.multiAppRunArgs)) {
                throw new JetTaskFailureException(s("JetApi.MultiAppRunArgsNotForMultiApp.Failure"));
            }

            if (profileStartup) {
                if (!excelsiorJet.isStartupAcceleratorSupported()) {
                    // startup accelerator is enabled by default,
                    // so if it is not supported, no warn about it.
                    profileStartup = false;
                }
            }

            runtimeConfiguration.fillDefaults(this, excelsiorJet);

            checkTrialVersionConfig(excelsiorJet);

            checkGlobal(excelsiorJet);

            checkExcelsiorInstallerConfig(excelsiorJet);

            checkWindowsServiceConfig();

            checkOSXBundleConfig();

            pdbConfiguration.fillDefaults(this, excelsiorJet);

            checkProtectData(excelsiorJet);

        } catch (JetHomeException e) {
            throw new JetTaskFailureException(e.getMessage());
        }
    }

    public File checkFileWithDefault(File file, String defaultFileName, String notExistParam) throws JetTaskFailureException {
        return Utils.checkFileWithDefault(file, new File(jetResourcesDir, defaultFileName),
                "JetApi.FileDoesNotExist.Error", notExistParam);
    }

    private void checkVersionInfo(ExcelsiorJet excelsiorJet) throws JetHomeException, JetTaskFailureException {
        if (addWindowsVersionInfo == null) {
            addWindowsVersionInfo = !windowsVersionInfoConfiguration.isEmpty();
        }
        if (!addWindowsVersionInfo && !windowsVersionInfoConfiguration.isEmpty()) {
            throw new JetTaskFailureException(s("JetApi.AddWindowsVersionInfo.Failure"));
        }

        if (!excelsiorJet.getTargetOS().isWindows()) {
            addWindowsVersionInfo = false;
        }
        if (addWindowsVersionInfo && !excelsiorJet.isWindowsVersionInfoSupported()) {
            logger.warn(s("JetApi.NoVersionInfoInStandard.Warning"));
            addWindowsVersionInfo = false;
        }
        if (addWindowsVersionInfo || excelsiorJetPackaging().isNativeBundle()) {
            if (Utils.isEmpty(vendor)) {
                //No organization name. Get it from groupId.
                if (Utils.isEmpty(groupId)) {
                    if (addWindowsVersionInfo) {
                        throw new JetTaskFailureException(s("JetApi.VendorIsNotSetForVersionInfo"));
                    } else {
                        throw new JetTaskFailureException(s("JetApi.VendorIsNotSetForPackaging", excelsiorJetPackaging().toString()));
                    }
                }
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
            windowsVersionInfoConfiguration.fillDefaults(this);
        }
    }

    ExecProfilesConfig execProfiles() {
        return execProfilesConfiguration;
    }

    private void checkGlobal(ExcelsiorJet excelsiorJet) throws JetHomeException, JetTaskFailureException {
        if (globalOptimizer) {
            if (!excelsiorJet.isGlobalOptimizerSupported()) {
                logger.warn(s("JetApi.NoGlobal.Warning"));
                globalOptimizer = false;
            }
        }

        if (globalOptimizer) {
            ExecProfilesConfig execProfiles = execProfiles();
            if (!execProfiles.getUsg().exists()) {
                throw new JetTaskFailureException(s("JetApi.NoTestRun.Failure"));
            }
        }
    }

    private void checkTrialVersionConfig(ExcelsiorJet excelsiorJet) throws JetTaskFailureException, JetHomeException {
        if ((trialVersion != null) && trialVersion.isDefined()) {
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

    private void checkExcelsiorInstallerConfig(ExcelsiorJet excelsiorJet) throws JetTaskFailureException {
        if (excelsiorJetPackaging() == EXCELSIOR_INSTALLER) {
            excelsiorInstallerConfiguration.fillDefaults(this, excelsiorJet);
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

    private void checkOSXBundleConfig() throws JetTaskFailureException {
        if (excelsiorJetPackaging() == OSX_APP_BUNDLE) {
            String fourDigitVersion = Utils.deriveFourDigitVersion(version);
            osxBundleConfiguration.fillDefaults(this, outputName, product,
                    Utils.deriveFourDigitVersion(version),
                    Utils.deriveFourDigitVersion(fourDigitVersion.substring(0, fourDigitVersion.lastIndexOf('.'))));
            if (osxBundleConfiguration.icon == null) {
                logger.warn(s("JetApi.NoIconForOSXAppBundle.Warning"));
            }
        }

    }

    private void checkProtectData(ExcelsiorJet excelsiorJet) throws JetTaskFailureException {
        if (protectData) {
            if (!excelsiorJet.isDataProtectionSupported()) {
                throw new JetTaskFailureException(s("JetApi.NoDataProtectionInStandard.Failure"));
            } else {
                if (cryptSeed == null) {
                    File cryptSeedFile = new File(pdbConfiguration.pdbLocation(), "cryptseed");
                    if (cryptSeedFile.exists()) {
                        //read cryptseed from PDB if exists.
                        try {
                            cryptSeed = Files.readAllLines(cryptSeedFile.toPath()).get(0);
                        } catch (Exception e) {
                            logger.warn(e.getMessage(), e);
                        }
                    }
                    if (cryptSeed == null) {
                        cryptSeed = Utils.randomAlphanumeric(64);
                        //store cryptseed to pdb to stabalize it.
                        pdbConfiguration().pdbLocation().mkdirs();
                        try (Writer writer = new BufferedWriter(new FileWriter(cryptSeedFile))) {
                            writer.write(cryptSeed);
                        } catch (IOException e) {
                            logger.warn(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks that either the mainWar or the mainJar parameter is specified, and that
     * the respective file exists and conforms to the Spring Boot jar or war structure.
     */
    private boolean checkSpringBootArtifact() throws JetTaskFailureException {
        boolean isSpringBootArchive = false;
        if ((mainJar == null) && (mainWar == null)) {
            //both parameters are null, try to autodetect
            mainJar = new File(targetDir, artifactName + ".jar");
            mainWar = new File(targetDir, artifactName + ".war");
            if (mainJar.exists() && mainWar.exists()) {
                throw new JetTaskFailureException(s("JetApi.BothMainJarAndWarFound.Failure", mainJar.getAbsolutePath(), mainWar.getAbsolutePath()));
            } else if (mainJar.exists()) {
                isSpringBootArchive = true;
                mainWar = null;
            } else if (mainWar.exists()) {
                mainJar = null;
            } else {
                throw new JetTaskFailureException(s("JetApi.MainJarOrWarNotFound.Failure", mainJar.getAbsolutePath(), mainWar.getAbsolutePath()));
            }


        } else if (mainJar != null) {
            if (!mainJar.exists()) {
                throw new JetTaskFailureException(s("JetApi.MainJarNotFound.Failure", mainJar.getAbsolutePath()));
            }

            isSpringBootArchive = true;
        } else {
            assert mainWar !=null;
            if (!mainWar.exists()) {
                throw new JetTaskFailureException(s("JetApi.MainWarNotFound.Failure", mainWar.getAbsolutePath()));
            }
        }

        return checkSpringBootArtifact(mainArtifact(), isSpringBootArchive, true);
    }

    /**
     * Returns {@code true} for supported versions.
    */
    private boolean checkSpringBootVersion(String version) {
        String[] versionParts = version.split("\\.");
        if (versionParts.length < 2) {
            return false;
        }

        try {
            int major = Integer.valueOf(versionParts[0]);
            int minor = Integer.valueOf(versionParts[1]);
            return (major > 1) || (major == 1) && (minor >= 4);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks that {@code mainArtifact} conforms to the Spring Boot jar or war structure according
     * {@code checkJar} parameter.
     *
     * @param mainArtifact artifact to check
     * @param checkJar if the artifact is jar
     * @param failOnVersionCheck if we should throw an exception on unsupported Spring Boot version
     * @return {@code true} if the artifact conforms to the respecitve Spring Boot archive structure.
     *
     * @throws JetTaskFailureException if {@code failOnVersionCheck} and Spring Boot version is not supported
     */
    private boolean checkSpringBootArtifact(File mainArtifact, boolean checkJar, boolean failOnVersionCheck) throws JetTaskFailureException {
        Manifest manifest;
        try {
            manifest =  new JarFile(mainArtifact).getManifest();
        } catch (IOException e) {
            return false;
        }

        if (manifest == null) {
            return false;
        }

        Attributes mainAttributes = manifest.getMainAttributes();
        String main = mainAttributes.getValue(Name.MAIN_CLASS);
        if (checkJar) {
            if (!SPRING_BOOT_JAR_MAIN_CLASS.equals(main)) {
                return false;
            }
        } else {
            if (!SPRING_BOOT_WAR_MAIN_CLASS.equals(main)) {
                return false;
            }
        }

        String startClass = mainAttributes.getValue(SPRING_BOOT_START_CLASS_ATTR);
        if (startClass == null) {
            return false;
        }

        String springBootVersion = mainAttributes.getValue(SPRING_BOOT_VERSION_ATTR);
        if ((springBootVersion != null) && !checkSpringBootVersion(springBootVersion)) {
            if (failOnVersionCheck) {
                throw new JetTaskFailureException(Txt.s("JetApi.SpringBoot.NotSupportedVersion.Failure", mainJar.getAbsolutePath(), springBootVersion));
            } else {
                return false;
            }
        }

        return true;
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

    /**
     * Copies Spring Boot jar/war to the build directory.
     */
    void copySpringBootArtifact() throws IOException {
        try {
            Utils.copyFile(mainArtifact().toPath(), new File(jetBuildDir, mainArtifact().getName()).toPath());
        } catch (IOException e) {
            throw new IOException(s("JetApi.ErrorCopyingSpringBootArchive.Exception", mainArtifact().getAbsolutePath()), e.getCause());
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

    public String creatorPlugin() {
        return creatorPlugin;
    }

    public String projectName() {
        return projectName;
    }

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

    List<PackageFile> packageFiles() {
        return packageFiles;
    }

    String[] jvmArgs() {
        return jvmArgs;
    }

    boolean isAddWindowsVersionInfo() {
        return addWindowsVersionInfo;
    }

    public String inceptionYear() {
        return inceptionYear;
    }

    PackagingType excelsiorJetPackaging() {
        return PackagingType.fromString(excelsiorJetPackaging);
    }

    WindowsVersionInfoConfig windowsVersionInfoConfiguration() {
        return windowsVersionInfoConfiguration;
    }

    OptimizationPreset optimizationPreset() {
        return OptimizationPreset.fromString(optimizationPreset);
    }

    public boolean globalOptimizer() {
        return globalOptimizer;
    }

    public RuntimeConfig runtimeConfiguration() {
        return runtimeConfiguration;
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

    public String version() {
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

    boolean stackAllocation() {
        return stackAllocation;
    }

    boolean hideConsole() {
        return hideConsole;
    }

    int profileStartupTimeout() {
        return profileStartupTimeout;
    }

    public File jetOutputDir() {
        return jetOutputDir;
    }

    public ApplicationType appType()  {
        return appType;
    }

    String[] compilerOptions() {
        return compilerOptions;
    }

    public String[] runArgs() {
        return runArgs;
    }

    public String[] multiAppRunArgs() {
        return multiAppRunArgs;
    }

    public String[] exeRunArgs() {
        return (multiApp || appType == ApplicationType.TOMCAT) ? multiAppRunArgs : runArgs;
    }

    PDBConfig pdbConfiguration() {
        return pdbConfiguration;
    }

    public String getTerminationVMProp(File termFile) {
        switch (TerminationPolicy.fromString(terminationPolicy)) {
            case CTRL_C:
                return "-Djet.ctrlc.signal.file=" + termFile.getAbsolutePath();
            case HALT:
                return  "-Djet.terminator=" + termFile.getAbsolutePath();
            default:
                throw new AssertionError("Unknown termination policy:" + terminationPolicy);
        }
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

    public JetProject packageFiles(List<PackageFile> packageFiles) {
        this.packageFiles = packageFiles;
        return this;
    }

    public JetProject execProfiles(ExecProfilesConfig execProfilesConfig) {
        this.execProfilesConfiguration = execProfilesConfig;
        return this;
    }

    public JetProject jvmArgs(String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public JetProject addWindowsVersionInfo(Boolean addWindowsVersionInfo) {
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

    public JetProject windowsVersionInfoConfiguration(WindowsVersionInfoConfig windowsVersionInfoConfiguration) {
        this.windowsVersionInfoConfiguration = windowsVersionInfoConfiguration;
        return this;
    }

    public JetProject inceptionYear(String inceptionYear) {
        this.inceptionYear = inceptionYear;
        return this;
    }

    public JetProject optimizationPreset(String optimizationPreset) {
        this.optimizationPreset = optimizationPreset;
        return this;
    }

    public JetProject globalOptimizer(boolean globalOptimizer) {
        this.globalOptimizer = globalOptimizer;
        return this;
    }

    public JetProject runtimeConfiguration(RuntimeConfig runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
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

    public JetProject stackAllocation(boolean stackAllocation) {
        this.stackAllocation = stackAllocation;
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

    public JetProject jetOutputDir(File jetOutputDir) {
        this.jetOutputDir = jetOutputDir;
        return this;
    }

    public JetProject jetBuildDir(File jetBuildDir) {
        this.jetBuildDir = jetBuildDir;
        return this;
    }

    public JetProject jetAppDir(File jetAppDir) {
        this.jetAppDir = jetAppDir;
        return this;
    }

    public JetProject compilerOptions(String[] compilerOptions) {
        this.compilerOptions = compilerOptions;
        return this;
    }

    public JetProject runArgs(String[] runArgs) {
        this.runArgs = runArgs;
        return this;
    }

    public JetProject multiAppRunArgs(String[] runArgs) {
        this.multiAppRunArgs = runArgs;
        return this;
    }

    public JetProject pdbConfiguration(PDBConfig pdbConfig) {
        pdbConfiguration = pdbConfig;
        return this;
    }

    public JetProject terminationPolicy(String terminationPolicy) {
        this.terminationPolicy = terminationPolicy;
        return this;
    }

    public File jetBuildDir() {
        return jetBuildDir;
    }

    public File jetAppDir() {
        return jetAppDir;
    }

    public File jetAppToProfileDir() {
        return execProfilesConfiguration.profilingImageDir;
    }

    public boolean isProfileLocally() {
        return execProfilesConfiguration.profileLocally;
    }

    public boolean isMainArtifactJar() {
        return mainJar != null;
    }

    public File mainArtifact() {
        switch (appType) {
            case TOMCAT:
                return mainWar;
            case SPRING_BOOT:
                return isMainArtifactJar() ? mainJar : mainWar;
            default:
                return mainJar;
        }
    }

    public static ApplicationType checkAndGetAppType(String appType) throws JetTaskFailureException {
        return ApplicationType.validate(appType);
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

    String exeRelativePath(ExcelsiorJet excelsiorJet) {
        //TODO: add configuration to customize exe location.
        switch (appType) {
            case PLAIN:
            case WINDOWS_SERVICE:
            case SPRING_BOOT:
                return excelsiorJet.getTargetOS().mangleExeName(outputName());
            case DYNAMIC_LIBRARY:
                return excelsiorJet.getTargetOS().mangleDllName(outputName(), false);
            case TOMCAT:
                return "bin" + File.separatorChar + excelsiorJet.getTargetOS().mangleExeName(outputName());
            default:
                throw new AssertionError("Unknown apptype: " + appType);
        }
    }
}
