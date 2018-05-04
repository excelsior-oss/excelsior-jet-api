package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.tasks.Tests;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

import static com.excelsiorjet.api.tasks.Tests.assertThrows;
import static com.excelsiorjet.api.util.Txt.s;
import static org.junit.Assert.assertEquals;

public class PDBConfigTest {

    @Test
    public void testNotSupported() {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(false).when(excelsiorJet).isSmartSupported();
        JetProject project = Mockito.mock(JetProject.class);
        PDBConfig pdbConfig = new PDBConfig();
        pdbConfig.baseDir = new File("");
        assertThrows(()->pdbConfig.fillDefaults(project, excelsiorJet), s("JetApi.PDBConfigurationNotSupported.Failure"));
    }


    @Test
    public void testKeepInBuildDirWithBaseDir() {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(true).when(excelsiorJet).isSmartSupported();
        Mockito.doReturn(true).when(excelsiorJet).isPDBConfigurationSupported();
        JetProject project = Mockito.mock(JetProject.class);
        PDBConfig pdbConfig = new PDBConfig();
        pdbConfig.baseDir = new File("");
        pdbConfig.keepInBuildDir = true;
        assertThrows(()->pdbConfig.fillDefaults(project, excelsiorJet), s("JetApi.PDBInBuildDir.Failure", "baseDir"));
    }

    @Test
    public void testBaseDirWithSpecificLocation() {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(true).when(excelsiorJet).isSmartSupported();
        Mockito.doReturn(true).when(excelsiorJet).isPDBConfigurationSupported();
        JetProject project = Mockito.mock(JetProject.class);
        PDBConfig pdbConfig = new PDBConfig();
        pdbConfig.baseDir = new File("");
        pdbConfig.specificLocation = new File("");
        assertThrows(()->pdbConfig.fillDefaults(project, excelsiorJet), s("JetApi.PDBBasedDirAndSpecificLocation.Failure"));
    }

    @Test
    public void testDefaults() throws JetTaskFailureException {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(true).when(excelsiorJet).isSmartSupported();
        Mockito.doReturn(true).when(excelsiorJet).isPDBConfigurationSupported();
        JetProject project = Tests.testProject(ApplicationType.PLAIN);
        PDBConfig pdbConfig = new PDBConfig();
        pdbConfig.fillDefaults(project, excelsiorJet);
        assertEquals(project.projectName(), pdbConfig.pdbLocation().getName());
        assertEquals(project.groupId(), pdbConfig.pdbLocation().getParentFile().getName());
    }
}
