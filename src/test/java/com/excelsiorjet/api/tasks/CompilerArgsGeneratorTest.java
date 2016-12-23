package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

import static com.excelsiorjet.api.tasks.Tests.excelsiorJet;
import static com.excelsiorjet.api.tasks.Tests.testProject;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompilerArgsGeneratorTest {

    private String linesToString(String... lines) {
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    @Test
    public void testMainJarClasspathEntry() throws Exception {
        JetProject prj = Tests.testProject(ApplicationType.PLAIN);
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));

    }

    @Test
    public void testDependencyPack() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.PLAIN).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).pack(ClasspathEntry.PackType.ALL).asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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
    public void testDependencyDefaultPackValue() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        JetProject prj = testProject(ApplicationType.PLAIN).
                projectDependencies(singletonList(DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
        String expectedPrjTail = linesToString(
                "!classpathentry test.jar",
                "  -optimize=all",
                "  -protect=all",
                "!end",
                "!classpathentry lib/dep.jar",
                "  -optimize=autodetect",
                "  -protect=nomatter",
                "!end");
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testPathPack() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depFileSpy).pack(ClasspathEntry.PackType.ALL).asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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
                projectDependencies(singletonList(dep1)).
                dependencies(asList(artSettings, groupSettings));
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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
                projectDependencies(asList(dep1, dep2)).
                dependencies(singletonList(groupSettings));
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
        String expectedPrj = linesToString("-apptype=tomcat",
                "-appdir=" + Tests.testBaseDir.resolve("prj/build/jet/build/tomcat-home").toString().replace(File.separatorChar, '/'),
                "-outputname=test",
                "-decor=ht",
                "-inline-",
                "%-jetvmprop=");

        assertEquals(expectedPrj, compilerArgsGenerator.projectFileContent());
    }

    @Test
    public void testExternalDirWithPackagePath() throws Exception {
        File depDirSpy = Tests.dirSpy(Tests.projectDir.resolve("extDir").toString());
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depDirSpy).packagePath("abc").asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet);
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet);
        String expectedPrjWinServiceSettings = linesToString(
                "-servicemain=HelloWorld",
                "-servicename=ServiceName"
        );
        assertTrue(compilerArgsGenerator.projectFileContent().contains(expectedPrjWinServiceSettings));
    }

    @Test
    public void testRuntimeKind() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        prj.runtimeConfiguration().kind = "desktop";
        prj.validate(excelsiorJet(), true);

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj, excelsiorJet());
        assertTrue(compilerArgsGenerator.projectFileContent().contains("-jetrt=desktop"));
    }
}
