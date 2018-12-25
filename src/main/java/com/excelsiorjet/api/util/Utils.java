/*
 * Copyright (c) 2015-2017, Excelsior LLC.
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
package com.excelsiorjet.api.util;

import com.excelsiorjet.api.platform.Host;
import com.excelsiorjet.api.tasks.JetTaskFailureException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

public class Utils {

    public static void cleanDirectory(File f) throws IOException {
        Files.walkFileTree(f.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            private void deleteFile(File f) throws IOException {
                if (!f.delete()) {
                    if (f.exists()) {
                        throw new IOException(Txt.s("JetApi.UnableToDelete.Error", f.getAbsolutePath()));
                    }
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                deleteFile(file.toFile());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                if (file.toFile().exists()) {
                    throw new IOException(Txt.s("JetApi.UnableToDelete.Error", f.getAbsolutePath()));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                deleteFile(dir.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void cleanDirectorySilently(File f) {
        try {
            cleanDirectory(f);
        } catch (IOException ignore) {
        }
    }

    public static void copyFile(Path source, Path target) throws IOException {
        if (!target.toFile().exists()) {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        } else if (source.toFile().lastModified() != target.toFile().lastModified()) {
            //copy only files that were changed
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }

    }

    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path subfolder, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(subfolder)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(sourceFile));
                copyFile(sourceFile, targetFile);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path sourceFile, IOException e) throws IOException {
                throw new IOException(Txt.s("Utils.CannotCopyFile.Error", sourceFile.toString(), e.getMessage()), e);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path source, IOException ioe) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static boolean isEmpty(String s) {
        return (s == null) || s.isEmpty();
    }

    public static boolean isEmpty(String[] strings) {
        return (strings == null) || (strings.length == 0);
    }

    public static String randomAlphanumeric(int count) {
        char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < count; i++) {
            res.append(chars[(int) (chars.length * Math.random())]);
        }
        return res.toString();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

    public static void mkdir(File dir) throws JetTaskFailureException {
        if (!dir.exists() && !dir.mkdirs()) {
            if (!dir.exists()) {
                throw new JetTaskFailureException(s("JetApi.DirCreate.Error", dir.getAbsolutePath()));
            }
            logger.warn(s("JetApi.DirCreate.Warning", dir.getAbsolutePath()));
        }
    }

    @FunctionalInterface
    private interface CreateArchiveEntry {
        ArchiveEntry createEntry(String name, long size, int mode);
    }

    private static ArchiveEntry createZipEntry(String name, long size, int mode) {
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        if (Host.isUnix()) {
            entry.setUnixMode(mode);
        }
        return entry;
    }

    private static ArchiveEntry createTarEntry(String name, long size, int mode) {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(size);
        if (Host.isUnix()) {
            entry.setMode(mode);
        }
        return entry;
    }

    private static void compressDirectoryToArchive(String rootDir, String sourceDir, ArchiveOutputStream out, CreateArchiveEntry cae) throws IOException {
        File[] files = new File(sourceDir).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                compressDirectoryToArchive(rootDir, sourceDir + File.separator + file.getName(), out, cae);
            } else {
                ArchiveEntry entry = cae.createEntry(
                        file.getAbsolutePath().substring(rootDir.length() + 1),
                        file.length(),
                        file.canExecute() ? /*-rwxr-xr-x*/ 0100755 : /*-rw-r--r--*/ 0100644
                );
                out.putArchiveEntry(entry);
                try (InputStream in = new BufferedInputStream(new FileInputStream(sourceDir + File.separator + file.getName()))) {
                    copy(in, out);
                }
                out.closeArchiveEntry();
            }
        }
    }

    public static void compressToZipFile(File sourceDir, File outputFile) throws IOException {
        try (ZipArchiveOutputStream zipFile = new ZipArchiveOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            compressDirectoryToArchive(sourceDir.getAbsolutePath(), sourceDir.getAbsolutePath(), zipFile,
                    Utils::createZipEntry);
        }
    }

    public static void compressToTarGzFile(File sourceDir, File outputFile) throws IOException {
        try (TarArchiveOutputStream tarFile = new TarArchiveOutputStream(new GzipCompressorOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile))))) {
            compressDirectoryToArchive(sourceDir.getAbsolutePath(), sourceDir.getAbsolutePath(), tarFile,
                    Utils::createTarEntry);
        }
    }

    public static void copyQuietly(Path source, Path target) {
        // We could just use Maven FileUtils.copyDirectory method but it copies a directory as a whole
        // while here we copy only those files that were changed from previous build.
        try {
            copyDirectory(source, target);
        } catch (IOException e) {
            logger.warn(s("TestRunTask.ErrorWhileCopying.Warning", source.toString(), target.toString(), e.getMessage()), e);
        }
    }

    public static String parameterToEnumConstantName(String parameter) {
        return parameter.toUpperCase().replace('-', '_');
    }

    public static String enumConstantNameToParameter(String constantName) {
        return constantName.toLowerCase().replace('_', '-');
    }

    /**
     * Encloses string in double quotes (") if it contains space.
     */
    public static String quoteCmdLineArgument(String arg) {
        return arg.contains(" ") ? '"' + arg + '"' : arg;
    }

    /**
     * Splits a string containing value for {@link com.excelsiorjet.api.tasks.JetProject#runArgs},
     * where arguments are separated by commas and commas within an argument are escaped with '\'.
     * <p>
     * For example, "value1,value2.1\, value2.2"
     * is parsed into 2 arguments: ["value1", "value2.1, value2.2"]
     */
    public static String[] parseRunArgs(String runArgs) {
        List<String> res = new ArrayList<>();
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < runArgs.length(); i++) {
            char c = runArgs.charAt(i);
            if (c == ',') {
                if (i > 0 && runArgs.charAt(i - 1) == '\\') {
                    // replace "\," with ","
                    buff.setCharAt(buff.length() - 1, c);
                } else {
                    // split args
                    res.add(buff.toString());
                    buff = new StringBuilder();
                }
            } else {
                buff.append(c);
            }
        }
        res.add(buff.toString());
        return res.toArray(new String[res.size()]);
    }

    public static String[] prepend(String firstElement, String[] remaining) {
        if (remaining == null) {
            return new String[]{firstElement};
        }
        ArrayList<String> res = new ArrayList<>();
        res.add(firstElement);
        res.addAll(Arrays.asList(remaining));
        return res.toArray(new String[remaining.length + 1]);
    }

    public static String getCanonicalPath(File path) {
        try {
            return path.getCanonicalPath();
        } catch (IOException e) {
            // getCanonicalPath throws IOException,
            // so just return absolute path in a very rare case of IOException as there is no other
            // appropriate way to handle this situation.
            return path.getAbsolutePath();
        }
    }

    /**
     * @return String in format "([groupId],[artifactId],[version], [path])" (all fields are optional)
     */
    public static String idStr(String groupId, String artifactId, String version, File path) {
        return Stream.of(groupId, artifactId, version, path == null? null: getCanonicalPath(path)).
                filter(item -> item != null).
                collect(Collectors.joining(":", "(", ")"));
    }

    public static void linesToFile(List<String> lines, File file) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {
            lines.forEach(out::println);
        }
    }

    public static String deriveFourDigitVersion(String version) {
        String[] versions = version.split("\\.");
        String[] finalVersions = new String[]{"0", "0", "0", "0"};
        for (int i = 0; i < Math.min(versions.length, 4); ++i) {
            try {
                finalVersions[i] = Integer.decode(versions[i]).toString();
            } catch (NumberFormatException e) {
                int minusPos = versions[i].indexOf('-');
                if (minusPos > 0) {
                    String v = versions[i].substring(0, minusPos);
                    try {
                        finalVersions[i] = Integer.decode(v).toString();
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return String.join(".", (CharSequence[]) finalVersions);
    }

    public static File checkFileWithDefault(File file, File defaultFile, String notExistKey, String notExistParam) throws JetTaskFailureException {
        if (file == null) {
            if (defaultFile.exists()) {
                return defaultFile;
            }
        } else if (!file.exists()) {
            throw new JetTaskFailureException(s(notExistKey, file.getAbsolutePath(), notExistParam));
        }
        return file;
    }
}
