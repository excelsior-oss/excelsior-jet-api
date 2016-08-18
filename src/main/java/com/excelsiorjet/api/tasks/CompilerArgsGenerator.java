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

import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.util.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Generator of Excelsior JET's project files.
 *
 * @author Aleksey Zhidkov
 */
class CompilerArgsGenerator {

    private final JetProject project;

    CompilerArgsGenerator(JetProject project) {
        this.project = project;
    }

    String projectFileContent() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);

        compilerArgs().forEach(out::println);
        if (project.compilerOptions() != null) {
            for (String option : project.compilerOptions()) {
                out.println(option);
            }
        }

        for (ClasspathEntry dep : project.classpathEntries()) {
            if (project.appType() == ApplicationType.PLAIN) {
                out.println("!classpathentry " + project.toPathRelativeToJetBuildDir(dep).toString());
            } else if (project.appType() == ApplicationType.TOMCAT){
                String warDeployName = project.tomcatConfiguration().warDeployName;
                String entryPath = ":/WEB-INF/";
                if (dep.isMainArtifact) {
                    entryPath += "classes";
                } else {
                    entryPath += "lib/" + dep.path.getName();
                }
                out.println("!classloaderentry webapp webapps/" + warDeployName.substring(0, warDeployName.lastIndexOf(".war")) + entryPath);
            }
            if (dep.optimize != null) {
                out.println("  -optimize=" + dep.optimize.jetValue);
            }
            if (dep.protect != null) {
                out.println("  -protect=" + dep.protect.jetValue);
            }
            if (dep.pack != null) {
                out.println("  -pack=" + dep.pack.jetValue);
            }
            out.println("!end");
        }

        for (String mod : modules()) {
            out.println("!module " + mod);
        }

        return stringWriter.toString();
    }

    private List<String> modules() {
        ArrayList<String> modules = new ArrayList<>();

        if (Utils.isWindows()) {
            if (project.icon().isFile()) {
                modules.add(project.icon().getAbsolutePath());
            }
        }

        TestRunExecProfiles execProfiles = new TestRunExecProfiles(project.execProfilesDir(), project.execProfilesName());
        if (execProfiles.getUsg().exists()) {
            modules.add(execProfiles.getUsg().getAbsolutePath());
        }

        return modules;
    }

    private List<String> compilerArgs() {
        ArrayList<String> compilerArgs = new ArrayList<>();

        switch (project.appType()) {
            case PLAIN:
                compilerArgs.add("-main=" + project.mainClass());

                if (project.splash().isFile()) {
                    compilerArgs.add("-splash=" + project.splash().getAbsolutePath());
                } else {
                    compilerArgs.add("-splashgetfrommanifest+");
                }

                break;
            case TOMCAT:
                compilerArgs.add("-apptype=tomcat");
                compilerArgs.add("-appdir=" + project.tomcatInBuildDir());
                if (project.tomcatConfiguration().hideConfig) {
                    compilerArgs.add("-hideconfiguration+");
                }
                if (!project.tomcatConfiguration().genScripts) {
                    compilerArgs.add("-gentomcatscripts-");
                }
                break;
            default:
                throw new AssertionError("Unknown app type");
        }


        if (Utils.isWindows()) {
            if (project.hideConsole()) {
                compilerArgs.add("-gui+");
            }
        }

        compilerArgs.add("-outputname=" + project.outputName());
        compilerArgs.add("-decor=ht");

        if (project.profileStartup()) {
            compilerArgs.add("-saprofmode=ALWAYS");
            compilerArgs.add("-saproftimeout=" + project.profileStartupTimeout());
        }

        if (project.isAddWindowsVersionInfo()) {
            compilerArgs.add("-versioninfocompanyname=" + project.vendor());
            compilerArgs.add("-versioninfoproductname=" + project.product());
            compilerArgs.add("-versioninfoproductversion=" + project.winVIVersion());
            compilerArgs.add("-versioninfolegalcopyright=" + project.winVICopyright());
            compilerArgs.add("-versioninfofiledescription=" + project.winVIDescription());
        }

        if (project.multiApp()) {
            compilerArgs.add("-multiapp+");
        }

        if (project.globalOptimizer()) {
            compilerArgs.add("-global+");
        }

        if (project.trialVersion() != null) {
            compilerArgs.add("-expire=" + project.trialVersion().getExpire());
            compilerArgs.add("-expiremsg=" + project.trialVersion().expireMessage);
        }

        if (project.protectData()) {
            compilerArgs.add("-cryptseed=" + project.cryptSeed());
        }

        TestRunExecProfiles execProfiles = new TestRunExecProfiles(project.execProfilesDir(), project.execProfilesName());
        if (execProfiles.getStartup().exists()) {
            compilerArgs.add("-startupprofile=" + execProfiles.getStartup().getAbsolutePath());
        }

        switch (project.inlineExpansion()) {
            case TINY_METHODS_ONLY:
                compilerArgs.add("-inline-");
                break;
            case LOW:
                compilerArgs.add("-inlinelimit=50");
                compilerArgs.add("-inlinetolimit=250");
                break;
            case MEDIUM:
                compilerArgs.add("-inlinelimit=100");
                compilerArgs.add("-inlinetolimit=500");
                break;
            case VERY_AGGRESSIVE:
                compilerArgs.add("-inlinelimit=250");
                compilerArgs.add("-inlinetolimit=2000");
            case AGGRESSIVE:
                //use default
                break;
            default:
                throw new AssertionError("Unknown inline expansion type: " + project.inlineExpansion());
        }

        if (project.runArgs().length > 0) {
            String quotedArgs = Arrays.stream(project.runArgs())
                    .map(Utils::quoteCmdLineArgument)
                    .collect(joining(" "));
            compilerArgs.add("-runarguments=" + quotedArgs);
        }

        if (project.stackTraceSupport() == StackTraceSupportType.FULL) {
            compilerArgs.add("-genstacktrace+");

        }

        // JVM args may contain $(Root) prefix for system property value
        // (that should expand to installation directory location).
        // However JET compiler replaces such occurrences with s value of "Root" equation if the "$(Root)" is
        // used in the project file.
        // So we need to pass jetvmprop as separate compiler argument as workaround.
        // We also write the equation in commented form to the project in order to see it in the technical support.
        compilerArgs.add("%" + jetVMPropOpt());

        return compilerArgs;
    }

    private List<String> jvmArgs() {
        List<String> jvmArgs = project.jvmArgs() != null ? new ArrayList<>(Arrays.asList(project.jvmArgs())) : new ArrayList<>();
        if (project.stackTraceSupport() == StackTraceSupportType.NONE) {
            jvmArgs.add("-Djet.stack.trace=false");
        }
        return jvmArgs;
    }

    String jetVMPropOpt() {
        return "-jetvmprop=" + String.join(" ", jvmArgs());
    }
}
