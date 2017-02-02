package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.dependencies.DependencySettings;
import com.excelsiorjet.api.tasks.config.dependencies.ProjectDependency;

import java.io.File;

public class DependencyBuilder {

    private static int nextVersion = 0;

    private String groupId;
    private String artifactId;
    private String version;
    private File path;
    private ClasspathEntry.ProtectionType protect;
    private ClasspathEntry.OptimizationType optimize;
    private ClasspathEntry.PackType pack;
    private String packagePath;
    private Boolean isLibrary;
    private Boolean disableCopyToPackage;

    public static DependencyBuilder testProjectDependency(File path) {
        DependencyBuilder dependencyBuilder = new DependencyBuilder();
        dependencyBuilder.groupId = "groupId";
        dependencyBuilder.artifactId = "artifactId";
        dependencyBuilder.version = String.valueOf(nextVersion++);
        dependencyBuilder.path = path;
        return dependencyBuilder;
    }

    public static DependencyBuilder testDependencySettings() {
        DependencyBuilder dependencyBuilder = new DependencyBuilder();
        dependencyBuilder.groupId = "groupId";
        dependencyBuilder.artifactId = "artifactId";
        dependencyBuilder.version = String.valueOf(nextVersion++);
        return dependencyBuilder;
    }

    public static DependencyBuilder testExternalDependency(File path) {
        DependencyBuilder dependencyBuilder = new DependencyBuilder();
        dependencyBuilder.path = path;
        return dependencyBuilder;
    }

    public static DependencyBuilder groupDependencySettings(String groupId) {
        DependencyBuilder dependencyBuilder = new DependencyBuilder();
        dependencyBuilder.groupId = groupId;
        return dependencyBuilder;
    }

    public static DependencyBuilder artifactDependencySettings(String artifactId) {
        DependencyBuilder dependencyBuilder = new DependencyBuilder();
        dependencyBuilder.artifactId = artifactId;
        return dependencyBuilder;
    }

    public static DependencyBuilder empty() {
        return new DependencyBuilder();
    }

    public DependencyBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public DependencyBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public DependencyBuilder version(String version) {
        this.version = version;
        return this;
    }

    public DependencyBuilder pack(ClasspathEntry.PackType pack) {
        this.pack = pack;
        return this;
    }

    public DependencyBuilder path(File path) {
        this.path = path;
        return this;
    }

    public DependencyBuilder packagePath(String packagePath) {
        this.packagePath = packagePath;
        return this;
    }

    public DependencyBuilder isLib(boolean isLib) {
        this.isLibrary = isLib;
        return this;
    }

    public DependencyBuilder disableCopyToPackage(boolean disableCopyToPackage) {
        this.disableCopyToPackage = disableCopyToPackage;
        return this;
    }

    public DependencyBuilder protect(ClasspathEntry.ProtectionType protect) {
        this.protect = protect;
        return this;
    }

    public DependencyBuilder optimize(ClasspathEntry.OptimizationType optimize) {
        this.optimize = optimize;
        return this;
    }

    public DependencySettings asDependencySettings() {
        DependencySettings dependencySettings = new DependencySettings(groupId, artifactId, version, path);
        dependencySettings.protect = this.protect == null ? null : this.protect.userValue;
        dependencySettings.optimize = this.optimize == null ? null : this.optimize.userValue;
        dependencySettings.pack = this.pack == null ? null : this.pack.userValue;
        dependencySettings.isLibrary = this.isLibrary;
        dependencySettings.packagePath = this.packagePath;
        dependencySettings.disableCopyToPackage = this.disableCopyToPackage;
        return dependencySettings;
    }

    public ProjectDependency asProjectDependency() {
        return new ProjectDependency(groupId, artifactId, version, path, false);
    }
}
