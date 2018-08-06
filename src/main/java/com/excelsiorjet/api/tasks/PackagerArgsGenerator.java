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
import com.excelsiorjet.api.platform.Host;
import com.excelsiorjet.api.tasks.config.ApplicationType;
import com.excelsiorjet.api.tasks.config.excelsiorinstaller.*;
import com.excelsiorjet.api.tasks.config.packagefile.PackageFile;
import com.excelsiorjet.api.tasks.config.runtime.RuntimeConfig;
import com.excelsiorjet.api.tasks.config.windowsservice.WindowsServiceConfig;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * Provides {@code xpack} arguments derived from the given {@link JetProject}.
 *
 * @author Aleksey Zhidkov
 */
public class PackagerArgsGenerator {

    private final JetProject project;
    private final ExcelsiorJet excelsiorJet;

    public PackagerArgsGenerator(JetProject project, ExcelsiorJet excelsiorJet) {
        this.project = project;
        this.excelsiorJet = excelsiorJet;
    }

    ArrayList<XPackOption> getCommonXPackOptions() throws JetTaskFailureException {
        ArrayList<XPackOption> xpackOptions = new ArrayList<>();

        File source = null;
        String exeRelativePath = project.exeRelativePath(excelsiorJet);
        switch (project.appType()) {
            case DYNAMIC_LIBRARY:
            case PLAIN:
            case WINDOWS_SERVICE:
            case SPRING_BOOT:
                if (project.packageFilesDir() != null) {
                    source = project.packageFilesDir();
                    xpackOptions.add(new XPackOption("-source", source.getAbsolutePath()));
                }

                xpackOptions.add(new XPackOption("-add-file", exeRelativePath, "/"));

                for (PackageFile pfile : project.packageFiles()) {
                    xpackOptions.add(new XPackOption("-add-file", pfile.path.getAbsolutePath(), pfile.packagePath));
                }
                break;
            case TOMCAT:
                source = project.tomcatInBuildDir();
                xpackOptions.add(new XPackOption("-source", source.getAbsolutePath()));
                break;
            default:
                throw new AssertionError("Unknown app type");
        }

        RuntimeConfig runtime = project.runtimeConfiguration();

        if (runtime.location != null) {
            String defaultRtLocation = "rt";
            String rtLocation = defaultRtLocation;
            if (source != null) {
                if (new File(source, runtime.location).exists()) {
                    throw new JetTaskFailureException(s("JetBuildTask.RuntimeLocationClash.Failure", runtime.location, source.getAbsolutePath()));
                }
                //check that user files does not contain a file with "rt" name;
                int i = 0;
                while (new File(source, rtLocation).exists()) {
                    // if "rt" file exists in user files, xpack will try to assign "rt_i" for runtime,
                    rtLocation = defaultRtLocation + '_' + i++;
                }
            }
            if (!runtime.location.equals("rt")) {
                xpackOptions.add(new XPackOption("-move-file", rtLocation, runtime.location));
            }
        }

        if (!Utils.isEmpty(runtime.components)) {
            if (checkNone(runtime.components)) {
                xpackOptions.add(new XPackOption("-remove-opt-rt-files", "all"));
            } else {
                xpackOptions.add(new XPackOption("-add-opt-rt-files", String.join(",", runtime.components)));
                if (Arrays.stream(runtime.components).noneMatch(s -> (s.equalsIgnoreCase("jce") || s.equalsIgnoreCase("all")))) {
                    //jce is included by default, so if it is not in the list remove it
                    xpackOptions.add(new XPackOption("-remove-opt-rt-files", "jce"));
                }
            }
        }

        if (!Utils.isEmpty(runtime.locales)) {
            if (checkNone(runtime.locales)) {
                xpackOptions.add(new XPackOption("-remove-locales", "all"));
            } else {
                xpackOptions.add(new XPackOption("-add-locales", String.join(",", runtime.locales)));
                if (Arrays.stream(runtime.locales).noneMatch(s -> (s.equalsIgnoreCase("european") || s.equalsIgnoreCase("all")))) {
                    //european is included by default, so if it is not in the list remove it
                    xpackOptions.add(new XPackOption("-remove-locales", "european"));
                }
            }
        }


        if (runtime.slimDown != null) {
            xpackOptions.add(new XPackOption("-detached-base-url", runtime.slimDown.detachedBaseURL));
            xpackOptions.add(new XPackOption("-detach-components",
                    (runtime.slimDown.detachComponents != null && runtime.slimDown.detachComponents.length > 0) ?
                            String.join(",", runtime.slimDown.detachComponents) : "auto"));
            xpackOptions.add(new XPackOption("-detached-package",
                    new File(project.jetOutputDir(), runtime.slimDown.detachedPackage).getAbsolutePath()
            ));
        }

        if (excelsiorJet.isCompactProfilesSupported()) {
            xpackOptions.add(new XPackOption("-profile",  runtime.profile));
        }

        if (runtime.diskFootprintReduction != null) {
            xpackOptions.add(new XPackOption("-reduce-disk-footprint", runtime.diskFootprintReduction));
        }

        if (project.appType() != ApplicationType.TOMCAT) {
            for (ClasspathEntry classpathEntry : project.classpathEntries()) {
                Path depInBuildDir = project.toPathRelativeToJetBuildDir(classpathEntry);
                if ((ClasspathEntry.PackType.NONE == classpathEntry.getEffectivePack()) &&
                        (project.appType() != ApplicationType.SPRING_BOOT || classpathEntry.isMainArtifact)) {
                    if (classpathEntry.packagePath == null) {
                        if (classpathEntry.disableCopyToPackage != null && classpathEntry.disableCopyToPackage) {
                            xpackOptions.add(new XPackOption("-disable-resource", exeRelativePath, depInBuildDir.getFileName().toString()));
                        } else {
                            xpackOptions.add(new XPackOption("-add-file", depInBuildDir.toString(),
                                    classpathEntry.path.isDirectory() ? "/" : "/lib"
                            ));
                        }
                    } else {
                        xpackOptions.add(new XPackOption("-add-file", depInBuildDir.toString(), classpathEntry.packagePath));
                        xpackOptions.add(new XPackOption("-assign-resource", exeRelativePath, depInBuildDir.getFileName().toString(),
                                depInBuildDir.toString()));
                    }
                }

            }
        }

        return xpackOptions;
    }

