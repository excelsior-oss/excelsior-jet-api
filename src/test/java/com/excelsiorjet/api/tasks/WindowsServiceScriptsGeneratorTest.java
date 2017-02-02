package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.config.windowsservice.WindowsServiceConfig;
import com.excelsiorjet.api.tasks.config.windowsservice.LogOnType;
import com.excelsiorjet.api.tasks.config.windowsservice.StartupType;
import com.excelsiorjet.api.tasks.config.ApplicationType;
import org.junit.Before;
import org.junit.Test;

import static com.excelsiorjet.api.tasks.Tests.excelsiorJet;
import static com.excelsiorjet.api.tasks.Tests.testProject;
import static org.junit.Assert.assertArrayEquals;

public class WindowsServiceScriptsGeneratorTest {

    JetProject prj;
    ExcelsiorJet excelsiorJet;
    WindowsServiceScriptsGenerator scriptsGenerator;

    @Before
    public void setUp() throws JetTaskFailureException {
        prj = testProject(ApplicationType.WINDOWS_SERVICE);
        excelsiorJet = excelsiorJet();
        scriptsGenerator = new WindowsServiceScriptsGenerator(prj, excelsiorJet);
        WindowsServiceConfig winService = prj.windowsServiceConfiguration();
        winService.name = "testService";
        winService.displayName = "Test Service";
        winService.description = "Test Service Description";
        winService.fillDefaults(prj);
    }

    @Test
    public void testISrvArgs(){
        assertArrayEquals(
                new String[]{
                        "-install test.exe",
                        "-displayname \"Test Service\"",
                        "-description \"Test Service Description\"",
                        "-auto"
                },

                scriptsGenerator.isrvArgs().toArray()
        );
    }

    @Test
    public void testISrvArgsAdvanced() {
        WindowsServiceConfig winService = prj.windowsServiceConfiguration();
        winService.dependencies = new String[]{"dep1", "dep2"};
        winService.arguments = new String[]{"arg1", "arg2"};
        winService.allowDesktopInteraction = true;
        winService.startupType = StartupType.DISABLED.toString();
        assertArrayEquals(
                new String[]{
                        "-install test.exe",
                        "-displayname \"Test Service\"",
                        "-description \"Test Service Description\"",
                        "-disabled",
                        "-dependence dep1",
                        "-dependence dep2",
                        "-interactive",
                        "-args",
                        "arg1",
                        "arg2"
                },

                scriptsGenerator.isrvArgs().toArray()
        );
    }

    @Test
    public void testInstallBatSystemAccountStartAfter() {
        String rspFile = "test.rsp";
        assertArrayEquals(
                new String[] {
                        "@echo off",
                        "set servicename=testService",
                        "isrv @" + rspFile,
                        "if errorlevel 1 goto :failed",
                        "echo %servicename% service is successfully installed.",
                        "net start %servicename%",
                        "if errorlevel 1 goto :startfailed",
                        "goto :eof",
                        ":failed",
                        "echo %servicename% service installation failed (already installed?)",
                        "goto :eof",
                         ":startfailed",
                         "echo %servicename% service failed to start (need elevation to admin?)"
                        },
                scriptsGenerator.installBatFileContent(rspFile).toArray()
        );
    }

    @Test
    public void testInstallBatUserAccountNotStartAfter() {
        WindowsServiceConfig winService = prj.windowsServiceConfiguration();
        winService.startServiceAfterInstall = false;
        winService.logOnType = LogOnType.USER_ACCOUNT.toString();
        String rspFile = "test.rsp";
        assertArrayEquals(
                new String[] {
                        "@echo off",
                        "set servicename=testService",
                        "set /p name=\"Enter User (including domain prefix): \"",
                        "set \"psCommand=powershell -Command \"$pword = read-host 'Enter Password' -AsSecureString ; ^",
                        "$BSTR=[System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($pword); ^",
                        "[System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)\"\"",
                        "for /f \"usebackq delims=\" %%p in (`%psCommand%`) do set password=%%p",
                        "isrv @" + rspFile +  " -user %name% -password %password%",
                        "if errorlevel 1 goto :failed",
                        "echo %servicename% service is successfully installed.",
                        "goto :eof",
                        ":failed",
                        "echo %servicename% service installation failed (already installed or wrong user/password?)"
                        },
                scriptsGenerator.installBatFileContent(rspFile).toArray()
        );
    }

    @Test
    public void testUninstallBat() {
        assertArrayEquals(
                new String[] {
                        "@echo off",
                        "set servicename=testService",
                        "isrv -r test.exe",
                        "if errorlevel 1 goto :failed",
                        "echo %servicename% service is successfully removed.",
                        "goto :eof",
                        ":failed",
                        "echo %servicename% service uninstallation failed."
                },
                scriptsGenerator.uninstallBatFileContent().toArray()
        );
    }
}
