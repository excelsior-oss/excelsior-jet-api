package com.excelsiorjet.api.tasks.config.packagefile;

import com.excelsiorjet.api.tasks.Tests;
import org.junit.Test;

import java.io.File;

import static com.excelsiorjet.api.tasks.Tests.assertThrows;
import static com.excelsiorjet.api.util.Txt.s;
import static org.junit.Assert.*;

public class PackageFileTest {
    
    @Test
    public void packageFileTest() {
        File file = Tests.fileSpy("file");
        File folder = Tests.dirSpy("folder");
        PackageFile packageFile = new PackageFile(PackageFileType.FILE, folder);
        assertThrows(()->packageFile.validate(null), s("JetApi.PackageFileNotFile.Error", folder.getAbsolutePath()));
        PackageFile packageFolder = new PackageFile(PackageFileType.FOLDER, file);
        assertThrows(()->packageFolder.validate(null), s("JetApi.PackageFileNotFolder.Error", file.getAbsolutePath()));
    }

}