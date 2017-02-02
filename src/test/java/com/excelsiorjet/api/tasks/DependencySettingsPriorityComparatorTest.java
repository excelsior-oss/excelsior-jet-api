package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.dependencies.DependencySettings;
import org.junit.Test;

import static org.junit.Assert.*;

public class DependencySettingsPriorityComparatorTest {

    public DependencySettings group = DependencyBuilder.empty().groupId("groupId").asDependencySettings();
    public DependencySettings artifact = DependencyBuilder.empty().artifactId("artifactId").asDependencySettings();
    public DependencySettings groupArtifact = DependencyBuilder.empty().groupId("groupId").artifactId("artifactId").asDependencySettings();
    public DependencySettings artifactVersion = DependencyBuilder.empty().artifactId("artifactId").version("version").asDependencySettings();
    public DependencySettings groupVersion = DependencyBuilder.empty().groupId("groupId").version("version").asDependencySettings();
    public DependencySettings groupArtifactVersion = DependencyBuilder.empty().groupId("groupId").artifactId("artifactId").version("version").asDependencySettings();

    public DependencySettingsPriorityComparator cmp = new DependencySettingsPriorityComparator();

    // -a- - g--
    @Test
    public void testArtifactIsGreaterThanGroup() throws Exception {
        assertEquals(1, cmp.compare(artifact, group));
    }

    @Test
    public void testGroupIsLessThanArtifact() throws Exception {
        assertEquals(-1, cmp.compare(group, artifact));
    }

    // ga- - g--
    @Test
    public void testGroupArtifactIsGreaterThanGroup() throws Exception {
        assertEquals(1, cmp.compare(groupArtifact, group));
    }

    @Test
    public void testGroupIsLessThanGroupArtifact() throws Exception {
        assertEquals(-1, cmp.compare(group, groupArtifact));
    }

    // -av - g--
    @Test
    public void testArtifactVersionIsGreaterThanGroup() throws Exception {
        assertEquals(1, cmp.compare(artifactVersion, group));
    }

    @Test
    public void testGroupIsLessThanArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(group, artifactVersion));
    }

    // g-v - g--
    @Test
    public void testGroupVersionIsGreaterThanGroup() throws Exception {
        assertEquals(1, cmp.compare(groupVersion, group));
    }

    @Test
    public void testGroupIsLessThanGroupVersion() throws Exception {
        assertEquals(-1, cmp.compare(group, groupVersion));
    }

    // gav - g--
    @Test
    public void testGroupArtifactVersionIsGreaterThanGroup() throws Exception {
        assertEquals(1, cmp.compare(groupArtifactVersion, group));
    }

    @Test
    public void testGroupIsLessThanGroupArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(group, groupArtifactVersion));
    }

    // ga- - -a-
    @Test
    public void testGroupArtifactIsGreaterThanArtifact() throws Exception {
        assertEquals(1, cmp.compare(groupArtifact, artifact));
    }

    @Test
    public void testArtifactIsLessThanGroupArtifact() throws Exception {
        assertEquals(-1, cmp.compare(artifact, groupArtifact));
    }

    // -av - -a-
    @Test
    public void testArtifactVersionIsGreaterThanArtifact() throws Exception {
        assertEquals(1, cmp.compare(artifactVersion, artifact));
    }

    @Test
    public void testArtifactIsLessThanArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(artifact, artifactVersion));
    }

    // -a- - g-v
    @Test
    public void testArtifactIsGreaterThanGroupVersion() throws Exception {
        assertEquals(1, cmp.compare(artifact, groupVersion));
    }

    @Test
    public void testGroupVersionIsLessThanArtifact() throws Exception {
        assertEquals(-1, cmp.compare(groupVersion, artifact));
    }

    // gav - -a-
    @Test
    public void testGroupArtifactVersionIsGreaterThanArtifact() throws Exception {
        assertEquals(1, cmp.compare(groupArtifactVersion, artifact));
    }

    @Test
    public void testArtifactIsLessThanGroupArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(artifact, groupArtifactVersion));
    }

    // -av - ga-
    @Test
    public void testArtifactVersionIsGreaterThanGroupArtifact() throws Exception {
        assertEquals(1, cmp.compare(artifactVersion, groupArtifact));
    }

    @Test
    public void testGroupArtifactIsLessThanArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(groupArtifact, artifactVersion));
    }

    // ga- - g-v
    @Test
    public void testGroupArtifactIsGreaterThanGroupVersion() throws Exception {
        assertEquals(1, cmp.compare(groupArtifact, groupVersion));
    }

    @Test
    public void testGroupVersionIsLessThanGroupArtifact() throws Exception {
        assertEquals(-1, cmp.compare(groupVersion, groupArtifact));
    }

    // gav - ga-
    @Test
    public void testGroupArtifactVersionIsGreaterThanGroupArtifact() throws Exception {
        assertEquals(1, cmp.compare(groupArtifactVersion, groupArtifact));
    }

    @Test
    public void testGroupArtifactIsLessThanGroupArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(groupArtifact, groupArtifactVersion));

    }

    // -av - g-v
    @Test
    public void testArtifactVersionIsGreaterThanGroupVersion() throws Exception {
        assertEquals(1, cmp.compare(artifactVersion, groupVersion));
    }

    @Test
    public void testGroupVersionIsLessThanArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(groupVersion, artifactVersion));
    }

    // gav - -av
    @Test
    public void testGroupArtifactVersionIsGreaterThanArtifactVersion() throws Exception {
        assertEquals(1, cmp.compare(groupArtifactVersion, artifactVersion));
    }

    @Test
    public void testArtifactVersionIsLessThanGroupArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(artifactVersion, groupArtifactVersion));
    }

    // gav - g-v
    @Test
    public void testGroupArtifactVersionIsGreaterThanGroupVersion() throws Exception {
        assertEquals(1, cmp.compare(groupArtifactVersion, groupVersion));
    }

    @Test
    public void testGroupVersionIsLessThanGroupArtifactVersion() throws Exception {
        assertEquals(-1, cmp.compare(groupVersion, groupArtifactVersion));

    }
}
