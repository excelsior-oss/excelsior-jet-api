package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import com.excelsiorjet.api.util.Utils;
import org.junit.Test;

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
}
