package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static com.excelsiorjet.api.tasks.JetBuildTaskTest.mockUtilsClass;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
public class JetProjectTest {

    @Test
    public void testExternalDirectoryNotPackedValidation() throws Exception {
        File extDepDirSpy = Tests.dirSpy(Tests.projectDir.resolve("externalDir").toString());
        Mockito.when(extDepDirSpy.isDirectory()).thenReturn(true);

        JetProject project = Tests.testProject(ApplicationType.PLAIN).
                dependencies(singletonList(DependencyBuilder.testExternalDependency(extDepDirSpy).pack(ClasspathEntry.PackType.ALL).asDependencySettings()));

        try {
            project.validate(Mockito.spy(new ExcelsiorJet(null, null)), false);
            fail("JetTaskFailureException expected");
        } catch (JetTaskFailureException e) {
            assertEquals(Txt.s("JetApi.NotPackedExternalDirectory", "/tmp/excelsior-jet-api-test/prj/externalDir"), e.getMessage());
        }
    }

    @Test
    @PrepareForTest(value = {Utils.class})
    public void mainJarAddedToDependencies() throws Exception {
        mockUtilsClass();
        JetProject project = Tests.testProject(ApplicationType.PLAIN);
        project.processDependencies();

        List<ClasspathEntry> deps = project.copyClasspathEntries();
        assertEquals(1, deps.size());
        assertEquals(Tests.mainJar.toFile(), deps.get(0).path);
    }

    @Test
    public void testAmbiguousDependencySettingsValidation() throws Exception {
        DependencySettings dependencySettings = DependencyBuilder.empty().artifactId("artifactId").asDependencySettings();
        ProjectDependency dep1 = DependencyBuilder.testProjectDependency(null).groupId("groupId1").asProjectDependency();
        ProjectDependency dep2 = DependencyBuilder.testProjectDependency(null).groupId("groupId2").asProjectDependency();
        JetProject project = Tests.testProject(ApplicationType.PLAIN).
                projectDependencies(asList(dep1, dep2)).
                dependencies(singletonList(dependencySettings));

        try {
            project.validate(Mockito.spy(new ExcelsiorJet(null, null)), false);
            fail("JetTaskFailureException expected");
        } catch (JetTaskFailureException e) {
            List<String> dependencyIds = Stream.of(dep1, dep2).
                    map(ProjectDependency::idStr).
                    collect(toList());
            assertEquals(Txt.s("JetApi.AmbiguousArtifactIdOnlyDependencySettings", dependencySettings.idStr(), String.join(", ", dependencyIds)), e.getMessage());
        }
    }

    @Test
    public void testNullDependencySettingsId() throws Exception {
        DependencySettings dependencySettings = DependencyBuilder.empty().asDependencySettings();
        JetProject project = Tests.testProject(ApplicationType.PLAIN).
                dependencies(singletonList(dependencySettings));

        try {
            project.validate(Mockito.spy(new ExcelsiorJet(null, null)), false);
            fail("JetTaskFailureException expected");
        } catch (JetTaskFailureException e) {
            assertEquals(Txt.s("JetApi.DependencyIdRequired"), e.getMessage());
        }
    }

    @Test
    public void testExternalDependencyWithArtifactId() throws Exception {
        DependencySettings externalDependency = DependencyBuilder.testExternalDependency(Tests.fileSpy(Tests.externalJarAbs)).artifactId("artifactId").asDependencySettings();
        JetProject project = Tests.testProject(ApplicationType.PLAIN).
                dependencies(singletonList(externalDependency));

        try {
            project.validate(Mockito.spy(new ExcelsiorJet(null, null)), false);
            fail("JetTaskFailureException expected");
        } catch (JetTaskFailureException e) {
            assertEquals(Txt.s("JetApi.InvalidDependencySetting", "(artifactId)"), e.getMessage());
        }
    }

    @Test
    public void testExternalDependencyWithGroupId() throws Exception {
        DependencySettings externalDependency = DependencyBuilder.testExternalDependency(Tests.fileSpy(Tests.externalJarAbs)).groupId("groupId").asDependencySettings();
        JetProject project = Tests.testProject(ApplicationType.PLAIN).
                dependencies(singletonList(externalDependency));

        try {
            project.validate(Mockito.spy(new ExcelsiorJet(null, null)), false);
            fail("JetTaskFailureException expected");
        } catch (JetTaskFailureException e) {
            assertEquals(Txt.s("JetApi.InvalidDependencySetting", "(groupId)"), e.getMessage());
        }
    }

    @Test
    public void testExternalDependencyWithVersion() throws Exception {
        DependencySettings externalDependency = DependencyBuilder.testExternalDependency(Tests.fileSpy(Tests.externalJarAbs)).version("version").asDependencySettings();
        JetProject project = Tests.testProject(ApplicationType.PLAIN).
                dependencies(singletonList(externalDependency));

        try {
            project.validate(Mockito.spy(new ExcelsiorJet(null, null)), false);
            fail("JetTaskFailureException expected");
        } catch (JetTaskFailureException e) {
            assertEquals(Txt.s("JetApi.InvalidDependencySetting", "(version)"), e.getMessage());
        }
    }
}