    private static boolean argsValidForRsp(String[] args) {
        return (args == null) || Arrays.stream(args).noneMatch(arg -> arg.contains(" "));
    }

    private static String argsToString(String[] args) {
        return args == null?
               "":
               Arrays.stream(args).map(arg ->
                            arg.contains(" ") ?
                                    Host.isWindows()?
                                            // This looks weird right?
                                            // In fact this combination of " and \ was found to be working to escape spaces
                                            // within arguments by trial and error.
                                            // In short, there are three levels of magic here.
                                            // First, Java's ProcessImpl does a magic with escaping " inside arguments meeting Windows
                                            // standards but it does it wrong. Then Windows itself does some magic with interpreting
                                            // " and \. Finally xpack does some magic with interpreting " and \.
                                            // This combination may not work on some Windows flavours and with some Java microversions,
                                            // but it least it were tested with Windows 10 and Java 8 Update 101.
                                            "\\\"\\\\\"\\\"" + arg + "\\\"\\\\\"\\\""
                                    :
                                            "\"" + arg + "\""
                            :
                                arg).collect(Collectors.joining(" "));
    }

    ArrayList<XPackOption> getExcelsiorInstallerXPackOptions(File target) throws JetTaskFailureException {
        ArrayList<XPackOption> xpackOptions = getCommonXPackOptions();

        boolean canInstallTomcatAsService = (project.appType() == ApplicationType.TOMCAT) &&
                                excelsiorJet.isWindowsServicesInExcelsiorInstallerSupported();
        if ((project.appType() == ApplicationType.WINDOWS_SERVICE) ||
                 canInstallTomcatAsService && project.tomcatConfiguration().installWindowsService)
        {
            xpackOptions.addAll(getWindowsServiceArgs());
        } else if (canInstallTomcatAsService && !project.tomcatConfiguration().installWindowsService) {
            xpackOptions.add(new XPackOption("-no-tomcat-service-install"));
        }

        ExcelsiorInstallerConfig config = project.excelsiorInstallerConfiguration();
        if (config.eula != null) {
            xpackOptions.add(new XPackOption(config.eulaFlag(), config.eula.getAbsolutePath()));
        }
        if (excelsiorJet.getTargetOS().isWindows() && (config.installerSplash != null)) {
            xpackOptions.add(new XPackOption("-splash", config.installerSplash.getAbsolutePath()));
        }

        if (config.language != null) {
            xpackOptions.add(new XPackOption("-language", config.language));
        }

        if (config.cleanupAfterUninstall) {
            xpackOptions.add(new XPackOption("-cleanup-after-uninstall"));
        }

        if (config.afterInstallRunnable.isDefined()) {
            xpackOptions.add(new XPackOption("-after-install-runnable",
                    argsValidForRsp(config.afterInstallRunnable.arguments),
                    config.afterInstallRunnable.target,
                    argsToString(config.afterInstallRunnable.arguments)));
        }

        if (config.compressionLevel != null) {
            xpackOptions.add(new XPackOption("-compression-level", config.compressionLevel));
        }

        if (config.installationDirectory.isDefined()) {
            if (!Utils.isEmpty(config.installationDirectory.path)) {
                xpackOptions.add(new XPackOption("-installation-directory", config.installationDirectory.path));
            }
            if (config.installationDirectory.type != null) {
                xpackOptions.add(new XPackOption("-installation-directory-type", config.installationDirectory.type));
            }
            if (config.installationDirectory.fixed) {
                xpackOptions.add(new XPackOption("-installation-directory-fixed"));
            }
        }

        if (excelsiorJet.getTargetOS().isWindows()) {
            //handle windows only parameters
            xpackOptions.addAll(getWindowsOnlyExcelsiorInstallerOptions(config));
        }

        if (config.installCallback != null) {
            xpackOptions.add(new XPackOption("-install-callback", config.installCallback.getAbsolutePath()));
        }

        if (config.uninstallCallback.isDefined()) {
            if (config.uninstallCallback.path != null) {
                xpackOptions.add(new XPackOption("-add-file",
                        config.uninstallCallback.path.getAbsolutePath(), config.uninstallCallback.packagePath));
            }
            xpackOptions.add(new XPackOption("-uninstall-callback", config.uninstallCallback.getLocationInPackage()));
        }

        if ((project.appType() == ApplicationType.TOMCAT) && project.tomcatConfiguration().allowUserToChangeTomcatPort) {
            xpackOptions.add(new XPackOption("-allow-user-to-change-tomcat-port"));
        }

        xpackOptions.add(new XPackOption("-backend", "excelsior-installer"));
        xpackOptions.add(new XPackOption("-company", project.vendor()));
        xpackOptions.add(new XPackOption("-product", project.product()));
        xpackOptions.add(new XPackOption("-version", project.version()));
        xpackOptions.add(new XPackOption("-target", target.getAbsolutePath()));
        return xpackOptions;
    }

