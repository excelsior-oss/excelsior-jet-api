package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

import static com.excelsiorjet.api.tasks.Tests.testProject;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompilerArgsGeneratorTest {

    @Test
    public void testMainJarClasspathEntry() throws Exception {
        JetProject prj = Tests.testProject(ApplicationType.PLAIN);
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n";
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
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n" +
                "!classpathentry lib/dep.jar\n" +
                "  -optimize=autodetect\n" +
                "  -protect=nomatter\n" +
                "  -pack=all\n" +
                "!end\n";
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testDependencyDefaultPackValue() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        JetProject prj = testProject(ApplicationType.PLAIN).
                projectDependencies(singletonList(DependencyBuilder.testProjectDependency(depFileSpy).asProjectDependency()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n" +
                "!classpathentry lib/dep.jar\n" +
                "  -optimize=autodetect\n" +
                "  -protect=nomatter\n" +
                "!end\n";
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testPathPack() throws Exception {
        File depFileSpy = Tests.mavenDepSpy("dep.jar");
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depFileSpy).pack(ClasspathEntry.PackType.ALL).asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n" +
                "!classpathentry lib/dep.jar\n" +
                "  -pack=all\n" +
                "!end\n";
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n" +
                "!classpathentry lib/dep.jar\n" +
                "  -optimize=autodetect\n" +
                "  -protect=nomatter\n" +
                "  -pack=none\n" +
                "!end\n";
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n" +
                "!classpathentry lib/dep1.jar\n" +
                "  -optimize=autodetect\n" +
                "  -protect=nomatter\n" +
                "  -pack=all\n" +
                "!end\n" +
                "!classpathentry lib/dep2.jar\n" +
                "  -optimize=autodetect\n" +
                "  -protect=nomatter\n" +
                "  -pack=all\n" +
                "!end\n";
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void noClasspathEntriesForTomcatApp() throws Exception {
        JetProject prj = testProject(ApplicationType.TOMCAT);
        prj.tomcatConfiguration().tomcatHome = "/tomcat-home";
        prj.processDependencies();

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrj = "-apptype=tomcat\n" +
                "-appdir=/tmp/excelsior-jet-api-test/prj/build/jet/build/tomcat-home\n" +
                "-outputname=test\n" +
                "-decor=ht\n" +
                "-inline-\n" +
                "%-jetvmprop=\n";

        assertEquals(expectedPrj, compilerArgsGenerator.projectFileContent());
    }

    @Test
    public void testExternalDirWithPackagePath() throws Exception {
        File depDirSpy = Tests.dirSpy(Tests.projectDir.resolve("extDir").toString());
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depDirSpy).packagePath("abc").asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n" +
                "!classpathentry abc/extDir\n" +
                "!end\n";
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

    @Test
    public void testExternalDirWithoutPackagePath() throws Exception {
        File depDirSpy = Tests.dirSpy(Tests.projectDir.resolve("extDir").toString());
        JetProject prj = testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(depDirSpy).asDependencySettings()));
        prj.processDependencies();
        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "!classpathentry test.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n" +
                "!classpathentry extDir\n" +
                "!end\n";
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

        CompilerArgsGenerator compilerArgsGenerator = new CompilerArgsGenerator(prj);
        String expectedPrjTail =
                "%-jetvmprop=\n" +
                "!classloaderentry webapp webapps/test:/WEB-INF/lib/dep.jar\n" +
                "  -optimize=all\n" +
                "  -protect=all\n" +
                "!end\n";
        assertTrue(compilerArgsGenerator.projectFileContent().endsWith(expectedPrjTail));
    }

}
