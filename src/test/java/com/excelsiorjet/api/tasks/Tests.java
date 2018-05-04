package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.JetEdition;
import com.excelsiorjet.api.log.StdOutLog;
import com.excelsiorjet.api.platform.OS;
import com.excelsiorjet.api.tasks.config.*;
import com.excelsiorjet.api.tasks.config.ApplicationType;
import com.excelsiorjet.api.tasks.config.compiler.ExecProfilesConfig;
import com.excelsiorjet.api.tasks.config.compiler.WindowsVersionInfoConfig;
import com.excelsiorjet.api.tasks.config.runtime.RuntimeConfig;
import com.excelsiorjet.api.tasks.config.runtime.RuntimeFlavorType;
import com.excelsiorjet.api.tasks.config.excelsiorinstaller.ExcelsiorInstallerConfig;
import com.excelsiorjet.api.tasks.config.windowsservice.WindowsServiceConfig;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.ResourceBundle;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Tests {

    static final Path testBaseDir = Paths.get(System.getProperty("java.io.tmpdir"), "excelsior-jet-api-test");
    private static final Path mavenLocalDir = testBaseDir.resolve(".m2");
    static final Path projectDir = testBaseDir.resolve("prj");
    private static final Path buildDir = projectDir.resolve("build");
    static final Path mainJar = buildDir.resolve("test.jar");
    private static final Path jetDir = buildDir.resolve("jet");
    static final Path jetBuildDir = jetDir.resolve("build");
    static final Path jetAppDir = jetDir.resolve("app");
    static final Path externalJarRel = Paths.get("lib", "external.jar");
    static final Path externalJarAbs = projectDir.resolve(externalJarRel);

    public static File dirSpy(String path) {
        File spy = Mockito.spy(new File(path));
        Mockito.when(spy.isFile()).thenReturn(false);
        Mockito.when(spy.isDirectory()).thenReturn(true);
        Mockito.when(spy.exists()).thenReturn(true);
        return spy;
    }

    static File dirSpy(Path path) {
        return dirSpy(path.toString());
    }

    public static File fileSpy(String path) {
        File spy = Mockito.spy(new File(path));
        Mockito.when(spy.isFile()).thenReturn(true);
        Mockito.when(spy.isDirectory()).thenReturn(false);
        Mockito.when(spy.exists()).thenReturn(true);
        return spy;
    }

    static File fileSpy(Path path) {
        return fileSpy(path.toString());
    }

    public static File fileSpy(String path, long modifyTime) {
        File spy = fileSpy(path);
        Mockito.when(spy.lastModified()).thenReturn(modifyTime);
        return spy;
    }

    static File mavenDepSpy(String depName) {
        return fileSpy(mavenLocalDir.resolve(depName).toString());
    }

    public static JetProject testProject(ApplicationType appType) throws JetTaskFailureException {
        JetProject.configureEnvironment(new StdOutLog(), ResourceBundle.getBundle("Strings"));
        JetProject project = new JetProject("test", "prjGroup", "0.1", appType, buildDir.toFile(), new File("/jr")).
                inlineExpansion("tiny-methods-only").
                runArgs(new String[0]).
                addWindowsVersionInfo(false).
                stackAllocation(true).
                projectDependencies(emptyList()).
                dependencies(emptyList()).
                mainClass("HelloWorld").
                jetBuildDir(jetBuildDir.toFile()).
                jetAppDir(jetAppDir.toFile()).
                packageFiles(Collections.emptyList()).
                excelsiorInstallerConfiguration(new ExcelsiorInstallerConfig()).
                windowsServiceConfiguration(new WindowsServiceConfig()).
                windowsVersionInfoConfiguration(new WindowsVersionInfoConfig()).
                runtimeConfiguration(new RuntimeConfig()).
                execProfiles(new ExecProfilesConfig()).
                outputName("test").
                stackTraceSupport("minimal").
                excelsiorJetPackaging("none").
                pdbConfiguration(new PDBConfig());
        switch (appType) {
            case PLAIN:
            case DYNAMIC_LIBRARY:
            case WINDOWS_SERVICE:
                project.mainJar(fileSpy(mainJar.toString()));
                break;
            case TOMCAT:
                project.tomcatConfiguration(new TomcatConfig()).
                        mainWar(fileSpy(buildDir.resolve("test.war")));
                //create fake tomcat dir
                Path tomcatHome = testBaseDir.resolve("tomcat");
                Path webapps = tomcatHome.resolve("webapps");
                webapps.toFile().mkdirs();
                webapps.toFile().deleteOnExit();
                tomcatHome.toFile().deleteOnExit();
                project.tomcatConfiguration().tomcatHome = tomcatHome.toString();
                project.tomcatConfiguration().warDeployName = "test.war";
                break;
            default:
                throw new AssertionError("Unknown app type");
        }
        //some tests disable validation where this default is set. TODO: refactor it
        project.runtimeConfiguration().profile = "auto";
        project.pdbConfiguration().keepInBuildDir = true;
        return project;
    }

    static ExcelsiorJet excelsiorJet() {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(OS.WINDOWS).when(excelsiorJet).getTargetOS();
        Mockito.doReturn(JetEdition.ENTERPRISE).when(excelsiorJet).getEdition();
        Mockito.doReturn(true).when(excelsiorJet).isTomcatSupported();
        Mockito.doReturn(true).when(excelsiorJet).isWindowsServicesSupported();
        Mockito.doReturn(true).when(excelsiorJet).isExcelsiorInstallerSupported();
        Mockito.doReturn(true).when(excelsiorJet).isWindowsServicesInExcelsiorInstallerSupported();
        Mockito.doReturn(true).when(excelsiorJet).isCompactProfilesSupported();
        Mockito.doReturn(true).when(excelsiorJet).isGlobalOptimizerSupported();
        Mockito.doReturn(true).when(excelsiorJet).isDiskFootprintReductionSupported();
        Mockito.doReturn(true).when(excelsiorJet).isHighDiskFootprintReductionSupported();
        Mockito.doReturn(true).when(excelsiorJet).isRuntimeSupported(RuntimeFlavorType.DESKTOP);
        Mockito.doReturn(true).when(excelsiorJet).isChangeRTLocationAvailable();
        Mockito.doReturn(true).when(excelsiorJet).since11_3();
        Mockito.doReturn(true).when(excelsiorJet).isAdvancedExcelsiorInstallerFeaturesSupported();
        Mockito.doReturn(true).when(excelsiorJet).isPGOSupported();
        return excelsiorJet;
    }

    public interface Validation {
        void validate() throws JetTaskFailureException;
    }

    public static void assertNotThrows(Validation body) {
        try {
            body.validate();
        } catch (JetTaskFailureException e) {
            fail("should be valid");
        }
    }

    public static void assertThrows(Validation body, String message) {
        try {
            body.validate();
            fail("should not be valid");
        } catch (JetTaskFailureException e) {
            assertEquals(message, e.getMessage());
        }
    }

}
