package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.log.StdOutLog;
import com.excelsiorjet.api.tasks.config.TomcatConfig;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import static java.util.Collections.emptyList;

class Tests {

    static final Path testBaseDir = Paths.get(System.getProperty("java.io.tmpdir"), "excelsior-jet-api-test");
    private static final Path mavenLocalDir = testBaseDir.resolve(".m2");
    static final Path projectDir = testBaseDir.resolve("prj");
    private static final Path buildDir = projectDir.resolve("build");
    static final Path mainJar = buildDir.resolve("test.jar");
    private static final Path jetDir = buildDir.resolve("jet");
    static final Path jetBuildDir = jetDir.resolve("build");
    static final Path externalJarRel = Paths.get("lib", "external.jar");
    static final Path externalJarAbs = projectDir.resolve(externalJarRel);

    static File dirSpy(String path) {
        File spy = Mockito.spy(new File(path));
        Mockito.when(spy.isFile()).thenReturn(false);
        Mockito.when(spy.isDirectory()).thenReturn(true);
        Mockito.when(spy.exists()).thenReturn(true);
        return spy;
    }

    static File dirSpy(Path path) {
        return dirSpy(path.toString());
    }

    static File fileSpy(String path) {
        File spy = Mockito.spy(new File(path));
        Mockito.when(spy.isFile()).thenReturn(true);
        Mockito.when(spy.isDirectory()).thenReturn(false);
        Mockito.when(spy.exists()).thenReturn(true);
        return spy;
    }

    static File fileSpy(Path path) {
        return fileSpy(path.toString());
    }

    static File mavenDepSpy(String depName) {
        return fileSpy(mavenLocalDir.resolve(depName).toString());
    }

    static JetProject testProject(ApplicationType appType) throws JetTaskFailureException {
        JetProject.configureEnvironment(new StdOutLog(), ResourceBundle.getBundle("Strings"));
        JetProject project = new JetProject("test", "prjGroup", "0.1", appType, buildDir.toFile(), new File("/jr")).
                splash(new File("splash")).
                icon(new File("icon")).
                inlineExpansion("tiny-methods-only").
                runArgs(new String[0]).
                projectDependencies(emptyList()).
                dependencies(emptyList()).
                mainClass("HelloWorld").
                jetBuildDir(jetBuildDir.toFile()).
                packageFilesDir(projectDir.resolve("src").resolve("jetresources").resolve("packageFiles").toFile()).
                outputName("test").
                excelsiorJetPackaging("none");
        if (appType == ApplicationType.PLAIN) {
            project.mainJar(fileSpy(mainJar.toString()));
        } else if (appType == ApplicationType.TOMCAT) {
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
        }
        return project;
    }

}
