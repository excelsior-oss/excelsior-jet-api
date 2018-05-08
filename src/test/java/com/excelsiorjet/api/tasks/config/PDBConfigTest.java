package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.log.Log;
import com.excelsiorjet.api.log.StdOutLog;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.tasks.Tests;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.ResourceBundle;

import static com.excelsiorjet.api.tasks.Tests.assertThrows;
import static com.excelsiorjet.api.util.Txt.s;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PDBConfigTest {

    static {
        JetProject.configureEnvironment(new StdOutLog(), ResourceBundle.getBundle("Strings"));
    }

    @Test
    public void testNotSupported() throws JetTaskFailureException {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(false).when(excelsiorJet).isSmartSupported();
        JetProject project = Mockito.mock(JetProject.class);

        Log previous = Log.logger;
        try {
            Log.logger = Mockito.spy(Log.logger);
            PDBConfig pdbConfig = new PDBConfig();
            pdbConfig.baseDir = new File("");
            pdbConfig.fillDefaults(project, excelsiorJet);
            assertTrue(pdbConfig.keepInBuildDir);
            Mockito.verify(Log.logger).warn(s("JetApi.PDBConfigurationNotSupported.Warning"));
        } finally {
            Log.logger = previous;
        }
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
