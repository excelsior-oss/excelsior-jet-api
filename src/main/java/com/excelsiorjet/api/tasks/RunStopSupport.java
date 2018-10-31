/*
 * Copyright (c) 2018, Excelsior LLC.
 *
 *  This file is part of Excelsior JET API.
 *
 *  Excelsior JET API is free software:
 *  you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Excelsior JET API is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Excelsior JET API.
 *  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.util.Txt;
import com.excelsiorjet.api.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.stream.Stream;

/**
 * Provides interprocess communication methods for running and stopping tasks.
 *
 * <p><b>Usage:</b></p>
 *
 * <p>In order to run a task:</p>
 * <blockquote><pre>
 *     RunStopSupport runStopSupport = new RunStopSupport(tempDir, false);
 *     File termFile = runStopSupport.prepareToRun();
 *     runTask(termFile);
 *     runStopSupport.taskFinished();
 * </blockquote></pre>
 *
 * It is expected that runTask() is run until it sees {@code termFile} in the file system.
 *
 * <p>In order to stop a task (from another process):</p>
 * <blockquote><pre>
 *     RunStopSupport runStopSupport = new RunStopSupport(tempDir, true);
 *     if (!runStopSupport.stopRunTask()) {
 *         //handle failed stop
 *     }
 * </blockquote></pre>
 * {@code stopRunTask} identifies what task is run now and stops it by creating the respective {@code termFile}.
 *
 * <p>Implementation details:</p>
 *    {@code prepareToRun} locks a file in a temp dir so another process where we call {@code stopRunTask} knows what
 *    task is waiting to stop. {@code stopRunTask} looks for the locked file and creates the respective termination file,
 *    thereby notifying the running task to stop. When the task is stopped it releases the lock and deletes both lock
 *    and termination files.
 */
public class RunStopSupport {

    private static final String TERM_FILE_PREFIX = "term.file";
    private static final String LOCK_PEFIX = "lock.";
    private static final String LOCK_TERM_FILE_PREFIX = LOCK_PEFIX + TERM_FILE_PREFIX;
    private static final String TERM_TEMP_DIR = "termination";

    //1 min timeout
    private static final int STOP_TIMEOUT = 60000;
    private static final int SLEEP_TIME = 300;


    private int id = -1;
    private File termTempDir;
    private boolean toStop;
    private Lock lock;

    private static Map<File, Lock> acquiredLocks = Collections.synchronizedMap(new HashMap<>());

    public RunStopSupport(File baseDir, boolean toStop) {
        String tempDirProp = System.getProperty("jet.run.temp.dir");
        this.termTempDir = new File(baseDir, Utils.isEmpty(tempDirProp)? TERM_TEMP_DIR : tempDirProp);
        this.toStop = toStop;
        termTempDir.mkdirs();
        cleanup();
        initID(toStop);
    }

    /**
     * File system lock wrapper.
     * When we need to lock a file, we have to create a channel and then call tryLock that returns a FileLock
     * on successful file locking. As we need to release that lock not immediately, we should keep the channel
     * and lock objects until the lock is released. We also keep track of acquired locks to prevent double locking
     * of the same file and to be able to use RunStopSupport objects within one process (useful for unit testing).
     */
    private static class Lock {
        private File fileLock;
        private FileOutputStream chanelHost;
        private FileLock lock;

        Lock(File fileLock, FileOutputStream chanelHost, FileLock lock) {
            this.fileLock = fileLock;
            this.chanelHost = chanelHost;
            this.lock = lock;
            acquiredLocks.put(fileLock, this);
        }

        void release() {
            try {
                lock.release();
                chanelHost.close();
                acquiredLocks.remove(fileLock);
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Tries to lock the given file
     * @return {code null} if the file is locked from another process, or the respective Lock object upon successful locking.
     */
    private static Lock acquireLock(File file) {
        if (acquiredLocks.containsKey(file)) {
            throw new IllegalStateException("double run task");
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            FileLock fileLock = fos.getChannel().tryLock();
            if (fileLock != null) {
                return new Lock(file, fos, fileLock);
            } else {
                fos.close();
                return null;
            }
        } catch (IOException e) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignore) {
                }
            }
            return null;
        }
    }