    private ArrayList<XPackOption> getWindowsOnlyExcelsiorInstallerOptions(ExcelsiorInstallerConfig config) {
        ArrayList<XPackOption> xpackOptions = new ArrayList<>();
        if (config.registryKey != null) {
            xpackOptions.add(new XPackOption("-registry-key", config.registryKey));
        }

        for (Shortcut shortcut: config.shortcuts) {
            if (shortcut.icon.path != null) {
                xpackOptions.add(new XPackOption("-add-file", shortcut.icon.path.getAbsolutePath(), shortcut.icon.packagePath));
            }
            xpackOptions.add(new XPackOption("-shortcut", argsValidForRsp(shortcut.arguments),
                    shortcut.location, shortcut.target, shortcut.name, shortcut.icon.getLocationInPackage(),
                    shortcut.workingDirectory, argsToString(shortcut.arguments)));
        }

        if (config.noDefaultPostInstallActions) {
            xpackOptions.add(new XPackOption("-no-default-post-install-actions"));
        }

        for (PostInstallCheckbox postInstallCheckbox: config.postInstallCheckboxes) {
            switch (postInstallCheckbox.type()) {
                case RUN:
                    xpackOptions.add(new XPackOption("-post-install-checkbox-run",
                            argsValidForRsp(postInstallCheckbox.arguments),
                            postInstallCheckbox.target, postInstallCheckbox.workingDirectory,
                            argsToString(postInstallCheckbox.arguments),
                            postInstallCheckbox.checkedArg()));
                    break;
                case OPEN:
                    xpackOptions.add(new XPackOption("-post-install-checkbox-open",
                            postInstallCheckbox.target, postInstallCheckbox.checkedArg()));
                    break;
                case RESTART:
                    xpackOptions.add(new XPackOption("-post-install-checkbox-restart", postInstallCheckbox.checkedArg()));
                    break;
                default:
                    throw new AssertionError("Unknown PostInstallCheckBox type: " + postInstallCheckbox.type);
            }
        }

        for (FileAssociation fileAssociation: config.fileAssociations) {
            if (fileAssociation.icon.path != null) {
                xpackOptions.add(new XPackOption("-add-file", fileAssociation.icon.path.getAbsolutePath(), fileAssociation.icon.packagePath));
            }
            xpackOptions.add(new XPackOption("-file-association", argsValidForRsp(fileAssociation.arguments),
                    fileAssociation.extension, fileAssociation.target, fileAssociation.description,
                    fileAssociation.targetDescription,fileAssociation.icon.getLocationInPackage(),
                    argsToString(fileAssociation.arguments), fileAssociation.checked? "checked" : "unchecked"));
        }

        if (config.welcomeImage != null) {
            xpackOptions.add(new XPackOption("-welcome-image", config.welcomeImage.getAbsolutePath()));
        }

        if (config.installerImage != null) {
            xpackOptions.add(new XPackOption("-installer-image", config.installerImage.getAbsolutePath()));
        }

        if (config.uninstallerImage != null) {
            xpackOptions.add(new XPackOption("-uninstaller-image", config.uninstallerImage.getAbsolutePath()));
        }
        return xpackOptions;
    }

