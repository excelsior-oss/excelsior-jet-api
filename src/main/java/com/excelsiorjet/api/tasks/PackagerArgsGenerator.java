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
import com.excelsiorjet.api.tasks.config.WindowsServiceConfig;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.excelsiorjet.api.log.Log.logger;
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

    public ArrayList<String> getCommonXPackArgs() throws JetTaskFailureException {
        ArrayList<String> xpackArgs = new ArrayList<>();

        String exeName = excelsiorJet.getTargetOS().mangleExeName(project.outputName());
        switch (project.appType()) {
            case DYNAMIC_LIBRARY:
                //overwrite exe name for dynamic library
                exeName = excelsiorJet.getTargetOS().mangleDllName(project.outputName());
                //fall through
            case PLAIN:
            case WINDOWS_SERVICE:
                if (project.packageFilesDir().exists()) {
                    xpackArgs.add("-source");
                    xpackArgs.add(project.packageFilesDir().getAbsolutePath());
                }

                xpackArgs.addAll(Arrays.asList(
                        "-add-file", exeName, "/"
                ));
                break;
            case TOMCAT:
                xpackArgs.add("-source");
                xpackArgs.add(project.tomcatInBuildDir().getAbsolutePath());
                if (project.packageFilesDir().exists()) {
                    logger.warn(s("TestRunTask.PackageFilesIgnoredForTomcat.Warning"));
                }
                break;
            default:
                throw new AssertionError("Unknown app type");
        }

        if ((project.optRtFiles() != null) && (project.optRtFiles().length > 0)) {
            if (checkNone(project.optRtFiles())) {
                xpackArgs.add("-remove-opt-rt-files");
                xpackArgs.add("all");
            } else {
                xpackArgs.add("-add-opt-rt-files");
                xpackArgs.add(String.join(",", project.optRtFiles()));
                if (Arrays.stream(project.optRtFiles()).noneMatch(s -> (s.equalsIgnoreCase("jce") || s.equalsIgnoreCase("all")))) {
                    xpackArgs.add("-remove-opt-rt-files");
                    //jce is included by default, so if it is not in the list remove it
                    xpackArgs.add("jce");
                }
            }
        }

        if ((project.locales() != null) && (project.locales().length > 0)) {
            if (checkNone(project.locales())) {
                xpackArgs.add("-remove-locales");
                xpackArgs.add("all");
            } else {
                xpackArgs.add("-add-locales");
                xpackArgs.add(String.join(",", project.locales()));
                if (Arrays.stream(project.locales()).noneMatch(s -> (s.equalsIgnoreCase("european") || s.equalsIgnoreCase("all")))) {
                    xpackArgs.add("-remove-locales");
                    //european is included by default, so if it is not in the list remove it
                    xpackArgs.add("european");
                }
            }
        }


        if (project.javaRuntimeSlimDown() != null) {
            xpackArgs.addAll(Arrays.asList(
                    "-detached-base-url", project.javaRuntimeSlimDown().detachedBaseURL,
                    "-detach-components",
                    (project.javaRuntimeSlimDown().detachComponents != null && project.javaRuntimeSlimDown().detachComponents.length > 0) ?
                            String.join(",", project.javaRuntimeSlimDown().detachComponents) : "auto",
                    "-detached-package", new File(project.jetOutputDir(), project.javaRuntimeSlimDown().detachedPackage).getAbsolutePath()
            ));
        }

        if (excelsiorJet.isCompactProfilesSupported()) {
            xpackArgs.add("-profile");
            xpackArgs.add(project.compactProfile().toString());
        }

        if (project.appType() != ApplicationType.TOMCAT) {
            for (ClasspathEntry classpathEntry : project.classpathEntries()) {
                Path depInBuildDir = project.toPathRelativeToJetBuildDir(classpathEntry);
                if (ClasspathEntry.PackType.NONE == classpathEntry.pack) {
                    if (classpathEntry.packagePath == null) {
                        if (classpathEntry.disableCopyToPackage != null && classpathEntry.disableCopyToPackage) {
                            xpackArgs.add("-disable-resource");
                            xpackArgs.add(exeName);
                            xpackArgs.add(depInBuildDir.getFileName().toString());
                        } else {
                            xpackArgs.add("-add-file");
                            xpackArgs.add(depInBuildDir.toString());
                            if (classpathEntry.path.isDirectory()) {
                                xpackArgs.add("/");
                            } else {
                                xpackArgs.add("/lib");
                            }
                        }
                    } else {
                        xpackArgs.add("-add-file");
                        xpackArgs.add(depInBuildDir.toString());
                        xpackArgs.add(classpathEntry.packagePath);

                        xpackArgs.add("-assign-resource");
                        xpackArgs.add(exeName);
                        xpackArgs.add(depInBuildDir.getFileName().toString());
                        xpackArgs.add(depInBuildDir.toString());
                    }
                }

            }
        }

        return xpackArgs;
    }

    public ArrayList<String> getExcelsiorInstallerXPackArgs(File target) throws JetTaskFailureException {
        ArrayList<String> xpackArgs = getCommonXPackArgs();

        boolean canInstallTomcatAsService = (project.appType() == ApplicationType.TOMCAT) &&
                                excelsiorJet.isWindowsServicesInExcelsiorInstallerSupported();
        if ((project.appType() == ApplicationType.WINDOWS_SERVICE) ||
                 canInstallTomcatAsService && project.tomcatConfiguration().installWindowsService)
        {
            addWindowsServiceArgs(xpackArgs);
        } else if (canInstallTomcatAsService && !project.tomcatConfiguration().installWindowsService) {
            xpackArgs.add("-no-tomcat-service-install");
        }

        if (project.excelsiorInstallerConfiguration().eula.exists()) {
            xpackArgs.add(project.excelsiorInstallerConfiguration().eulaFlag());
            xpackArgs.add(project.excelsiorInstallerConfiguration().eula.getAbsolutePath());
        }
        if (excelsiorJet.getTargetOS().isWindows() && project.excelsiorInstallerConfiguration().installerSplash.exists()) {
            xpackArgs.add("-splash");
            xpackArgs.add(project.excelsiorInstallerConfiguration().installerSplash.getAbsolutePath());
        }
        xpackArgs.addAll(Arrays.asList(
                "-backend", "excelsior-installer",
                "-company", project.vendor(),
                "-product", project.product(),
                "-version", project.version(),
                "-target", target.getAbsolutePath())
        );
        return xpackArgs;
    }

    //Surprisingly Windows just removes empty argument from list of arguments if we do not pass "" instead.
    private String escapeEmptyArgForWindows(String arg) {
        return arg.isEmpty() ? "\"\"" : arg;
    }

    private void addWindowsServiceArgs(ArrayList<String> xpackArgs) {
        String exeName = excelsiorJet.getTargetOS().mangleExeName(project.outputName());
        if (project.appType() == ApplicationType.TOMCAT) {
            exeName = "bin/" + exeName;
        }
        WindowsServiceConfig serviceConfig = project.windowsServiceConfiguration();
        String serviceArguments = "";
        if (serviceConfig.arguments != null) {
            serviceArguments = Arrays.stream(serviceConfig.arguments).map(s ->
                    s.contains(" ") ?
                            // This looks weird right?
                            // In fact this combination of " and \ was found to be working to escape spaces
                            // within arguments by trial and error.
                            // In short, there are three levels of magic here.
                            // First, Java's ProcessImpl does a magic with escaping " inside arguments meeting Windows
                            // standards but it does it wrong. Then Windows itself does some magic with interpreting
                            // " and \. Finally xpack does some magic with interpreting " and \.
                            // This combination may not work on some Windows flavours and with some Java microversions,
                            // but it least it were tested with Windows 10 and Java 8 Update 101.
                            "\\\"\\\\\"\\\"" + s + "\\\"\\\\\"\\\"" :
                            s).
                    collect(Collectors.joining(" "));
        }
        xpackArgs.addAll(Arrays.asList(
                "-service",
                exeName,
                escapeEmptyArgForWindows(serviceArguments),
                serviceConfig.displayName,
                escapeEmptyArgForWindows(serviceConfig.description)
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

        xpackArgs.addAll(Arrays.asList(
                "-service-startup",
                exeName,
                logOnType,
                serviceConfig.getStartupType().toXPackValue(),
                startAfterInstall
        ));

        if (serviceConfig.dependencies != null) {
            xpackArgs.addAll(Arrays.asList(
                    "-service-dependencies",
                    exeName,
                    String.join(",", serviceConfig.dependencies)
            ));
        }
    }


    // checks that array contains only "none" value.
    // Unfortunately, it is not possible to know if a user sets a parameter to an empty array from Maven.
    private static boolean checkNone(String[] values) {
        return values.length == 1 && (values[0].equalsIgnoreCase("none"));
    }

    public ArrayList<String> getCommonXPackArgs(String targetDir) throws JetTaskFailureException {
        ArrayList<String> xpackArgs = getCommonXPackArgs();
        xpackArgs.addAll(Arrays.asList(
                "-target", targetDir
        ));
        return xpackArgs;
    }
}
