package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.config.compiler.ExecProfilesConfig;
import com.excelsiorjet.api.tasks.config.dependencies.DependencySettings;
import com.excelsiorjet.api.tasks.config.dependencies.ProjectDependency;
import com.excelsiorjet.api.tasks.config.ApplicationType;
import com.excelsiorjet.api.tasks.config.dependencies.OptimizationPreset;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static com.excelsiorjet.api.tasks.Tests.excelsiorJet;
import static com.excelsiorjet.api.tasks.Tests.testProject;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompilerArgsGeneratorTest {

    private String linesToString(String... lines) {
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    @Test
    public void testMainJarClasspathEntry() throws Exception {
        JetProject prj = Tests.testProject(ApplicationType.PLAIN);
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));

    }

    @Test
    public void testDependencySmartPackAll() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.PLAIN).
                optimizationPreset(OptimizationPreset.SMART.toString()).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).pack(ClasspathEntry.PackType.ALL).asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry lib/dep.jar",
                "  -optimize=autodetect",
                "  -protect=nomatter",
                "  -pack=all",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testDependencyDefaultValues() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        JetProject prj = testProject(ApplicationType.PLAIN).
                projectDependencies(singletonList(DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry lib/dep.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testPathPack() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depFileSpy).pack(ClasspathEntry.PackType.ALL).asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry lib/dep.jar",
                "  -pack=all",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testThatSettingsWithArtifactOverridesGroupSettings() throws JetTaskFailureException {
        ProjectDependency dep1 = DependencyBuilder.testProjectDependency(Tests.mavenDepSpy("dep.jar")).asProjectDependency();
        DependencySettings groupSettings = DependencyBuilder.groupDependencySettings("groupId").pack(ClasspathEntry.PackType.ALL).asDependencySettings();
        DependencySettings artSettings = DependencyBuilder.artifactDependencySettings("artifactId").pack(ClasspathEntry.PackType.NONE).asDependencySettings();
        JetProject prj = testProject(ApplicationType.PLAIN).
                optimizationPreset(OptimizationPreset.SMART.toString()).
                projectDependencies(singletonList(dep1)).
                dependencies(asList(artSettings, groupSettings));
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry lib/dep.jar",
                "  -optimize=autodetect",
                "  -protect=nomatter",
                "  -pack=none",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testGroupSettings() throws JetTaskFailureException {
        ProjectDependency dep1 = DependencyBuilder.testProjectDependency(Tests.mavenDepSpy("dep1.jar")).artifactId("artifactId1").asProjectDependency();
        ProjectDependency dep2 = DependencyBuilder.testProjectDependency(Tests.mavenDepSpy("dep2.jar")).artifactId("artifactId2").asProjectDependency();
        DependencySettings groupSettings = DependencyBuilder.groupDependencySettings("groupId").pack(ClasspathEntry.PackType.ALL).asDependencySettings();
        JetProject prj = testProject(ApplicationType.PLAIN).
                optimizationPreset(OptimizationPreset.SMART.toString()).
                projectDependencies(asList(dep1, dep2)).
                dependencies(singletonList(groupSettings));
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry lib/dep1.jar",
                "  -optimize=autodetect",
                "  -protect=nomatter",
                "  -pack=all",
                "!end",
                "!classpathentry lib/dep2.jar",
                "  -optimize=autodetect",
                "  -protect=nomatter",
                "  -pack=all",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void noClasspathEntriesForTomcatApp() throws Exception {
        JetProject prj = testProject(ApplicationType.TOMCAT);
        prj.tomcatConfiguration().tomcatHome = "/tomcat-home";
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrj = linesToString("-apptype=tomcat",
                "-appdir=" + Tests.testBaseDir.resolve("prj/build/jet/build/tomcat-home").toString().replace(File.separatorChar, '/'),
                "-outputname=test",
                "-decor=ht",
                "-inline-",
                "%-jetvmprop=");

        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrj));
    }

    @Test
    public void testExternalDirWithPackagePath() throws Exception {
        File depDirSpy = Tests.dirSpy(Tests.projectDir.resolve("extDir").toString());
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depDirSpy).packagePath("abc").asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry abc/extDir",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testExternalDirWithoutPackagePath() throws Exception {
        File depDirSpy = Tests.dirSpy(Tests.projectDir.resolve("extDir").toString());
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depDirSpy).asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry extDir",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testTomcatDependencySettings() throws Exception {
        File depSpy = Tests.mavenDepSpy("dep.jar");
        DependencySettings dependencySettings = DependencyBuilder.testDependencySettings().protect(ClasspathEntry.ProtectionType.ALL).optimize(ClasspathEntry.OptimizationType.ALL).asDependencySettings();
        JetProject prj = Mockito.spy(testProject(ApplicationType.TOMCAT).
                projectDependencies(singletonList(DependencyBuilder.testProjectDependency(depSpy).version(dependencySettings.version).asProjectDependency())).
                dependencies(singletonList(dependencySettings)));
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String expectedPrjTail = linesToString(
                "%-jetvmprop=",
                "!classloaderentry webapp webapps/test:/WEB-INF/lib/dep.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testInvocationDll() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.DYNAMIC_LIBRARY).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        assertTrue(compilerArgsGenerator.projectFileContent().contains("gendll"));
    }

    @Test
    public void testWindowsService() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.WINDOWS_SERVICE).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet, false);
        String expectedPrjWinServiceSettings = linesToString(
                "-servicemain=HelloWorld",
                "-servicename=test"
        );
        assertTrue(compilerArgsGenerator.projectFileContent().contains(expectedPrjWinServiceSettings));
    }

    @Test
    public void testWindowsServiceName() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.WINDOWS_SERVICE).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        prj.windowsServiceConfiguration().name = "ServiceName";
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet, false);
        String expectedPrjWinServiceSettings = linesToString(
                "-servicemain=HelloWorld",
                "-servicename=ServiceName"
        );
        assertTrue(compilerArgsGenerator.projectFileContent().contains(expectedPrjWinServiceSettings));
    }

    @Test
    public void testRuntimeKind() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        prj.runtimeConfiguration().flavor = "desktop";
        prj.validate(excelsiorJet(), true);

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        assertTrue(compilerArgsGenerator.projectFileContent().contains("-jetrt=desktop"));
    }

    @Test
    public void testStackAlloc() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        prj.validate(excelsiorJet(), true);

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        assertFalse(compilerArgsGenerator.projectFileContent().contains("-genstackalloc"));

        prj = prj.stackAllocation(false);

        compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        assertTrue(compilerArgsGenerator.projectFileContent().contains("-genstackalloc-"));
    }

    @Test
    public void testPGO() throws JetTaskFailureException, IOException {
        JetProject prj = Mockito.spy(testProject(ApplicationType.PLAIN));
        ExecProfilesConfig execProfiles = Mockito.mock(ExecProfilesConfig.class);
        Mockito.doReturn(Tests.fileSpy("Test.jprof")).when(execProfiles).getJProfile();
        Mockito.doReturn(Tests.fileSpy("Test.usg")).when(execProfiles).getUsg();
        Mockito.when(prj.execProfiles()).thenReturn(execProfiles);
        prj.validate(excelsiorJet(), true);

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), false);
        String prjContent = compilerArgsGenerator.projectFileContent();
        assertTrue(prjContent.contains("-pgo+"));
        assertTrue(prjContent.contains("-jprofile="));
        assertTrue(prjContent.contains("Test.jprof"));

        compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet(), true);
        prjContent = compilerArgsGenerator.projectFileContent();
        assertFalse(prjContent.contains("-pgo+"));
        assertFalse(prjContent.contains("-jprofile="));
        assertTrue(prjContent.contains("-Djet.profiler"));
        assertTrue(prjContent.contains("-Djet.jprof.name"));
    }
}
