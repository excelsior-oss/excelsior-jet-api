package com.excelsiorjet.api;

import java.io.File;
import java.util.stream.Stream;

public interface AbstractJetTaskConfig {

    File tomcatHome();

    File tomcatInBuildDir();

    String warDeployName();

    File mainWar();

    AbstractLog log();

    String jetHome();

    String packaging();

    File mainJar();

    String mainClass();

    void setMainClass(String mainClass);

    TomcatConfig tomcatConfiguration();

    Stream<Artifact> getArtifacts();

    String groupId();

    File buildDir();

    String finalName();

    File basedir();

    File packageFilesDir();

    File execProfilesDir();

    String execProfilesName();

    String[] jvmArgs();

}
