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
package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.tasks.config.enums.InstallationDirectoryType;
import com.excelsiorjet.api.tasks.config.enums.SetupCompressionLevel;
import com.excelsiorjet.api.tasks.config.enums.SetupLanguage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.excelsiorjet.api.util.EncodingDetector.detectEncoding;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Configuration parameters for Excelsior Installer packaging type.
 *
 * @author Nikita Lipsky
 */
public class ExcelsiorInstallerConfig {

    private static final String AUTO_DETECT_EULA_ENCODING = "autodetect";
    private static final String UNICODE_EULA_FLAG = "-unicode-eula";
    private static final String EULA_FLAG = "-eula";

    private static final Set<String> VALID_EULA_ENCODING_VALUES = new LinkedHashSet<String>() {{
        add(StandardCharsets.US_ASCII.name());
        add(StandardCharsets.UTF_16LE.name());
        add(AUTO_DETECT_EULA_ENCODING);
    }};

    public ExcelsiorInstallerConfig() {
        //init sub objects to just not check them for null.
        installationDirectory = new InstallationDirectory();
        shortcuts = Collections.emptyList();
        postInstallCheckboxes = Collections.emptyList();
        fileAssociations = Collections.emptyList();
        uninstallCallback = new PackageFile();
        afterInstallRunnable = new AfterInstallRunnable();
    }

    /**
     * The license agreement file. Used for Excelsior Installer.
     * File containing the end-user license agreement, for Excelsior Installer to display during installation.
     * The file must be a plain text file either in US-ASCII or UTF-16LE encoding.
     * If not set, and the file {@code eula.txt} in {@link JetProject#jetResourcesDir} folder exists,
     * that file is used by convention.
     *
     * @see #eulaEncoding eulaEncoding
     */
    public File eula;

    /**
     * Encoding of the EULA file. Permitted values:
     * <ul>
     *     <li>{@code US-ASCII}</li>
     *     <li>{@code UTF-16LE}</li>
     *     <li>{@code autodetect} (Default value)</li>
     * </ul>
     * If set to {@code autodetect}, the plugin looks for a byte order mark (BOM) in the file specified by {@link #eula}, and:
     * <ul>
     * <li>assumes US-ASCII encoding if no BOM is present,</li>
     * <li>assumes UTF-16LE encoding if the respective BOM ({@code 0xFF 0xFE}) is present, or </li>
     * <li>halts execution with error if some other BOM is present.</li>
     * </ul>
     * @see <a href="https://en.wikipedia.org/wiki/Byte_order_mark">Byte order mark</a>
     * @see #eula eula
     */
    protected String eulaEncoding = AUTO_DETECT_EULA_ENCODING;

    /**
     * (Windows) Excelsior Installer splash screen image in BMP format.
     * If not set, and the file {@code installerSplash.bmp} in {@link JetProject#jetResourcesDir} folder exists,
     * that file is used by convention.
     */
    public File installerSplash;

    /**
     * Resulting setup language.
     * <p>
     * Excelsior Installer can display its screens in multiple languages.
     * By default, it selects the most appropriate language based on the locale settings of the target system.
     * Use this parameter to force a specific language instead. Permitted values:
     *  {@code autodetect} (default), {@code english}, {@code french}, {@code german}, {@code japanese}, {@code russian},
     *  {@code polish}, {@code spanish}, {@code italian}, {@code brazilian}.
     * </p>
     * The functionality is available for Excelsior JET 11.3 and above.
     */
    public String language;

    /**
     * Remove all files from the installation folder on uninstall.
     * <p>
     * By default, the uninstaller only removes those files and directories from the installation directory
     * that the original applicaion installer, and possibly update installers, had created.
     * If any files and directories were created by a post-install runnnable, callback DLL,
     * installed and/or third-party applications, the uninstaller will leave them in place and report to the user that
     * it was unable to remove the installation directory due to their presence.
     * </p>
     * The functionality is available for Excelsior JET 11.3 and above.
     */
    public Boolean cleanupAfterUninstall;

    /**
     * Excelsior Installer can optionally run one of the executable files included in the package
     * upon successful installation. Use this parameter to specify the executable and its arguments.
     *
     * @see AfterInstallRunnable#target
     * @see AfterInstallRunnable#arguments
     */
    public AfterInstallRunnable afterInstallRunnable;

    /**
     * Compression level used for files packaging into setup.
     * <p>
     * Excelsior Installer support three compression levels: {@code fast}, {@code medium} (default) and {@code high} (slow).
     * </p>
     * The functionality is available for Excelsior JET 11.3 and above.
     */
    public String compressionLevel;

