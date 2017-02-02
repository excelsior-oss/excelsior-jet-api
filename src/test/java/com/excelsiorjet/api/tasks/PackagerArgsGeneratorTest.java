package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.tasks.config.*;
import com.excelsiorjet.api.tasks.config.dependencies.DependencySettings;
import com.excelsiorjet.api.tasks.config.dependencies.ProjectDependency;
import com.excelsiorjet.api.tasks.config.ApplicationType;
import com.excelsiorjet.api.tasks.config.excelsiorinstaller.ExcelsiorInstallerConfig;
import com.excelsiorjet.api.tasks.config.excelsiorinstaller.FileAssociation;
import com.excelsiorjet.api.tasks.config.excelsiorinstaller.PostInstallCheckbox;
import com.excelsiorjet.api.tasks.config.excelsiorinstaller.Shortcut;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.*;

import static com.excelsiorjet.api.tasks.Tests.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class PackagerArgsGeneratorTest {

    private String toPlatform(String path) {
        return path.replace('/', File.separatorChar);
    }

    private int assertOptionsContain(List<XPackOption> options, String option, String... parameters) {
        XPackOption xpackOption = new XPackOption(option, parameters);
        int idx = options.indexOf(xpackOption);
        if (idx < 0) {
            Optional<XPackOption> optCandidate = options.stream().filter(o -> o.option.equals(option)).findFirst();
            if (!optCandidate.isPresent()) {
                fail("Option " + option + " is not present");
            } else {
                //will fail with suitable message
                assertArrayEquals(parameters, optCandidate.get().parameters);
            }
        }
        return idx;
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

        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        int addExeIdx =
                assertOptionsContain(xPackOptions, "-add-file", excelsiorJet.getTargetOS().mangleExeName("test"), "/");
        int addLibIdx =
                assertOptionsContain(xPackOptions, "-add-file", toPlatform("lib/test.jar"), "/lib");
        assertTrue(addLibIdx > addExeIdx);
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
        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        int addExeIdx =
                assertOptionsContain(xPackOptions, "-add-file", excelsiorJet.getTargetOS().mangleExeName("test"), "/");
        int addLibIdx =
                assertOptionsContain(xPackOptions, "-add-file", externalJarRel.toString(), "/lib");
        assertTrue(addLibIdx > addExeIdx);
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

        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        int addExeIdx =
                assertOptionsContain(xPackOptions, "-add-file", excelsiorJet.getTargetOS().mangleExeName("test"), "/");
        int addLibIdx =
                assertOptionsContain(xPackOptions, "-add-file", toPlatform("extDep/test.jar"), "extDep");
        assertTrue(addLibIdx > addExeIdx);
        assertOptionsContain(xPackOptions, "-assign-resource", excelsiorJet.getTargetOS().mangleExeName("test"),
                "test.jar", toPlatform("extDep/test.jar"));
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

        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        int addExeIdx =
                assertOptionsContain(xPackOptions, "-add-file", excelsiorJet.getTargetOS().mangleExeName("test"), "/");

        int disableResourceIdx =
                assertOptionsContain(xPackOptions, "-disable-resource",
                        excelsiorJet.getTargetOS().mangleExeName("test"), "external.jar");
        assertTrue(disableResourceIdx > addExeIdx);
    }

    @Test
    public void testInvocationDll() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.DYNAMIC_LIBRARY).
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        assertOptionsContain(xPackOptions, "-add-file", excelsiorJet.getTargetOS().mangleDllName("test"), "/");
    }

    @Test
    public void testWindowsService() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.WINDOWS_SERVICE).
                excelsiorJetPackaging("excelsior-installer").
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.windowsServiceConfiguration().dependencies = new String[]{"dep1", "dep 2"};
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);


        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getExcelsiorInstallerXPackOptions(new File("target.exe"));

        String exeName = excelsiorJet.getTargetOS().mangleExeName("test");
        int addExeIdx =
                assertOptionsContain(xPackOptions, "-add-file", exeName, "/");
        int serviceIdx =
                assertOptionsContain(xPackOptions, "-service", exeName, "", "test", "test");
        assertTrue(serviceIdx > addExeIdx);
        int serviceStartupIdx =
                assertOptionsContain(xPackOptions, "-service-startup", exeName, "system", "auto", "start-after-install");
        assertTrue(serviceStartupIdx > serviceIdx);
        int dependenciesIdx =
                assertOptionsContain(xPackOptions, "-service-dependencies", exeName, "dep1,dep 2");
        assertTrue(dependenciesIdx > serviceStartupIdx);
    }

    @Test
    public void testTomcatWindowsService() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.TOMCAT).
                excelsiorJetPackaging("excelsior-installer").
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getExcelsiorInstallerXPackOptions(new File("target.exe"));

        String exeName = "bin/" + excelsiorJet.getTargetOS().mangleExeName("test");
        int serviceIdx =
                assertOptionsContain(xPackOptions,"-service", exeName, "", "Apache Tomcat",
                        "Apache Tomcat Server - http://tomcat.apache.org/");
        int serviceStartupIdx =
                assertOptionsContain(xPackOptions,"-service-startup", exeName, "system", "auto", "start-after-install");
        assertTrue(serviceStartupIdx > serviceIdx);
    }

    @Test
    public void testCompactProfile() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        prj.runtimeConfiguration().profile = "compact3";
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        assertOptionsContain(xPackOptions,"-profile", "compact3");
    }

    @Test
    public void testDiskFootprintReduction() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN).globalOptimizer(true);
        prj.runtimeConfiguration().diskFootprintReduction = "high-memory";
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.execProfilesDir(prj.jetResourcesDir()).execProfilesName("test");
        TestRunExecProfiles testRunExecProfiles = Mockito.mock(TestRunExecProfiles.class);
        Mockito.doReturn(fileSpy("test.usg")).when(testRunExecProfiles).getUsg();
        prj = Mockito.spy(prj);
        Mockito.when(prj.testRunExecProfiles()).thenReturn(testRunExecProfiles);
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        assertOptionsContain(xPackOptions, "-reduce-disk-footprint", "high-memory");
    }

    @Test
    public void testRtLocation() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        prj.runtimeConfiguration().location = "hidden/rt";
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        assertOptionsContain(xPackOptions,"-move-file", "rt", "hidden/rt");
    }

    @Test
    public void testPackageFiles() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        prj.packageFiles(Arrays.asList(new PackageFile(fileSpy("test.file"), null),
                                       new PackageFile(fileSpy("test2.file"), "test/location")));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        assertOptionsContain(xPackOptions, "-add-file", excelsiorJet.getTargetOS().mangleExeName("test"), "/");
        assertOptionsContain(xPackOptions, "-add-file", new File("test.file").getAbsolutePath(), "/");
        assertOptionsContain(xPackOptions, "-add-file", new File("test2.file").getAbsolutePath(), "test/location");
    }

    @Test
    public void testExcelsiorInstaller() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        ExcelsiorInstallerConfig config = prj.excelsiorInstallerConfiguration();
        ExcelsiorJet excelsiorJet = excelsiorJet();

        prj.excelsiorJetPackaging("excelsior-installer");
        config.cleanupAfterUninstall = true;
        config.compressionLevel = "high";
        config.language = "french";
        config.afterInstallRunnable.target = "runnable";
        config.afterInstallRunnable.arguments = new String[]{"arg1", "arg2"};
        config.installationDirectory.path = "inst/dir/path";
        config.installationDirectory.type = "absolute-path";
        config.installationDirectory.fixed = true;
        config.registryKey = "registry/key";
        Shortcut shortcut = new Shortcut();
        shortcut.location = "startup";
        shortcut.target = "target";
        shortcut.name = "shortcut";
        shortcut.icon = new PackageFile(fileSpy("shortcut.ico"), null);
        shortcut.workingDirectory = "working/directory";
        shortcut.arguments = new String[]{"arg1", "arg2"};
        config.shortcuts = Collections.singletonList(shortcut);
        config.noDefaultPostInstallActions = true;
        PostInstallCheckbox runCheckbox = new PostInstallCheckbox();
        runCheckbox.type = "run";
        runCheckbox.target = "runTarget";
        runCheckbox.workingDirectory = "run/directory";
        runCheckbox.arguments = new String[0];
        PostInstallCheckbox openCheckbox = new PostInstallCheckbox();
        openCheckbox.type = "open";
        openCheckbox.target = "openTarget";
        PostInstallCheckbox restartCheckbox = new PostInstallCheckbox();
        restartCheckbox.type = "restart";
        restartCheckbox.checked = false;
        config.postInstallCheckboxes = Arrays.asList(runCheckbox, openCheckbox, restartCheckbox);
        FileAssociation fileAssociation = new FileAssociation();
        fileAssociation.extension = "ext";
        fileAssociation.target = "faTarget";
        fileAssociation.description = "assoc description";
        fileAssociation.targetDescription = "target description";
        fileAssociation.icon = new PackageFile(fileSpy("fileassoc.ico"), "icons");
        fileAssociation.arguments = new String[]{"arg"};
        fileAssociation.checked = false;
        config.fileAssociations = Collections.singletonList(fileAssociation);
        config.installCallback = fileSpy("install.callback");
        config.uninstallCallback = new PackageFile(null, "uninstall.callback");
        config.welcomeImage = fileSpy("welcome.image");
        config.installerImage = fileSpy("installer.image");
        config.uninstallerImage = fileSpy("uninstaller.image");

        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getExcelsiorInstallerXPackOptions(new File("setup.exe"));

        assertOptionsContain(xPackOptions, "-cleanup-after-uninstall");
        assertOptionsContain(xPackOptions, "-compression-level", "high");
        assertOptionsContain(xPackOptions, "-language", "french");
        assertOptionsContain(xPackOptions, "-after-install-runnable", "runnable", "arg1 arg2");
        assertOptionsContain(xPackOptions, "-installation-directory", "inst/dir/path");
        assertOptionsContain(xPackOptions, "-installation-directory-type", "absolute-path");
        assertOptionsContain(xPackOptions, "-installation-directory-fixed");
        assertOptionsContain(xPackOptions, "-registry-key", "registry/key");
        assertOptionsContain(xPackOptions, "-add-file", new File("shortcut.ico").getAbsolutePath(), "/");
        assertOptionsContain(xPackOptions, "-shortcut", "startup", "target", "shortcut", "/shortcut.ico", "working/directory", "arg1 arg2");
        assertOptionsContain(xPackOptions, "-no-default-post-install-actions");
        assertOptionsContain(xPackOptions, "-post-install-checkbox-run", "runTarget", "run/directory", "", "checked");
        assertOptionsContain(xPackOptions, "-post-install-checkbox-open", "openTarget", "checked");
        assertOptionsContain(xPackOptions, "-post-install-checkbox-restart", "unchecked");
        assertOptionsContain(xPackOptions, "-add-file", new File("fileassoc.ico").getAbsolutePath(), "icons");
        assertOptionsContain(xPackOptions, "-file-association", "ext", "faTarget", "assoc description",
                "target description", "icons/fileassoc.ico", "arg", "unchecked");
        assertOptionsContain(xPackOptions, "-install-callback", new File("install.callback").getAbsolutePath());
        assertOptionsContain(xPackOptions, "-uninstall-callback", "uninstall.callback");
        assertOptionsContain(xPackOptions, "-welcome-image", new File("welcome.image").getAbsolutePath());
        assertOptionsContain(xPackOptions, "-installer-image", new File("installer.image").getAbsolutePath());
        assertOptionsContain(xPackOptions, "-uninstaller-image", new File("uninstaller.image").getAbsolutePath());
    }

    @Test
    public void testTomcatPort() throws Exception {
        File testJarSpy = mavenDepSpy("test.jar");
        ProjectDependency dep = DependencyBuilder.testProjectDependency(testJarSpy).asProjectDependency();
        JetProject prj = testProject(ApplicationType.TOMCAT).
                excelsiorJetPackaging("excelsior-installer").
                projectDependencies(singletonList(dep)).
                dependencies(singletonList(DependencyBuilder.testDependencySettings().version(dep.version).asDependencySettings()));
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.tomcatConfiguration().allowUserToChangeTomcatPort = true;
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);

        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getExcelsiorInstallerXPackOptions(new File("target.exe"));
        assertOptionsContain(xPackOptions, "-allow-user-to-change-tomcat-port");
    }

    @Test
    public void testArgFile() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<XPackOption> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();
        assertArrayEquals(new String[]{
                "-add-file " + excelsiorJet.getTargetOS().mangleExeName("test") + " /",
                "-profile auto"
        }, PackagerArgsGenerator.getArgFileContent(xPackOptions).toArray());
    }
}
