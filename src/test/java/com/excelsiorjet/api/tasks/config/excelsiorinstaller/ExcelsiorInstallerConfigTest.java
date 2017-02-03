package com.excelsiorjet.api.tasks.config.excelsiorinstaller;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.platform.OS;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.config.packagefile.PackageFile;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;

import static com.excelsiorjet.api.tasks.Tests.assertNotThrows;
import static com.excelsiorjet.api.tasks.Tests.assertThrows;
import static com.excelsiorjet.api.util.Txt.s;

public class ExcelsiorInstallerConfigTest {

    private static ExcelsiorJet excelsiorJet(OS targetOS) {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(targetOS).when(excelsiorJet).getTargetOS();
        return excelsiorJet;
    }

    private static ExcelsiorJet excelsiorJetWithAdvancedFeatures(boolean advancedFeatures) {
        ExcelsiorJet excelsiorJet = Mockito.mock(ExcelsiorJet.class);
        Mockito.doReturn(advancedFeatures).when(excelsiorJet).isAdvancedExcelsiorInstallerFeaturesSupported();
        return excelsiorJet;
    }

    private File notExistingFile(String name) {
        File file = Mockito.spy(new File(name));
        Mockito.doReturn(false).when(file).exists();
        return file;
    }

    @Test
    public void testInstallationDirectory() {
        InstallationDirectory installationDirectory = new InstallationDirectory();
        installationDirectory.type = "user-home";
        assertThrows(()->installationDirectory.validate(excelsiorJet(OS.WINDOWS)),
                s("JetApi.ExcelsiorInstaller.SpecificOSInstallationDirectoryType", installationDirectory.type, "Linux"));
        installationDirectory.type = "program-files";
        assertThrows(()->installationDirectory.validate(excelsiorJet(OS.LINUX)),
                s("JetApi.ExcelsiorInstaller.SpecificOSInstallationDirectoryType", installationDirectory.type, "Windows"));
    }

    @Test
    public void testShortcut() {
        ExcelsiorJet excelsiorJet = excelsiorJetWithAdvancedFeatures(false);
        Shortcut shortcut = new Shortcut();
        assertThrows(()->shortcut.validate(excelsiorJet), s("JetApi.ExcelsiorInstaller.ShortcutNameNull"));
        shortcut.name = "shortcut";
        assertThrows(()->shortcut.validate(excelsiorJet), s("JetApi.ExcelsiorInstaller.ShortcutTargetNull", shortcut.name));
        shortcut.target = "target";
        shortcut.icon = new PackageFile(null, "icon");
        assertThrows(()->shortcut.validate(excelsiorJet), s("JetApi.ExcelsiorInstaller.ShortcutIconNotSupported", shortcut.name));
        File iconFile = notExistingFile("icon");
        shortcut.icon = new PackageFile(iconFile, null);
        assertThrows(()->shortcut.validate(excelsiorJetWithAdvancedFeatures(true)),
                s("JetApi.ExcelsiorInstaller.ShortcutIconDoesNotExist", iconFile.getAbsolutePath(), shortcut.name));
    }

    @Test
    public void testPostInstallCheckbox() {
        PostInstallCheckbox postInstallCheckbox = new PostInstallCheckbox();
        assertThrows(postInstallCheckbox::validate, s("JetApi.ExcelsiorInstaller.PostInstallActionTargetNull"));
        postInstallCheckbox.type = "restart";
        assertNotThrows(postInstallCheckbox::validate);
        postInstallCheckbox.target = "target";
        assertThrows(postInstallCheckbox::validate, s("JetApi.ExcelsiorInstaller.PostInstallActionTargetNotNullForRestart"));
        postInstallCheckbox.type = "open";
        postInstallCheckbox.workingDirectory = "workDir";
        assertThrows(postInstallCheckbox::validate,
                s("JetApi.ExcelsiorInstaller.NotRunPostInstallActionParameter", "workingDirectory", "target"));
    }

    @Test
    public void testFileAssociation() {
        FileAssociation fileAssociation = new FileAssociation();
        assertThrows(fileAssociation::validate, s("JetApi.ExcelsiorInstaller.FileAssociationExtensionNull"));
        fileAssociation.extension = "ext";
        assertThrows(fileAssociation::validate, s("JetApi.ExcelsiorInstaller.FileAssociationTargetNull", "ext"));
        fileAssociation.target = "target";
        File iconFile = notExistingFile("icon");
        fileAssociation.icon = new PackageFile(iconFile, null);
        assertThrows(fileAssociation::validate,
                s("JetApi.ExcelsiorInstaller.FileAssociationIconDoesNotExist", iconFile.getAbsolutePath(), fileAssociation.extension));
    }

    @Test
    public void testExcelsiorInstallConfig() {
        ExcelsiorJet excelsiorJet = excelsiorJet(OS.WINDOWS);
        JetProject project = Mockito.mock(JetProject.class);
        Mockito.doReturn(new File("jetResourcesDir")).when(project).jetResourcesDir();
        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            assertNotThrows(()->config.fillDefaults(project, excelsiorJet));
        }

        {
            //ExcelsiorInstallerConfig.fillDefaults is not reentrant, that's why we create it
            //before each check
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.language = "Russian";
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.Since11_3Parameter", "language"));
        }

        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.registryKey = "regKey";
            config.installCallback = new File("installCallback");
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.Since11_3Parameters", "registryKey,installCallback"));
        }

        Mockito.doReturn(true).when(excelsiorJet).since11_3();
        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.afterInstallRunnable.arguments = new String[]{"arg"};
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.AfterInstallRunnableTargetNull"));
        }

        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.compressionLevel = "high";
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.UnsupportedCompressionLevel", "high"));
        }

        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.postInstallCheckboxes = Collections.singletonList(new PostInstallCheckbox());
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.UnsupportedParameter", "postInstallCheckboxes"));
        }

        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.fileAssociations = Collections.singletonList(new FileAssociation());
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.UnsupportedParameter", "fileAssociations"));
        }

        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.uninstallCallback.path = notExistingFile("uninstall");
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.FileDoesNotExist", config.uninstallCallback.path.getAbsolutePath(),
                            "uninstallCallback"));
        }

        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.welcomeImage = notExistingFile("welcome");
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.UnsupportedParameter", "welcomeImage"));
        }

        Mockito.doReturn(true).when(excelsiorJet).isAdvancedExcelsiorInstallerFeaturesSupported();
        {
            ExcelsiorInstallerConfig config = new ExcelsiorInstallerConfig();
            config.installerImage = notExistingFile("installerImageFile");
            assertThrows(()->config.fillDefaults(project, excelsiorJet),
                    s("JetApi.ExcelsiorInstaller.FileDoesNotExist", config.installerImage.getAbsolutePath(),
                            "installerImage"));
        }
    }
}
