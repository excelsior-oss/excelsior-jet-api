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
import com.excelsiorjet.api.tasks.config.ExcelsiorInstallerConfig;
import com.excelsiorjet.api.tasks.config.PackageFile;
import com.excelsiorjet.api.tasks.config.RuntimeConfig;
import com.excelsiorjet.api.tasks.config.WindowsServiceConfig;
import com.excelsiorjet.api.tasks.config.enums.ApplicationType;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * xpack option representation.
     * It may be passed to xpack as separate arguments in the form of "option [parameters]" and
     * as a line in xpack response file unless {@code validForRspFile} is false.
     */
    public static class Option {
        String option;
        String[] parameters;

        // Unfortunately some xpack options may contain a parameter that is list of arguments such as
        // "arg1 arg2 arg3". However if an argument has a space inside, there is no way to express such an argument
        // in xpack response file in JET 11.3 and it should be passed to xpack directly. See JET-9035 for details.
        boolean validForRspFile;

        Option(String option, String... parameters) {
            this(option, true, parameters);
        }

        Option(String option, boolean validForRspFile, String... parameters) {
            this.option = option;
            this.parameters = parameters;
            this.validForRspFile = validForRspFile;
        }

        @Override
        public boolean equals(Object obj) {
            Option option2 = (Option) obj;
            return option.equals(option2.option) && Arrays.equals(parameters, option2.parameters);
        }
    }

    public PackagerArgsGenerator(JetProject project, ExcelsiorJet excelsiorJet) {
        this.project = project;
        this.excelsiorJet = excelsiorJet;
    }

    ArrayList<Option> getCommonXPackOptions() throws JetTaskFailureException {
        ArrayList<Option> xpackOptions = new ArrayList<>();

        File source = null;
        String exeName = excelsiorJet.getTargetOS().mangleExeName(project.outputName());
        switch (project.appType()) {
            case DYNAMIC_LIBRARY:
                //overwrite exe name for dynamic library
                exeName = excelsiorJet.getTargetOS().mangleDllName(project.outputName());
                //fall through
            case PLAIN:
            case WINDOWS_SERVICE:
                if (project.packageFilesDir().exists()) {
                    source = project.packageFilesDir();
                    xpackOptions.add(new Option("-source", source.getAbsolutePath()));
                }

                xpackOptions.add(new Option("-add-file", exeName, "/"));

                if (project.packageFiles().size() > 0) {
                    for (PackageFile pfile: project.packageFiles()) {
                        xpackOptions.add(new Option("-add-file", pfile.path.getAbsolutePath(), pfile.packagePath));
                    }
                }
                break;
            case TOMCAT:
                source = project.tomcatInBuildDir();
                xpackOptions.add(new Option("-source", source.getAbsolutePath()));
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
                xpackOptions.add(new Option("-move-file", rtLocation, runtime.location));
            }
        }

        if (!Utils.isEmpty(runtime.components)) {
            if (checkNone(runtime.components)) {
                xpackOptions.add(new Option("-remove-opt-rt-files", "all"));
            } else {
                xpackOptions.add(new Option("-add-opt-rt-files", String.join(",", runtime.components)));
                if (Arrays.stream(runtime.components).noneMatch(s -> (s.equalsIgnoreCase("jce") || s.equalsIgnoreCase("all")))) {
                    //jce is included by default, so if it is not in the list remove it
                    xpackOptions.add(new Option("-remove-opt-rt-files", "jce"));
                }
            }
        }

        if (!Utils.isEmpty(runtime.locales)) {
            if (checkNone(runtime.locales)) {
                xpackOptions.add(new Option("-remove-locales", "all"));
            } else {
                xpackOptions.add(new Option("-add-locales", String.join(",", runtime.locales)));
                if (Arrays.stream(runtime.locales).noneMatch(s -> (s.equalsIgnoreCase("european") || s.equalsIgnoreCase("all")))) {
                    //european is included by default, so if it is not in the list remove it
                    xpackOptions.add(new Option("-remove-locales", "european"));
                }
            }
        }


        if (runtime.slimDown != null) {
            xpackOptions.add(new Option("-detached-base-url", runtime.slimDown.detachedBaseURL));
            xpackOptions.add(new Option("-detach-components",
                    (runtime.slimDown.detachComponents != null && runtime.slimDown.detachComponents.length > 0) ?
                            String.join(",", runtime.slimDown.detachComponents) : "auto"));
            xpackOptions.add(new Option("-detached-package",
                    new File(project.jetOutputDir(), runtime.slimDown.detachedPackage).getAbsolutePath()
            ));
        }

        if (excelsiorJet.isCompactProfilesSupported()) {
            xpackOptions.add(new Option("-profile",  runtime.compactProfile().toString()));
        }

        if (runtime.diskFootprintReduction() != null) {
            xpackOptions.add(new Option("-reduce-disk-footprint", runtime.diskFootprintReduction().toString()));
        }

        if (project.appType() != ApplicationType.TOMCAT) {
            for (ClasspathEntry classpathEntry : project.classpathEntries()) {
                Path depInBuildDir = project.toPathRelativeToJetBuildDir(classpathEntry);
                if (ClasspathEntry.PackType.NONE == classpathEntry.pack) {
                    if (classpathEntry.packagePath == null) {
                        if (classpathEntry.disableCopyToPackage != null && classpathEntry.disableCopyToPackage) {
                            xpackOptions.add(new Option("-disable-resource", exeName, depInBuildDir.getFileName().toString()));
                        } else {
                            xpackOptions.add(new Option("-add-file", depInBuildDir.toString(),
                                    classpathEntry.path.isDirectory() ? "/" : "/lib"
                            ));
                        }
                    } else {
                        xpackOptions.add(new Option("-add-file", depInBuildDir.toString(), classpathEntry.packagePath));
                        xpackOptions.add(new Option("-assign-resource", exeName, depInBuildDir.getFileName().toString(),
                                depInBuildDir.toString()));
                    }
                }

            }
        }

        return xpackOptions;
    }

    ArrayList<Option> getExcelsiorInstallerXPackOptions(File target) throws JetTaskFailureException {
        ArrayList<Option> xpackOptions = getCommonXPackOptions();

        boolean canInstallTomcatAsService = (project.appType() == ApplicationType.TOMCAT) &&
                                excelsiorJet.isWindowsServicesInExcelsiorInstallerSupported();
        if ((project.appType() == ApplicationType.WINDOWS_SERVICE) ||
                 canInstallTomcatAsService && project.tomcatConfiguration().installWindowsService)
        {
            addWindowsServiceArgs(xpackOptions);
        } else if (canInstallTomcatAsService && !project.tomcatConfiguration().installWindowsService) {
            xpackOptions.add(new Option("-no-tomcat-service-install"));
        }

        ExcelsiorInstallerConfig config = project.excelsiorInstallerConfiguration();
        if (config.eula.exists()) {
            xpackOptions.add(new Option(config.eulaFlag(), config.eula.getAbsolutePath()));
        }
        if (excelsiorJet.getTargetOS().isWindows() && config.installerSplash.exists()) {
            xpackOptions.add(new Option("-splash", config.installerSplash.getAbsolutePath()));
        }

        if (config.language != null) {
            xpackOptions.add(new Option("-language", config.language));
        }

        if (config.cleanupAfterUninstall) {
            xpackOptions.add(new Option("cleanup-after-uninstall"));
        }

        if (config.compressionLevel != null) {
            xpackOptions.add(new Option("-compression-level", config.compressionLevel));
        }

        xpackOptions.add(new Option("-backend", "excelsior-installer"));
        xpackOptions.add(new Option("-company", project.vendor()));
        xpackOptions.add(new Option("-product", project.product()));
        xpackOptions.add(new Option("-version", project.version()));
        xpackOptions.add(new Option("-target", target.getAbsolutePath()));
        return xpackOptions;
    }

    //Surprisingly Windows just removes empty argument from list of arguments if we do not pass "" instead.
    private static String escapeEmptyArgForWindows(String arg) {
        return arg.isEmpty() ? "\"\"" : arg;
    }

    private void addWindowsServiceArgs(ArrayList<Option> xpackOptions) {
        String exeName = excelsiorJet.getTargetOS().mangleExeName(project.outputName());
        if (project.appType() == ApplicationType.TOMCAT) {
            exeName = "bin/" + exeName;
        }
        WindowsServiceConfig serviceConfig = project.windowsServiceConfiguration();
        String serviceArguments = "";
        boolean validForRspFile = true;
        if (serviceConfig.arguments != null) {
            validForRspFile = Arrays.stream(serviceConfig.arguments).anyMatch(s -> s.contains(" "));
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
        xpackOptions.add(new Option("-service",
                validForRspFile,
                exeName,
                serviceArguments,
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

        xpackOptions.add(new Option("-service-startup",
                exeName,
                logOnType,
                serviceConfig.getStartupType().toXPackValue(),
                startAfterInstall
        ));

        if (serviceConfig.dependencies != null) {
            xpackOptions.add(new Option("-service-dependencies",
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

    private ArrayList<Option> getCommonXPackOptions(String targetDir) throws JetTaskFailureException {
        ArrayList<Option> xpackOptions = getCommonXPackOptions();
        xpackOptions.add(new Option("-target", targetDir));
        return xpackOptions;
    }

    private static ArrayList<String> optionsToArgs(ArrayList<Option> options) {
        ArrayList<String> args = new ArrayList<>();
        for (Option option: options) {
            args.add(option.option);
            args.addAll(Arrays.stream(option.parameters).map(
                    p -> Host.isWindows() && p.isEmpty()?escapeEmptyArgForWindows(p): p).collect(Collectors.toList()));
        }
        return args;
    }

    ArrayList<String> getExcelsiorInstallerXPackArgs(File target) throws JetTaskFailureException {
        return optionsToArgs(getExcelsiorInstallerXPackOptions(target));
    }

    ArrayList<String> getCommonXPackArgs(String targetDir) throws JetTaskFailureException {
        return optionsToArgs(getCommonXPackOptions(targetDir));
    }
}
