package com.excelsiorjet;

import com.excelsiorjet.api.JetHome;
import com.excelsiorjet.api.JetHomeException;
import com.excelsiorjet.api.cmd.*;
import com.excelsiorjet.api.log.Log;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author kit
 *         Date: 04.12.2015
 */
public class XJavaTest {

    private Log log = mock(Log.class);
    private JetHome jetHome;

    public XJavaTest() throws JetHomeException {
        jetHome = new JetHome();
    }

    @Test
    public void runHello() throws CmdLineToolException, JetHomeException {
        assertEquals(0,
                new XJava(jetHome, "testClasses.HelloWorld")
                .withLog(log)
                .workingDirectory(TestUtils.workDir())
                        .execute());
        verify(log).info("Hello world!");
    }
}
