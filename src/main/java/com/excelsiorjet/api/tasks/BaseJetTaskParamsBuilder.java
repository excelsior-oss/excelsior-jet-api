package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.TomcatConfig;

import java.io.File;
import java.util.stream.Stream;

public class BaseJetTaskParamsBuilder {

    private File mainWar;
    private String jetHome;
    private String packaging;
    private File mainJar;
    private String mainClass;
    private TomcatConfig tomcatConfiguration;
    private Stream<ClasspathEntry> dependencies;
    private String groupId;
    private File buildDir;
    private String finalName;
    private File basedir;
    private File packageFilesDir;
    private File execProfilesDir;
    private String execProfilesName;
    private String[] jvmArgs;

    public BaseJetTaskParamsBuilder setMainWar(File mainWar) {
        this.mainWar = mainWar;
        return this;
    }

    public BaseJetTaskParamsBuilder setJetHome(String jetHome) {
        this.jetHome = jetHome;
        return this;
    }

    public BaseJetTaskParamsBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public BaseJetTaskParamsBuilder setMainJar(File mainJar) {
        this.mainJar = mainJar;
        return this;
    }

    public BaseJetTaskParamsBuilder setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public BaseJetTaskParamsBuilder setTomcatConfiguration(TomcatConfig tomcatConfiguration) {
        this.tomcatConfiguration = tomcatConfiguration;
        return this;
    }

    public BaseJetTaskParamsBuilder setDependencies(Stream<ClasspathEntry> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public BaseJetTaskParamsBuilder setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public BaseJetTaskParamsBuilder setBuildDir(File buildDir) {
        this.buildDir = buildDir;
        return this;
    }

    public BaseJetTaskParamsBuilder setFinalName(String finalName) {
        this.finalName = finalName;
        return this;
    }

    public BaseJetTaskParamsBuilder setBasedir(File basedir) {
        this.basedir = basedir;
        return this;
    }

    public BaseJetTaskParamsBuilder setPackageFilesDir(File packageFilesDir) {
        this.packageFilesDir = packageFilesDir;
        return this;
    }

    public BaseJetTaskParamsBuilder setExecProfilesDir(File execProfilesDir) {
        this.execProfilesDir = execProfilesDir;
        return this;
    }

    public BaseJetTaskParamsBuilder setExecProfilesName(String execProfilesName) {
        this.execProfilesName = execProfilesName;
        return this;
    }

    public BaseJetTaskParamsBuilder setJvmArgs(String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public BaseJetTaskParams createAbstractJetTaskConfig() {
        return new BaseJetTaskParams(mainWar, jetHome, packaging, mainJar, mainClass, tomcatConfiguration, dependencies, groupId, buildDir, finalName, basedir, packageFilesDir, execProfilesDir, execProfilesName, jvmArgs);
    }
}