package com.excelsiorjet.api.tasks;

import com.excelsiorjet.TestUtils;
import com.excelsiorjet.api.util.Txt;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class RunStopSupportTest {

    //1 min timeout
    private static int RUN_TIMEOUT = 60000;

    private RunStopSupport runStopSupport(boolean toStop) {
        return new RunStopSupport(TestUtils.workDir(), toStop);
    }

    /**
     * Simulates running a task via creating a thread, RunStopSupport object and waiting for the termination file.
     */
    private class RunTask {

        private static final int SLEEP_TIME = 100;

        private Thread runThread;

        private File termFile;

        RunTask(long duration) {
            RunStopSupport runStopSupport = runStopSupport(false);
            try {
                termFile = runStopSupport.prepareToRunTask();
            } catch (JetTaskFailureException e) {
                return;
            }
            runThread = new Thread(()->{
                long timeToEnd = duration;
                while (timeToEnd > 0) {
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        break;
                    }
                    timeToEnd -= SLEEP_TIME;
                    if (termFile.exists()) {
                        break;
                    }
                }
                runStopSupport.taskFinished();
            });
            runThread.start();
        }

        boolean isCompleted() {
            return !runThread.isAlive();
        }

        void join(long millis) {
            try {
                runThread.join(millis);
            } catch (InterruptedException ingore) {
            }
        }
    }

    private RunTask runTask(long duration) {
        return new RunTask(duration);
    }

    private RunTask runTask() {
        return runTask(RUN_TIMEOUT);
    }

    private void stopTask(RunTask expectedToStop) {
        try {
            runStopSupport(true).stopRunTask();
        } catch (JetTaskFailureException e) {
            fail(e.getMessage());
        }
        assertTrue(expectedToStop.isCompleted());
    }

    @Test
    public void basicUsage() {
        RunTask runTask = runTask();
        stopTask(runTask);
    }

    @Test
    public void twoTasks() {
        RunTask runTask1 = runTask();
        RunTask runTask2 = runTask();
        stopTask(runTask2);
        assertFalse(runTask1.isCompleted());
        stopTask(runTask1);
    }

    @Test
    public void secondDies() {
        RunTask runTask1 = runTask();
        RunTask runTask2 = runTask(100);

        //wait until second task dies;
        runTask2.join(RUN_TIMEOUT);
        assertTrue(runTask2.isCompleted());
        assertFalse(runTask1.isCompleted());

        //kill first task
        stopTask(runTask1);
    }

    @Test
    public void middleDies() {
        RunTask runTask1 = runTask();
        RunTask runTask2 = runTask(100);
        RunTask runTask3 = runTask();

        //wait until second task dies;
        runTask2.join(RUN_TIMEOUT);
        assertTrue(runTask2.isCompleted());
        assertFalse(runTask3.isCompleted());

        //kill 3d task
        stopTask(runTask3);
        assertFalse(runTask1.isCompleted());

        //kill first task
        stopTask(runTask1);
        assertTrue(runTask1.isCompleted());
    }

    @Test
    public void stopNoRun() {
        try {
            runStopSupport(true).stopRunTask();
            fail("Stopped task without run");
        } catch (JetTaskFailureException e) {
            assertEquals(e.getMessage(), Txt.s("StopTask.NoRunApp.Error"));
            //passed
        }
    }

}