    /**
     * Installation directory configuration.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     *
     * @see InstallationDirectory#type
     * @see InstallationDirectory#path
     * @see InstallationDirectory#fixed
     */
    public InstallationDirectory installationDirectory;

    /**
     * (Windows) Registry key for installation.
     * <p>
     * During installation, Excelsior Installer creates a registry key to store information
     * required for the installation of update packages.
     * The key is located either in the HKEY_LOCAL_MACHINE/SOFTWARE/ or the HKEY_CURRENT_USER/SOFTWARE/ subtree,
     * depending on whether Common or Personal installation type gets selected during installation.
     * By default, the rest of the full name of the key is derived from the values of {@link JetProject#vendor},
     * {@link JetProject#product} and {@link JetProject#version} parameters:
     * {@code company-name/product-name/product-version}
     * </p>
     * The functionality is available for Excelsior JET 11.3 and above.
     */
    public String registryKey;

    /**
     * (Windows) Shortcuts descriptions which the resulting installer will create at specified locations.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     * @see Shortcut
     */
    public List<Shortcut> shortcuts;

    /**
     * Upon successful installation, Excelsior Installer can optionally display to the user a list of checkboxes
     * enabling various post-install actions, such as launching the installed application,
     * viewing the readme file, restarting the system, and so on.
     * The default is to add a launch action for each JET-compiled executable in the package with the text
     * "Start executable-name".
     * If you do not want to add the default action, set this paramter to {@code false}.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     * @see #postInstallCheckboxes
     */
    public Boolean noDefaultPostInstallActions;

    /**
     * Post install actions descriptions.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     * @see #noDefaultPostInstallActions
     */
    public List<PostInstallCheckbox> postInstallCheckboxes;

    /**
     * File associations descriptions.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     * @see FileAssociation
     */
    public List<FileAssociation> fileAssociations;

    /**
     * Install callback dynamic library.
     * If not set, and the file {@code install.dll/libinstall.so} in {@link JetProject#jetResourcesDir} folder exists,
     * that file is used by convention.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     */
    public File installCallback;

    /**
     * Uninstall callback dynamic library.
     * <p>
     * An uninstall callback dynamic library has to be present on the end user system at the time of uninstall,
     * so you need to specify its location in the project.
     * You may omit {@link PackageFile#path} parameter of the uninstallCallback,
     * if {@link JetProject#packageFilesDir} already contains a library at the specified {@link PackageFile#packagePath}
     * parameter else the library will be added to the package to the specified {@link PackageFile#packagePath} folder.
     * If the file {@code uninstall.dll/libuninstall.so} in {@link JetProject#jetResourcesDir} folder exists,
     * that file is used by convention.
     * </p>
     * The functionality is available for Excelsior JET 11.3 and above.
     */
    public PackageFile uninstallCallback;

    /**
     * (Windows) Image to display on the first screen of the installation wizard. Recommended size: 177*314px.
     * If not set, and the file {@code welcomeImage.bmp} in {@link JetProject#jetResourcesDir} folder exists,
     * that file is used by convention.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     */
    public File welcomeImage;

    /**
     * (Windows) Image to display in the upper-right corner on subsequent screens. Recommended size: 109*59px.
     * If not set, and the file {@code installerImage.bmp} in {@link JetProject#jetResourcesDir} folder exists,
     * that file is used by convention.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     */
    public File installerImage;

    /**
     * (Windows) Image to display on the first screen of the uninstall wizard. Recommended size: 177*314px.
     * If not set, and the file {@code uninstallerImage.bmp} in {@link JetProject#jetResourcesDir} folder exists,
     * that file is used by convention.
     * <p>
     * The functionality is available for Excelsior JET 11.3 and above.
     * </p>
     */
    public File uninstallerImage;

    public void fillDefaults(JetProject project, ExcelsiorJet excelsiorJet) throws JetTaskFailureException {
        //check eula settings
        if (!VALID_EULA_ENCODING_VALUES.contains(eulaEncoding)) {
            throw new JetTaskFailureException(s("JetApi.Package.Eula.UnsupportedEncoding", eulaEncoding));
        }

        if (eula == null) {
            eula = new File(project.jetResourcesDir(), "eula.txt");
        } else if (!eula.exists()) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.FileDoesNotExist", eula, "eula"));
        }