    /**
     * Returns {@code true} if the file is locked from another process or within this process.
     */
    private static boolean isLocked(File file) {
        if (acquiredLocks.containsKey(file)) {
            return true;
        }
        Lock lock = acquireLock(file);
        if (lock == null) {
            return true;
        } else {
            lock.release();
            return false;
        }
    }

    /**
     * Cleans up the temporary directory from files that are not used by any of the run tasks.
     */
    private void cleanup() {
        File[] termFileLocks = termTempDir.listFiles(f->f.getName().startsWith(LOCK_TERM_FILE_PREFIX));
        if (termFileLocks != null) {
            Stream.of(termFileLocks).filter(f -> !isLocked(f))
                    .forEach(File::delete);
        }
        File[] termFiles = termTempDir.listFiles(f->f.getName().startsWith(TERM_FILE_PREFIX));
        if (termFiles != null) {
            Stream.of(termFiles).filter(f -> !new File(termTempDir, LOCK_PEFIX + f.getName()).exists())
                    .forEach(File::delete);
        }
    }

    /**
     *  Looks for the last created lock file and retrieves its id.
     */
    private int findLastID() {
        File[] termFileLocks = termTempDir.listFiles(f->f.getName().startsWith(LOCK_TERM_FILE_PREFIX));
        if (termFileLocks != null) {
            Optional<File> lastFile = Stream.of(termFileLocks).max(
                    (File f1, File f2) -> f1.lastModified() == f2.lastModified() ? f1.getName().compareTo(f2.getName()) :
                            Long.compare(f1.lastModified(), f2.lastModified())
            );
            if (lastFile.isPresent()) {
                String fname = lastFile.get().getName();
                return Integer.valueOf(fname.substring(LOCK_TERM_FILE_PREFIX.length()));
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private void initID(boolean toStop) {
       id = findLastID() + (toStop? 0 : 1);
    }

    private File getLockFile() {
        return new File(termTempDir, LOCK_TERM_FILE_PREFIX + id);
    }

    private File getTermFile() {
        return new File(termTempDir, TERM_FILE_PREFIX + id);
    }

    private boolean acquireLock() {
        lock = acquireLock(getLockFile());
        return lock != null;
    }

    public File prepareToRunTask() throws JetTaskFailureException {
        if (toStop) {
            throw new IllegalStateException("prepareToRunTask run when is going to stop");
        }
        if (!acquireLock()) {
            // it seems to be data race (simultaneous runs). Rare case, tell user to try again
            throw new JetTaskFailureException(Txt.s("RunTask.FailedToRun.Error"));
        }
        return getTermFile();
    }

    public void taskFinished() {
        if (toStop) {
            throw new IllegalStateException("taskFinished when is going to stop");
        }
        if (lock == null) {
            throw new IllegalStateException("prepareToRun was not called");
        }
        lock.release();
        getLockFile().delete();
        getTermFile().delete();
    }

    public void stopRunTask() throws JetTaskFailureException {
        if (!toStop) {
            throw new IllegalStateException("stopRunTask when is going to run");
        }
        if (id == -1) {
            throw new JetTaskFailureException(Txt.s("StopTask.NoRunApp.Error"));
        }
        try {
            if (isLocked(getLockFile())) {
                File termFile = getTermFile();
                termFile.createNewFile();
                int  timeToEnd = STOP_TIMEOUT;
                while ((timeToEnd > 0 ) && termFile.exists()) {
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException ignore) {
                    }
                    timeToEnd -= SLEEP_TIME;
                }
                if (timeToEnd <= 0) {
                    throw new JetTaskFailureException(Txt.s("StopTask.StopTimeout.Error"));
                }
            } else {
                //the process died before we stop it. At least we saw it so ignore that.
            }
        } catch (IOException e) {
            throw new JetTaskFailureException(Txt.s("StopTask.StopFailure.Error", e.getMessage()));
        }
    }

}
