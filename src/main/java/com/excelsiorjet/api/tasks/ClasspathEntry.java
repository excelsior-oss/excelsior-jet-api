package com.excelsiorjet.api.tasks;

import java.io.File;

public class ClasspathEntry {

    private final File file;
    private final boolean isLib;

    public ClasspathEntry(File file, boolean isLib) {
        this.file = file;
        this.isLib = isLib;
    }

    public File getFile() {
        return file;
    }

    public boolean isLib() {
        return isLib;
    }

}