        if (installerSplash == null) {
            installerSplash = new File(project.jetResourcesDir(), "installerSplash.bmp");
        } else if (!installerSplash.exists()) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.FileDoesNotExist", installerSplash, "installerSplash"));
        }

        if (!excelsiorJet.since11_3()) {
            String parameter = null;
            if (language != null) {
                parameter = "language";
            } else if (!afterInstallRunnable.isEmpty()) {
                parameter = "afterInstallRunnable";
            } else if (compressionLevel != null) {
                parameter = "compressionLevel";
            } else if (!installationDirectory.isEmpty()) {
                parameter = "installationDirectory";
            } else if (!shortcuts.isEmpty()) {
                parameter = "shortcuts";
            } else if (cleanupAfterUninstall != null) {
                parameter = "cleanupAfterUninstall";
            } else if (registryKey != null) {
                parameter = "registryKey";
            } else if (noDefaultPostInstallActions != null) {
                parameter = "noDefaultPostInstallActions";
            } else if (!postInstallCheckboxes.isEmpty()) {
                parameter = "postInstallCheckboxes";
            } else if (!fileAssociations.isEmpty()) {
                parameter = "fileAssociations";
            } else if (installCallback != null) {
                parameter = "installCallback";
            } else if (!uninstallCallback.isEmpty()) {
                parameter = "uninstallCallback";
            } else if (welcomeImage != null) {
                parameter = "welcomeImage";
            } else if (installerImage != null) {
                parameter = "installerImage";
            } else if (uninstallerImage != null) {
                parameter = "uninstallerImage";
            }
            if (parameter != null) {
                throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.Since11_3Parameter", parameter));
            }
        }

        if ((language != null) && (SetupLanguage.fromString(language) == null)) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.UnsupportedLanguage", language));
        }

        if (cleanupAfterUninstall == null) {
            cleanupAfterUninstall = false;
        }

        if (!afterInstallRunnable.isEmpty()) {
            afterInstallRunnable.validate();
        }

        if ((compressionLevel != null) && (SetupCompressionLevel.fromString(compressionLevel) == null)) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.UnknownCompressionLevel", compressionLevel));
        }

        if (!installationDirectory.isEmpty()) {
            installationDirectory.validate(excelsiorJet);
        }

        for (Shortcut shortcut: shortcuts) {
            shortcut.validate();
        }

        if (noDefaultPostInstallActions == null) {
            noDefaultPostInstallActions = false;
        }

        for (PostInstallCheckbox postInstallCheckbox: postInstallCheckboxes) {
            postInstallCheckbox.validate();
        }

        for (FileAssociation fileAssociation: fileAssociations) {
            fileAssociation.validate();
        }

        if (installCallback == null) {
            installCallback = new File(project.jetResourcesDir(), excelsiorJet.getTargetOS().mangleDllName("install"));
        } else if (!installCallback.exists()) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.FileDoesNotExist", installCallback, "installCallback"));
        }

        if (uninstallCallback.isEmpty()) {
            File uninstall = new File(project.jetResourcesDir(), excelsiorJet.getTargetOS().mangleDllName("uninstall"));
            if (uninstall.exists()) {
                uninstallCallback.path = uninstall;
            }
        }
        uninstallCallback.validate("JetApi.ExcelsiorInstaller.FileDoesNotExist", "uninstallCallback");

        if (welcomeImage == null) {
            welcomeImage = new File(project.jetResourcesDir(), "welcomeImage.bmp");
        } else if (!welcomeImage.exists()) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.FileDoesNotExist", welcomeImage, "welcomeImage"));
        }

        if (installerImage == null) {
            installerImage = new File(project.jetResourcesDir(), "installerImage.bmp");
        } else if (!installerImage.exists()) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.FileDoesNotExist", installerImage, "installerImage"));
        }

        if (uninstallerImage == null) {
            uninstallerImage = new File(project.jetResourcesDir(), "uninstallerImage.bmp");
        } else if (!uninstallerImage.exists()) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.FileDoesNotExist", uninstallerImage, "uninstallerImage"));
        }
    }

    public String eulaFlag() throws JetTaskFailureException {
        String actualEncoding;
        try {
            actualEncoding = detectEncoding(eula);
        } catch (IOException e) {
            throw new JetTaskFailureException(e.getMessage(), e);
        }

        if (!AUTO_DETECT_EULA_ENCODING.equals(eulaEncoding)) {
            if (!actualEncoding.equals(eulaEncoding)) {
                throw new JetTaskFailureException(s("JetApi.Package.Eula.EncodingDoesNotMatchActual", actualEncoding, eulaEncoding));
            }
        }

        if (StandardCharsets.UTF_16LE.name().equals(actualEncoding)) {
            return UNICODE_EULA_FLAG;
        } else if (StandardCharsets.US_ASCII.name().equals(actualEncoding)) {
            return EULA_FLAG;
        } else {
            throw new JetTaskFailureException(s("JetApi.Package.Eula.UnsupportedEncoding", eulaEncoding));
        }
    }


}
