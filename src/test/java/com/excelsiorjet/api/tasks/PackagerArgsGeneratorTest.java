package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.cmd.TestRunExecProfiles;
import com.excelsiorjet.api.tasks.PackagerArgsGenerator.Option;
import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.PackageFile;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import com.excelsiorjet.api.tasks.config.enums.ApplicationType;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.excelsiorjet.api.tasks.Tests.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class PackagerArgsGeneratorTest {

    private String toPlatform(String path) {
        return path.replace('/', File.separatorChar);
    }

    private int assertOptionsContain(List<Option> options, String option, String... parameters) {
        Option xpackOption = new Option(option, parameters);
        int idx = options.indexOf(xpackOption);
        if (idx < 0) {
            Optional<Option> optCandidate = options.stream().filter(o -> o.option.equals(option)).findFirst();
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

        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

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
        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

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

        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

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

        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

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

        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

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


        ArrayList<Option> xPackOptions = packagerArgsGenerator.getExcelsiorInstallerXPackOptions(new File("target.exe"));

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

        ArrayList<Option> xPackOptions = packagerArgsGenerator.getExcelsiorInstallerXPackOptions(new File("target.exe"));

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
        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

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
        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        assertOptionsContain(xPackOptions, "-reduce-disk-footprint", "high-memory");
    }

    @Test
    public void testRtLocation() throws JetTaskFailureException {
        JetProject prj = testProject(ApplicationType.PLAIN);
        prj.runtimeConfiguration().location = "hidden/rt";
        ExcelsiorJet excelsiorJet = excelsiorJet();
        prj.validate(excelsiorJet, true);
        PackagerArgsGenerator packagerArgsGenerator = new PackagerArgsGenerator(prj, excelsiorJet);
        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

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
        ArrayList<Option> xPackOptions = packagerArgsGenerator.getCommonXPackOptions();

        assertOptionsContain(xPackOptions, "-add-file", excelsiorJet.getTargetOS().mangleExeName("test"), "/");
        assertOptionsContain(xPackOptions, "-add-file", new File("test.file").getAbsolutePath(), "/");
        assertOptionsContain(xPackOptions, "-add-file", new File("test2.file").getAbsolutePath(), "test/location");
    }
}
