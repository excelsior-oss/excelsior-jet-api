package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.AbstractJetTaskConfig;
import com.excelsiorjet.api.tasks.config.TomcatConfig;

import java.io.File;
import java.util.stream.Stream;

public class BaseJetTaskParams {
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

    public BaseJetTaskParams setMainWar(File mainWar) {
        this.mainWar = mainWar;
        return this;
    }

    public BaseJetTaskParams setJetHome(String jetHome) {
        this.jetHome = jetHome;
        return this;
    }

    public BaseJetTaskParams setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public BaseJetTaskParams setMainJar(File mainJar) {
        this.mainJar = mainJar;
        return this;
    }

    public BaseJetTaskParams setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public BaseJetTaskParams setTomcatConfiguration(TomcatConfig tomcatConfiguration) {
        this.tomcatConfiguration = tomcatConfiguration;
        return this;
    }

    public BaseJetTaskParams setDependencies(Stream<ClasspathEntry> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public BaseJetTaskParams setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public BaseJetTaskParams setBuildDir(File buildDir) {
        this.buildDir = buildDir;
        return this;
    }

    public BaseJetTaskParams setFinalName(String finalName) {
        this.finalName = finalName;
        return this;
    }

    public BaseJetTaskParams setBasedir(File basedir) {
        this.basedir = basedir;
        return this;
    }

    public BaseJetTaskParams setPackageFilesDir(File packageFilesDir) {
        this.packageFilesDir = packageFilesDir;
        return this;
    }

    public BaseJetTaskParams setExecProfilesDir(File execProfilesDir) {
        this.execProfilesDir = execProfilesDir;
        return this;
    }

    public BaseJetTaskParams setExecProfilesName(String execProfilesName) {
        this.execProfilesName = execProfilesName;
        return this;
    }

    public BaseJetTaskParams setJvmArgs(String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public AbstractJetTaskConfig createAbstractJetTaskConfig() {
        return new AbstractJetTaskConfig(mainWar, jetHome, packaging, mainJar, mainClass, tomcatConfiguration, dependencies, groupId, buildDir, finalName, basedir, packageFilesDir, execProfilesDir, execProfilesName, jvmArgs);
    }
}