package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;

import static com.excelsiorjet.api.tasks.Tests.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PackagerArgsGeneratorTest {

    private String toPlatform(String path) {
        return path.replace('/', File.separatorChar);
    }

    @Test
    public void testAddFileForNotPacketArtifactWithoutPackagePath() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.PLAIN).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).pack(ClasspathEntry.PackType.NONE).asDependencySettings()));
        prj.processDependencies();
        ExcelsiorJet excelsiorJet = excelsiorJet();
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<String> xPackArgs = packagerArgsGenerator.getCommonXPackArgs();

        int addExeIdx = xPackArgs.indexOf("-add-file");
        assertTrue(addExeIdx >= 0);
        assertEquals(excelsiorJet.getTargetOS().mangleExeName("test"), xPackArgs.get(addExeIdx + 1));
        assertEquals("/", xPackArgs.get(addExeIdx + 2));

        int addLibIdx = xPackArgs.lastIndexOf("-add-file");
        assertTrue(addLibIdx > addExeIdx);
        assertEquals(toPlatform("lib/test.jar"), xPackArgs.get(addLibIdx + 1));
        assertEquals("/lib", xPackArgs.get(addLibIdx + 2));
    }

    @Test
    public void testAddFileForNotPacketExternalDependencyWithoutPackagePath() throws Exception {
        File extDepJarSpy = fileSpy(externalJarAbs);
        DependencySettings extDep = DependencyBuilder.testExternalDependency(extDepJarSpy).pack(ClasspathEntry.PackType.NONE).asDependencySettings();
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(extDep));
        prj.processDependencies();
        ExcelsiorJet excelsiorJet = excelsiorJet();
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<String> xPackArgs = packagerArgsGenerator.getCommonXPackArgs();

        int addExeIdx = xPackArgs.indexOf("-add-file");
        assertTrue(addExeIdx >= 0);
        assertEquals(excelsiorJet.getTargetOS().mangleExeName("test"), xPackArgs.get(addExeIdx + 1));
        assertEquals("/", xPackArgs.get(addExeIdx + 2));

        int addLibIdx = xPackArgs.lastIndexOf("-add-file");
        assertTrue(addLibIdx > addExeIdx);
        assertEquals(externalJarRel.toString(), xPackArgs.get(addLibIdx + 1));
        assertEquals("/lib", xPackArgs.get(addLibIdx + 2));
    }

    @Test
    public void testAssignResourceForNotPackedArtifactWithPackagePath() throws Exception {
        ProjectDependency dep = DependencyBuilder.testProjectDependency(new File("/.m2/test.jar")).asProjectDependency();
        JetProject prj = testProject(ApplicationType.PLAIN).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().pack(ClasspathEntry.PackType.NONE).version(dep.version).packagePath("extDep").asDependencySettings()));
        prj.processDependencies();
        ExcelsiorJet excelsiorJet = excelsiorJet();
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<String> xPackArgs = packagerArgsGenerator.getCommonXPackArgs();

        int addExeIdx = xPackArgs.indexOf("-add-file");
        assertTrue(addExeIdx >= 0);
        assertEquals(excelsiorJet.getTargetOS().mangleExeName("test"), xPackArgs.get(addExeIdx + 1));
        assertEquals("/", xPackArgs.get(addExeIdx + 2));

        int addLibIdx = xPackArgs.lastIndexOf("-add-file");
        assertTrue(addLibIdx > addExeIdx);
        assertEquals(toPlatform("extDep/test.jar"), xPackArgs.get(addLibIdx + 1));
        assertEquals("extDep", xPackArgs.get(addLibIdx + 2));

        assertEquals("-assign-resource", xPackArgs.get(addLibIdx + 3));
        assertEquals(excelsiorJet.getTargetOS().mangleExeName("test"), xPackArgs.get(addLibIdx + 4));
        assertEquals("test.jar", xPackArgs.get(addLibIdx + 5));
        assertEquals(toPlatform("extDep/test.jar"), xPackArgs.get(addLibIdx + 6));
    }

    @Test
    public void testDisableResource() throws Exception {
        File extDepJarSpy = fileSpy(externalJarAbs);
        DependencySettings extDep = DependencyBuilder.testExternalDependency(extDepJarSpy).pack(ClasspathEntry.PackType.NONE).disableCopyToPackage(true).asDependencySettings();
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(extDep));
        prj.processDependencies();
        ExcelsiorJet excelsiorJet = excelsiorJet();
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<String> xPackArgs = packagerArgsGenerator.getCommonXPackArgs();

        int addExeIdx = xPackArgs.indexOf("-add-file");
        assertTrue(addExeIdx >= 0);
        assertEquals(excelsiorJet.getTargetOS().mangleExeName("test"), xPackArgs.get(addExeIdx + 1));
        assertEquals("/", xPackArgs.get(addExeIdx + 2));

        int disableResourceIdx = xPackArgs.lastIndexOf("-disable-resource");
        assertTrue(disableResourceIdx > addExeIdx);
        assertEquals(excelsiorJet.getTargetOS().mangleExeName("test"), xPackArgs.get(disableResourceIdx + 1));
        assertEquals("external.jar", xPackArgs.get(disableResourceIdx + 2));
    }

    @Test
    public void testInvocationDll() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.DYNAMIC_LIBRARY).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<String> xPackArgs = packagerArgsGenerator.getCommonXPackArgs();

        int addExeIdx = xPackArgs.indexOf("-add-file");
        assertTrue(addExeIdx >= 0);
        assertEquals(excelsiorJet.getTargetOS().mangleDllName("test"), xPackArgs.get(addExeIdx + 1));
        assertEquals("/", xPackArgs.get(addExeIdx + 2));
    }

    @Test
    public void testWindowsService() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.WINDOWS_SERVICE).
                excelsiorJetPackaging("excelsior-installer").
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.windowsServiceConfiguration().dependencies = new String[]{"dep1", "dep 2"};
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);


        ArrayList<String> xPackArgs = packagerArgsGenerator.getExcelsiorInstallerXPackArgs(new File("target.exe"));

        String exeName = excelsiorJet.getTargetOS().mangleExeName("test");
        int addExeIdx = xPackArgs.indexOf("-add-file");
        assertTrue(addExeIdx >= 0);
        assertEquals(exeName, xPackArgs.get(addExeIdx + 1));
        assertEquals("/", xPackArgs.get(addExeIdx + 2));

        int serviceIdx = xPackArgs.lastIndexOf("-service");
        assertTrue(serviceIdx > addExeIdx);
        assertEquals(exeName, xPackArgs.get(serviceIdx + 1));
        assertEquals("\"\"", xPackArgs.get(serviceIdx + 2));
        assertEquals("test", xPackArgs.get(serviceIdx + 3));
        assertEquals("test", xPackArgs.get(serviceIdx + 4));

        int serviceStartupIdx = xPackArgs.lastIndexOf("-service-startup");
        assertTrue(serviceStartupIdx > serviceIdx);
        assertEquals(exeName, xPackArgs.get(serviceStartupIdx + 1));
        assertEquals("system", xPackArgs.get(serviceStartupIdx + 2));
        assertEquals("auto", xPackArgs.get(serviceStartupIdx + 3));
        assertEquals("start-after-install", xPackArgs.get(serviceStartupIdx + 4));

        int dependenciesIdx = xPackArgs.lastIndexOf("-service-dependencies");
        assertTrue(dependenciesIdx > serviceStartupIdx);
        assertEquals(exeName, xPackArgs.get(dependenciesIdx + 1));
        assertEquals("dep1,dep 2", xPackArgs.get(dependenciesIdx + 2));
    }

    @Test
    public void testTomcatWindowsService() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.TOMCAT).
                excelsiorJetPackaging("excelsior-installer").
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<String> xPackArgs = packagerArgsGenerator.getExcelsiorInstallerXPackArgs(new File("target.exe"));

        String exeName = "bin/" + excelsiorJet.getTargetOS().mangleExeName("test");

        int serviceIdx = xPackArgs.lastIndexOf("-service");
        assertEquals(exeName, xPackArgs.get(serviceIdx + 1));
        assertEquals("\"\"", xPackArgs.get(serviceIdx + 2));
        assertEquals("Apache Tomcat", xPackArgs.get(serviceIdx + 3));
        assertEquals("Apache Tomcat Server - http://tomcat.apache.org/", xPackArgs.get(serviceIdx + 4));

        int serviceStartupIdx = xPackArgs.lastIndexOf("-service-startup");
        assertTrue(serviceStartupIdx > serviceIdx);
        assertEquals(exeName, xPackArgs.get(serviceStartupIdx + 1));
        assertEquals("system", xPackArgs.get(serviceStartupIdx + 2));
        assertEquals("auto", xPackArgs.get(serviceStartupIdx + 3));
        assertEquals("start-after-install", xPackArgs.get(serviceStartupIdx + 4));
    }

    @Test
    public void testCompactProfile() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN).compactProfile("compact3");
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<String> xPackArgs = packagerArgsGenerator.getCommonXPackArgs();

        int profileIdx = xPackArgs.indexOf("-profile");
        assertTrue(profileIdx >= 0);
        assertEquals("compact3", xPackArgs.get(profileIdx + 1));

    }

    @Test
    public void testDiskFootprintReduction() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN).globalOptimizer(true).diskFootprintReduction("high-memory");
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.execProfilesDir(prj.jetResourcesDir()).execProfilesName("test");
        TestRunExecProfiles testRunExecProfiles = Mockito.mock(TestRunExecProfiles.class);
        Mockito.doReturn(fileSpy("test.usg")).when(testRunExecProfiles).getUsg();
        prj = Mockito.spy(prj);
        Mockito.when(prj.testRunExecProfiles()).thenReturn(testRunExecProfiles);
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<String> xPackArgs = packagerArgsGenerator.getCommonXPackArgs();

        int profileIdx = xPackArgs.indexOf("-reduce-disk-footprint");
        assertTrue(profileIdx >= 0);
        assertEquals("high-memory", xPackArgs.get(profileIdx + 1));
    }
}
