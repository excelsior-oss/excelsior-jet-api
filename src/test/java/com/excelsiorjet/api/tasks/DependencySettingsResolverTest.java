package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.dependencies.DependencySettings;
import com.excelsiorjet.api.tasks.config.dependencies.ProjectDependency;
import com.excelsiorjet.api.tasks.config.dependencies.OptimizationPreset;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class DependencySettingsResolverTest {

    @Test
    public void dependencyResolvesToItSelfWhenNoSettingsFound() throws Exception {
        DependencySettings dep = DependencyBuilder.testProjectDependency(new File("/dep")).asDependencySettings();
        ClasspathEntry resolvedDep = new ClasspathEntry(dep, false);
        assertEquals(new File("/dep"), resolvedDep.path);
        assertNull(resolvedDep.protect);
        assertNull(resolvedDep.optimize);
        assertNull(resolvedDep.pack);
        assertNull(resolvedDep.packagePath);
        assertNull(resolvedDep.disableCopyToPackage);
    }

    @Test
    public void testDefaultsForLibTypicalPreset() throws Exception {
        DependencySettings settings = DependencyBuilder.testDependencySettings().isLib(true).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.TYPICAL, "prjGroupId", Collections.singletonList(settings));
        ProjectDependency dep = DependencyBuilder.testProjectDependency(new File("/dep")).asProjectDependency();
        ClasspathEntry resolvedDep = dependencySettingsResolver.resolve(dep);

        assertEquals(ClasspathEntry.ProtectionType.ALL, resolvedDep.protect);
        assertEquals(ClasspathEntry.OptimizationType.ALL, resolvedDep.optimize);
    }

    @Test
    public void testDefaultsForLibSmartPreset() throws Exception {
        DependencySettings settings = DependencyBuilder.testDependencySettings().isLib(true).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.SMART, "prjGroupId", Collections.singletonList(settings));
        ProjectDependency dep = DependencyBuilder.testProjectDependency(new File("/dep")).asProjectDependency();
        ClasspathEntry resolvedDep = dependencySettingsResolver.resolve(dep);

        assertEquals(ClasspathEntry.ProtectionType.NOT_REQUIRED, resolvedDep.protect);
        assertEquals(ClasspathEntry.OptimizationType.AUTO_DETECT, resolvedDep.optimize);
    }

    @Test
    public void testDefaultsForNonLibTypicalPreset() throws Exception {
        DependencySettings settings = DependencyBuilder.testDependencySettings().isLib(false).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.TYPICAL, "groupId", Collections.singletonList(settings));
        ProjectDependency dep = DependencyBuilder.testProjectDependency(new File("/dep")).asProjectDependency();
        ClasspathEntry resolvedDep = dependencySettingsResolver.resolve(dep);

        assertEquals(ClasspathEntry.ProtectionType.ALL, resolvedDep.protect);
        assertEquals(ClasspathEntry.OptimizationType.ALL, resolvedDep.optimize);
    }

    @Test
    public void testDefaultsForNonLibSmartPreset() throws Exception {
        DependencySettings settings = DependencyBuilder.testDependencySettings().isLib(false).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.SMART, "groupId", Collections.singletonList(settings));
        ProjectDependency dep = DependencyBuilder.testProjectDependency(new File("/dep")).asProjectDependency();
        ClasspathEntry resolvedDep = dependencySettingsResolver.resolve(dep);

        assertEquals(ClasspathEntry.ProtectionType.ALL, resolvedDep.protect);
        assertEquals(ClasspathEntry.OptimizationType.ALL, resolvedDep.optimize);
    }

    @Test
    public void testGroupSettings() throws Exception {
        DependencySettings settings = DependencyBuilder.groupDependencySettings("groupId").protect(ClasspathEntry.ProtectionType.ALL).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.SMART, "prjGroupId", Collections.singletonList(settings));
        ProjectDependency dep1 = DependencyBuilder.testProjectDependency(new File("/dep1")).asProjectDependency();
        ProjectDependency dep2 = DependencyBuilder.testProjectDependency(new File("/dep2")).asProjectDependency();

        ClasspathEntry resolvedDep1 = dependencySettingsResolver.resolve(dep1);
        ClasspathEntry resolvedDep2 = dependencySettingsResolver.resolve(dep2);
        assertEquals(ClasspathEntry.ProtectionType.ALL, resolvedDep1.protect);
        assertEquals(ClasspathEntry.ProtectionType.ALL, resolvedDep2.protect);
    }

    @Test
    public void testThatArtifactSettingsOverwritesGroupSettings() throws Exception {
        DependencySettings groupSettings = DependencyBuilder.groupDependencySettings("groupId").protect(ClasspathEntry.ProtectionType.ALL).asDependencySettings();
        DependencySettings artifactSettings = DependencyBuilder.artifactDependencySettings("artifact1").protect(ClasspathEntry.ProtectionType.NOT_REQUIRED).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.SMART, "prjGroupId", asList(groupSettings, artifactSettings));
        ProjectDependency dep1 = DependencyBuilder.testProjectDependency(new File("/dep1")).artifactId("artifact1").asProjectDependency();
        ProjectDependency dep2 = DependencyBuilder.testProjectDependency(new File("/dep2")).artifactId("artifact2").asProjectDependency();

        ClasspathEntry resolvedDep1 = dependencySettingsResolver.resolve(dep1);
        ClasspathEntry resolvedDep2 = dependencySettingsResolver.resolve(dep2);
        assertEquals(ClasspathEntry.ProtectionType.NOT_REQUIRED, resolvedDep1.protect);
        assertEquals(ClasspathEntry.ProtectionType.ALL, resolvedDep2.protect);
    }

    @Test
    public void testThatGroupWithVersionSettingsOverwritesGroupSettings() throws Exception {
        DependencySettings groupSettings = DependencyBuilder.groupDependencySettings("groupId").protect(ClasspathEntry.ProtectionType.ALL).asDependencySettings();
        DependencySettings groupWithVersionSettings = DependencyBuilder.groupDependencySettings("groupId").version("version1").protect(ClasspathEntry.ProtectionType.NOT_REQUIRED).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.SMART, "prjGroupId", asList(groupSettings, groupWithVersionSettings));
        ProjectDependency dep1 = DependencyBuilder.testProjectDependency(new File("/dep1")).artifactId("artifact1").version("version1").asProjectDependency();
        ProjectDependency dep2 = DependencyBuilder.testProjectDependency(new File("/dep2")).artifactId("artifact2").asProjectDependency();

        ClasspathEntry resolvedDep1 = dependencySettingsResolver.resolve(dep1);
        ClasspathEntry resolvedDep2 = dependencySettingsResolver.resolve(dep2);
        assertEquals(ClasspathEntry.ProtectionType.NOT_REQUIRED, resolvedDep1.protect);
        assertEquals(ClasspathEntry.ProtectionType.ALL, resolvedDep2.protect);
    }

    @Test
    public void testPathOnlyProjectDependencyMatchesDependencySettings() throws Exception {
        DependencySettings settings = DependencyBuilder.testExternalDependency(Tests.externalJarAbs.toFile()).isLib(true).asDependencySettings();
        DependencySettingsResolver dependencySettingsResolver = new DependencySettingsResolver(OptimizationPreset.SMART, "prjGroupId", Collections.singletonList(settings));
        ProjectDependency dep = DependencyBuilder.testProjectDependency(Tests.externalJarAbs.toFile()).asProjectDependency();
        ClasspathEntry resolvedDep = dependencySettingsResolver.resolve(dep);

        assertEquals(ClasspathEntry.ProtectionType.NOT_REQUIRED, resolvedDep.protect);
        assertEquals(ClasspathEntry.OptimizationType.AUTO_DETECT, resolvedDep.optimize);
    }

}