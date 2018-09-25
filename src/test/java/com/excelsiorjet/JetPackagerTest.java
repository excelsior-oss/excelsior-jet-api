package com.excelsiorjet;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.JetHome;
import com.excelsiorjet.api.JetHomeException;
import com.excelsiorjet.api.cmd.*;
import com.excelsiorjet.api.log.Log;
import com.excelsiorjet.api.platform.Host;
import com.excelsiorjet.api.util.Utils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Nikita Lipsky
 */
public class JetPackagerTest {

    private Log log = mock(Log.class);

    private final String target = "hw-native";
    private File targetDir = new File(TestUtils.workDir(), target);

    @Test
    public void testPackHelloWorld() throws CmdLineToolException, JetHomeException, IOException {
        ExcelsiorJet excelsiorJet = new ExcelsiorJet(new JetHome(), log);
        assertEquals(0,
                new JetCompiler("testClasses/HelloWorld")
                .workingDirectory(TestUtils.workDir())
                        .execute());
        File exe = new File(TestUtils.workDir(), excelsiorJet.getTargetOS().mangleExeName("HelloWorld"));
        assertEquals(0,
                new JetPackager("-add-file", exe.getAbsolutePath(), "/", "-target",
                        targetDir.getAbsolutePath()).execute());
        File exePacked = new File(TestUtils.workDir(), target + File.separator + excelsiorJet.getTargetOS().mangleExeName("HelloWorld"));
        assertTrue(exePacked.exists());
        new CmdLineTool(exePacked.getAbsolutePath()).withLog(log).execute();
        verify(log).info("Hello world!");
        exe.delete();
    }

    @After
    public void tearDown() {
        Utils.cleanDirectorySilently(targetDir);
    }

    @Test
    public void createAndTestFakeJC() throws CmdLineToolException, JetHomeException, IOException {
        try {
            File classesDir = TestUtils.classesDir();
            assertEquals(0,
                    new JetCompiler("testClasses/FakeJC", "-outputname=jc",
                            "-lookup=*.class=" + classesDir.getAbsolutePath())
                            .workingDirectory(TestUtils.workDir())
                            .execute());

            File exe = new File(TestUtils.workDir(), Host.mangleExeName("jc"));
            File fakeJetBin = new File(TestUtils.getFakeJetHomeNoCreate(), "bin");
            fakeJetBin.mkdirs();

            assertEquals(0,
                    new JetPackager("-add-file", exe.getAbsolutePath(), "/",
                            "-add-file", classesDir.getAbsolutePath(), "/",
                            "-assign-resource", exe.getName(), classesDir.getName(), classesDir.getName(),
                            "-target", fakeJetBin.getAbsolutePath()).execute());


            new JetCompiler(new JetHome(TestUtils.getOrCreateFakeJetHome().getAbsolutePath()))
                    .withEnvironment("JET_HOME", "")
                    .withLog(log).execute();
            verify(log).info("Ok");
        } finally {
            TestUtils.cleanFakeJetDir();
        }
    }


}
