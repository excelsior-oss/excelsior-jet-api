package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.log.AbstractLog;
import com.excelsiorjet.api.util.Utils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.excelsiorjet.api.util.Txt.s;

class TaskUtils {

    private static final String LIB_DIR = "lib";

    private static void copyDependency(File from, File to, File buildDir, List<ClasspathEntry> dependencies, boolean isLib) throws JetTaskIoException {
        try {
            Utils.copyFile(from.toPath(), to.toPath());
            dependencies.add(new ClasspathEntry(buildDir.toPath().relativize(to.toPath()).toFile(), isLib));
        } catch (IOException e) {
            throw new JetTaskIoException(e);
        }
    }

    /**
     * Copies project dependencies.
     *
     * @return list of dependencies relative to buildDir
     */
    static List<ClasspathEntry> copyDependencies(File buildDir, File mainJar, Stream<ClasspathEntry> dependencies) throws JetTaskFailureException, IOException {
        File libDir = new File(buildDir, LIB_DIR);
        Utils.mkdir(libDir);
        ArrayList<ClasspathEntry> classpathEntries = new ArrayList<>();
        try {
            copyDependency(mainJar, new File(buildDir, mainJar.getName()), buildDir, classpathEntries, false);
            dependencies
                    .filter(a -> a.getFile().isFile())
                    .forEach(a ->
                            copyDependency(a.getFile(), new File(libDir, a.getFile().getName()), buildDir, classpathEntries, a.isLib())
                    )
            ;
            return classpathEntries;
        } catch (JetTaskIoException e) {
            // catch and unwrap io exception thrown by copyDependency in forEach lambda
            throw new IOException(s("JetMojo.ErrorCopyingDependency.Exception"), e.getCause());
        }
    }

    static String createJetCompilerProject(File buildDir, ArrayList<String> compilerArgs, List<ClasspathEntry> dependencies, ArrayList<String> modules, String prj) throws JetTaskFailureException {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(buildDir, prj))))) {
            compilerArgs.forEach(out::println);
            for (ClasspathEntry dep : dependencies) {
                out.println("!classpathentry " + dep.getFile().toString());
                out.println("  -optimize=" + (dep.isLib() ? "autodetect" : "all"));
                out.println("  -protect=" + (dep.isLib() ? "nomatter" : "all"));
                out.println("!end");
            }
            for (String mod : modules) {
                out.println("!module " + mod);
            }
        } catch (FileNotFoundException e) {
            throw new JetTaskFailureException(e.getMessage());
        }
        return prj;
    }

    private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipArchiveOutputStream out) throws IOException {
        File[] files = new File(sourceDir).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                compressDirectoryToZipfile(rootDir, sourceDir + File.separator + file.getName(), out);
            } else {
                ZipArchiveEntry entry = new ZipArchiveEntry(file.getAbsolutePath().substring(rootDir.length() + 1));
                if (Utils.isUnix() && file.canExecute()) {
                    entry.setUnixMode(0100777);
                }
                out.putArchiveEntry(entry);
                InputStream in = new BufferedInputStream(new FileInputStream(sourceDir + File.separator + file.getName()));
                IOUtils.copy(in, out);
                IOUtils.closeQuietly(in);
                out.closeArchiveEntry();
            }
        }
    }

    static void compressZipfile(File sourceDir, File outputFile) throws IOException {
        ZipArchiveOutputStream zipFile = new ZipArchiveOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile)));
        compressDirectoryToZipfile(sourceDir.getAbsolutePath(), sourceDir.getAbsolutePath(), zipFile);
        IOUtils.closeQuietly(zipFile);
    }

    static void copyQuietly(Path source, Path target) {
        // We could just use Maven FileUtils.copyDirectory method but it copies a directory as a whole
        // while here we copy only those files that were changed from previous build.
        try {
            Utils.copyDirectory(source, target);
        } catch (IOException e) {
            AbstractLog.instance().warn(s("TestRunMojo.ErrorWhileCopying.Warning", source.toString(), target.toString(), e.getMessage()), e);
        }
    }
}
