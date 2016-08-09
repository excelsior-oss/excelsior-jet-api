/*
 * Copyright (c) 2015, Excelsior LLC.
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

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

public class Utils {

    public static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").contains("Linux");
    }

    public static boolean isOSX() {
        return System.getProperty("os.name").contains("OS X");
    }

    public static boolean isUnix() {
        return isLinux() || isOSX();
    }

    public static String getExeFileExtension() {
        return isWindows() ? ".exe" : "";
    }

    public static String mangleExeName(String exe) {
        return exe + getExeFileExtension();
    }

    public static void cleanDirectory(File f) throws IOException {
        Files.walkFileTree(f.toPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            private void deleteFile(File f) throws IOException {
                if (!f.delete()) {
                    if (f.exists()) {
                        throw new IOException(Txt.s("Utils.CleanDirectory.Failed", f.getAbsolutePath()));
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
                    throw new IOException(Txt.s("Utils.CleanDirectory.Failed", f.getAbsolutePath()));
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

    public static void closeQuietly(AutoCloseable in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ignore) {}
    }

    public static void mkdir(File dir) throws JetTaskFailureException {
        if (!dir.exists() && !dir.mkdirs()) {
            if (!dir.exists()) {
                throw new JetTaskFailureException(s("JetApi.DirCreate.Error", dir.getAbsolutePath()));
            }
            logger.warn(s("JetApi.DirCreate.Warning", dir.getAbsolutePath()));
        }
    }

    private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipArchiveOutputStream out) throws IOException {
        File[] files = new File(sourceDir).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                compressDirectoryToZipfile(rootDir, sourceDir + File.separator + file.getName(), out);
            } else {
                ZipArchiveEntry entry = new ZipArchiveEntry(file.getAbsolutePath().substring(rootDir.length() + 1));
                if (isUnix() && file.canExecute()) {
                    entry.setUnixMode(0100777);
                }
                out.putArchiveEntry(entry);
                InputStream in = new BufferedInputStream(new FileInputStream(sourceDir + File.separator + file.getName()));
                copy(in, out);
                closeQuietly(in);
                out.closeArchiveEntry();
            }
        }
    }

    public static void compressZipfile(File sourceDir, File outputFile) throws IOException {
        ZipArchiveOutputStream zipFile = new ZipArchiveOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile)));
        compressDirectoryToZipfile(sourceDir.getAbsolutePath(), sourceDir.getAbsolutePath(), zipFile);
        closeQuietly(zipFile);
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

}
