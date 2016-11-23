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
import com.excelsiorjet.api.tasks.config.WindowsServiceConfig.LogOnType;
import com.excelsiorjet.api.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generates contents for Windows Service install.bat/uninstall.bat scripts.
 *
 * @author Nikita Lipsky
 */
public class WindowsServiceScriptsGenerator {

    private final JetProject project;
    private final ExcelsiorJet excelsiorJet;

    public WindowsServiceScriptsGenerator(JetProject project, ExcelsiorJet excelsiorJet) {
        this.project = project;
        this.excelsiorJet = excelsiorJet;
    }

    public List<String> isrvArgs() {
        String exeFile = excelsiorJet.getTargetOS().mangleExeName(project.outputName());
        ArrayList<String> args = new ArrayList<>(Arrays.asList(new String[] {
                "-install " + exeFile,
                "-displayname " + Utils.quoteCmdLineArgument(project.windowsServiceConfiguration().displayName),
                "-description " + Utils.quoteCmdLineArgument(project.windowsServiceConfiguration().description),
                project.windowsServiceConfiguration().getStartupType().toISrvCmdFlag()
        }));
        if (project.windowsServiceConfiguration().dependencies != null) {
            for (String dependency: project.windowsServiceConfiguration().dependencies) {
                args.add("-dependence " + Utils.quoteCmdLineArgument(dependency));
            }
        }
        if (project.windowsServiceConfiguration().allowDesktopInteraction) {
            args.add("-interactive");
        }
        if (project.windowsServiceConfiguration().arguments != null) {
            args.add("-args");
            Collections.addAll(args, project.windowsServiceConfiguration().arguments);
        }
        return args;
    }
    
    public List<String> installBatFileContent(String rspFile) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("@echo off");
        lines.add("set servicename=" + project.windowsServiceConfiguration().name);
        LogOnType logOnType = project.windowsServiceConfiguration().getLogOnType();
        switch (logOnType) {
            case LOCAL_SYSTEM_ACCOUNT:
                lines.add("isrv @" + rspFile);
                break;
            case USER_ACCOUNT:
                //little bit magic for prompting user/password for service installation
                lines.add("set /p name=\"Enter User (including domain prefix): \"");
                lines.add("set \"psCommand=powershell -Command \"$pword = read-host 'Enter Password' -AsSecureString ; ^");
                lines.add("$BSTR=[System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($pword); ^");
                lines.add("[System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)\"\"");
                lines.add("for /f \"usebackq delims=\" %%p in (`%psCommand%`) do set password=%%p");

                lines.add("isrv @" + rspFile +  " -user %name% -password %password%");
                break;
            default:
                throw new AssertionError("Unknown logOnType: " + logOnType);
        }
        lines.add("if errorlevel 1 goto :failed");
        lines.add("echo %servicename% service is successfully installed.");
        if (project.windowsServiceConfiguration().startServiceAfterInstall) {
            lines.add("net start %servicename%");
            lines.add("if errorlevel 1 goto :startfailed");
        }
        lines.add("goto :eof");
        lines.add(":failed");
        lines.add("echo %servicename% service installation failed (already installed" +
                (logOnType == LogOnType.USER_ACCOUNT ? " or wrong user/password?)" : "?)"));
        if (project.windowsServiceConfiguration().startServiceAfterInstall) {
            lines.add("goto :eof");
            lines.add(":startfailed");
            lines.add("echo %servicename% service failed to start (need elevation to admin?)");
        }
        return lines;
    }
    
    public List<String> uninstallBatFileContent() {
        String exeFile = excelsiorJet.getTargetOS().mangleExeName(project.outputName());
        ArrayList<String> lines = new ArrayList<>();
        lines.add("@echo off");
        lines.add("set servicename=" + project.windowsServiceConfiguration().name);
        lines.add("isrv -r " + exeFile);
        lines.add("if errorlevel 1 goto :failed");
        lines.add("echo %servicename% service is successfully removed.");
        lines.add("goto :eof");
        lines.add(":failed");
        lines.add("echo %servicename% service uninstallation failed.");
        return lines;
    }
}
