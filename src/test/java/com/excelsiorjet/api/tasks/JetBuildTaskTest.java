package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.log.StdOutLog;
import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import com.excelsiorjet.api.util.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class JetBuildTaskTest {

    static {
        JetProject.configureEnvironment(new StdOutLog(), ResourceBundle.getBundle("Strings"));
    }

    private final ArgumentCaptor<Path> fromCaptor = ArgumentCaptor.forClass(Path.class);
    private final ArgumentCaptor<Path> toCaptor = ArgumentCaptor.forClass(Path.class);

    @Test
    @PrepareForTest(value = {Utils.class})
    public void testMainJarCopied() throws Exception {
        prepareJetBuildDir();
        mockCopying("copyFile");
        File mainJarSpy = Tests.fileSpy(Tests.mainJar);
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);

        JetProject prj = Mockito.spy(Tests.testProject(ApplicationType.PLAIN).
                mainJar(mainJarSpy));
        prj.processDependencies();
        Mockito.doNothing().when(prj).validate(excelsiorJet, true);
        Mockito.when(excelsiorJet.compile(Tests.jetBuildDir.toFile(), "=p", "test.prj", "-jetvmprop=")).thenReturn(0);

        JetBuildTask buildTask = Mockito.spy(new JetBuildTask(excelsiorJet, prj));
        buildTask.execute();

        Mockito.verify(excelsiorJet).compile(Tests.jetBuildDir.toFile(), "=p", "test.prj", "-jetvmprop=");

        Path from = fromCaptor.getValue();
        Path to = toCaptor.getValue();
        assertEquals(Tests.mainJar.toString(), from.toFile().getAbsolutePath());
        assertEquals(Tests.jetBuildDir.resolve("test.jar").toString(), to.toFile().getAbsolutePath());
    }

    @Test
    @PrepareForTest(value = {Utils.class})
    public void testExternalJarCopied() throws Exception {
        mockCopying("copyFile");

        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        File externalJar = Tests.fileSpy(Tests.externalJarAbs);
        File mainJarSpy = Tests.fileSpy(Tests.mainJar);

        JetProject prj = Mockito.spy(Tests.testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(externalJar).pack(ClasspathEntry.PackType.ALL).asDependencySettings())).
                mainJar(mainJarSpy));
        prj.processDependencies();
        Mockito.doNothing().when(prj).validate(excelsiorJet, true);
        Mockito.when(excelsiorJet.compile(null, "=p", "test.prj", "-jetvmprop=")).thenReturn(0);

        JetBuildTask buildTask = new JetBuildTask(excelsiorJet, prj);
        buildTask.execute();

        Mockito.verify(excelsiorJet).compile(Tests.jetBuildDir.toFile(), "=p", "test.prj", "-jetvmprop=");

        Path externalFrom = fromCaptor.getAllValues().get(1);
        Path externalTo = toCaptor.getAllValues().get(1);
        assertEquals(Tests.externalJarAbs.toString(), externalFrom.toFile().getAbsolutePath());
        assertEquals(Tests.jetBuildDir.resolve(Tests.externalJarRel).toString(), externalTo.toFile().getAbsolutePath());
    }

    @Test
    @PrepareForTest(value = {Utils.class})
    public void testMainJarNotCopiedForTomcatApp() throws Exception {
        prepareJetBuildDir();
        mockUtilsClass();

        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);

        JetProject prj = Mockito.spy(Tests.testProject(ApplicationType.TOMCAT));
        prj.processDependencies();
        prj.tomcatConfiguration().tomcatHome = "/tomcat-home";
        Mockito.doNothing().when(prj).validate(excelsiorJet, true);
        Mockito.when(excelsiorJet.compile(null, "=p", "test.prj", "-jetvmprop=")).thenReturn(0);

        JetBuildTask buildTask = new JetBuildTask(excelsiorJet, prj);
        buildTask.execute();

        PowerMockito.verifyStatic(Mockito.never());
        Utils.copy(anyObject(), anyObject());
    }

    @Test
    @PrepareForTest(value = {Utils.class})
    public void testCopyDependencyToPathInPackage() throws Exception {
        mockCopying("copyFile");
        ArgumentCaptor<File> mkdirCaptor = ArgumentCaptor.forClass(File.class);
        PowerMockito.doNothing().when(Utils.class, "mkdir", mkdirCaptor.capture());

        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        File externalJar = Tests.fileSpy(Tests.externalJarAbs);
        ProjectDependency dep = DependencyBuilder.testProjectDependency(externalJar).asProjectDependency();
        DependencySettings depDesc = DependencyBuilder.testDependencySettings().pack(ClasspathEntry.PackType.NONE).version(dep.version).packagePath("extDep").asDependencySettings();
        File mainJarSpy = Tests.fileSpy(Tests.mainJar);
        JetProject prj = Mockito.spy(Tests.testProject(ApplicationType.PLAIN).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(depDesc)).
                mainJar(mainJarSpy));
        prj.processDependencies();
        Mockito.doNothing().when(prj).validate(excelsiorJet, true);
        Mockito.when(excelsiorJet.compile(null, "=p", "test.prj", "-jetvmprop=")).thenReturn(0);

        JetBuildTask buildTask = new JetBuildTask(excelsiorJet, prj);
        buildTask.execute();

        Mockito.verify(excelsiorJet).compile(Tests.jetBuildDir.toFile(), "=p", "test.prj", "-jetvmprop=");

        Path externalFrom = fromCaptor.getAllValues().get(1);
        Path externalTo = toCaptor.getAllValues().get(1);
        assertEquals(Tests.externalJarAbs.toString(), externalFrom.toFile().getAbsolutePath());
        assertEquals(Tests.jetBuildDir.resolve("extDep").resolve("external.jar"), externalTo);

        assertEquals(Tests.jetBuildDir.resolve("extDep").toString(), mkdirCaptor.getAllValues().get(2).getAbsolutePath());
    }

    @Test
    @PrepareForTest(value = {Utils.class})
    public void testExternalDirCopied() throws Exception {
        mockCopying("copyDirectory");

        Path externalDirRel = Paths.get("lib", "externalDir");
        Path externalDirAbs = Tests.projectDir.resolve(externalDirRel);

        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        File externalDir = Tests.dirSpy(externalDirAbs);
        File mainJarSpy = Tests.fileSpy(Tests.mainJar);
        JetProject prj = Mockito.spy(Tests.testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(externalDir).pack(ClasspathEntry.PackType.NONE).asDependencySettings())).
                mainJar(mainJarSpy));
        prj.processDependencies();
        Mockito.doNothing().when(prj).validate(excelsiorJet, true);
        Mockito.when(excelsiorJet.compile(null, "=p", "test.prj", "-jetvmprop=")).thenReturn(0);

        JetBuildTask buildTask = new JetBuildTask(excelsiorJet, prj);
        buildTask.execute();

        Mockito.verify(excelsiorJet).compile(Tests.jetBuildDir.toFile(), "=p", "test.prj", "-jetvmprop=");

        Path externalFrom = fromCaptor.getAllValues().get(0);
        Path externalTo = toCaptor.getAllValues().get(0);
        assertEquals(externalDirAbs, externalFrom);
        assertEquals(Tests.jetBuildDir.resolve("externalDir"), externalTo);
    }

    @Test
    @PrepareForTest(value = {Utils.class})
    public void testExternalDisabledJarCopiedToLib() throws Exception {
        mockCopying("copyFile");

        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        File externalJar = Tests.fileSpy(Tests.externalJarAbs);
        DependencySettings dep = DependencyBuilder.testExternalDependency(externalJar).pack(ClasspathEntry.PackType.NONE).disableCopyToPackage(true).asDependencySettings();
        JetProject prj = Mockito.spy(Tests.testProject(ApplicationType.PLAIN).
                mainJar(Tests.fileSpy(Tests.mainJar)).
                dependencies(singletonList(dep)));
        prj.processDependencies();
        Mockito.doNothing().when(prj).validate(excelsiorJet, true);
        Mockito.when(excelsiorJet.compile(null, "=p", "test.prj", "-jetvmprop=")).thenReturn(0);

        JetBuildTask buildTask = new JetBuildTask(excelsiorJet, prj);
        buildTask.execute();

        Mockito.verify(excelsiorJet).compile(Tests.jetBuildDir.toFile(), "=p", "test.prj", "-jetvmprop=");

        Path externalFrom = fromCaptor.getAllValues().get(1);
        Path externalTo = toCaptor.getAllValues().get(1);
        assertEquals(Tests.externalJarAbs, externalFrom);
        assertEquals(Tests.jetBuildDir.resolve(Tests.externalJarRel), externalTo);
    }

    private void mockCopying(String method) throws Exception {
        mockUtilsClass();
        PowerMockito.doNothing().when(Utils.class, method, fromCaptor.capture(), toCaptor.capture());
    }


    static void mockUtilsClass() throws Exception {
        PowerMockito.mockStatic(Utils.class, invocationOnMock -> {
            if (invocationOnMock.getMethod().getName().equals("parameterToEnumConstantName") ||
                    invocationOnMock.getMethod().getName().equals("idStr") ||
                    invocationOnMock.getMethod().getName().equals("getCanonicalPath"))
            {
                return invocationOnMock.callRealMethod();
            } else {
                return null;
            }
        });
        when(Utils.class, "isEmpty", anyObject()).thenReturn(true);
    }

    private void prepareJetBuildDir() throws IOException {
        Utils.cleanDirectory(Tests.jetBuildDir.toFile());
        Tests.jetBuildDir.toFile().mkdirs();
    }

}