    //Surprisingly Windows just removes empty argument from list of arguments if we do not pass "" instead.
    private static String escapeEmptyArgForWindows(String arg) {
        return arg.isEmpty() ? "\"\"" : arg;
    }

    private ArrayList<XPackOption> getWindowsServiceArgs() {
        ArrayList<XPackOption> xpackOptions = new ArrayList<>();
        String exeRelPath = project.exeRelativePath(excelsiorJet);
        WindowsServiceConfig serviceConfig = project.windowsServiceConfiguration();
        xpackOptions.add(new XPackOption("-service",
                argsValidForRsp(serviceConfig.arguments),
                exeRelPath,
                argsToString(serviceConfig.arguments),
                serviceConfig.displayName,
                serviceConfig.description
        ));

        String logOnType;
        switch (serviceConfig.getLogOnType()) {
            case LOCAL_SYSTEM_ACCOUNT:
                logOnType = serviceConfig.allowDesktopInteraction ? "system-desktop" : "system";
                break;
            case USER_ACCOUNT:
                logOnType = "user";
                break;
            default:
                throw new AssertionError("Unknown logOnType: " + serviceConfig.getLogOnType());
        }

        String startAfterInstall = serviceConfig.startServiceAfterInstall ? "start-after-install" :
                "no-start-after-install";

        xpackOptions.add(new XPackOption("-service-startup",
                exeRelPath,
                logOnType,
                serviceConfig.getStartupType().toXPackValue(),
                startAfterInstall
        ));

        if (serviceConfig.dependencies != null) {
            xpackOptions.add(new XPackOption("-service-dependencies",
                    exeRelPath,
                    String.join(",", serviceConfig.dependencies)
            ));
        }
        return xpackOptions;
    }


    // checks that array contains only "none" value.
    // Unfortunately, it is not possible to know if a user sets a parameter to an empty array from Maven.
    private static boolean checkNone(String[] values) {
        return values.length == 1 && (values[0].equalsIgnoreCase("none"));
    }

    ArrayList<XPackOption> getCommonXPackOptions(String targetDir) throws JetTaskFailureException {
        ArrayList<XPackOption> xpackOptions = getCommonXPackOptions();
        xpackOptions.add(new XPackOption("-target", targetDir));
        return xpackOptions;
    }

    static ArrayList<String> optionsToArgs(ArrayList<XPackOption> options, boolean notValidToRspOnly) {
        ArrayList<String> args = new ArrayList<>();
        for (XPackOption option: options) {
            if (!notValidToRspOnly || !option.validForRspFile) {
                args.add(option.option);
                args.addAll(Arrays.stream(option.parameters).map(
                        p -> Host.isWindows() && p.isEmpty()?escapeEmptyArgForWindows(p): p).collect(Collectors.toList()));
            }
        }
        return args;
    }

    static List<String> getArgFileContent(ArrayList<XPackOption> xpackOptions) {
        return xpackOptions.stream().map(XPackOption::toArgFileLine).collect(Collectors.toList());
    }

}
