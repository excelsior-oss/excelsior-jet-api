/*
 * Copyright (c) 2017, Excelsior LLC.
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
package com.excelsiorjet.api.tasks.config.excelsiorinstaller;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.tasks.config.packagefile.PackageFile;
import com.excelsiorjet.api.tasks.config.packagefile.PackageFileType;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * (Windows) Shortcut description.
 *
 * @author Nikita Lipsky
 */
public class Shortcut {

    /**
     * Shortcut location.
     * Valid values are {@code program-folder}, {@code desktop}, {@code start-menu}, and {@code startup}.
     */
    public String location;

    /**
     * Location of the shortcut target within the package.
     */
    public String target;

    /**
     * Name of the shortcut. By default, the short name of the target is used.
     */
    public String name;

    /**
     * Location of the shortcut icon.
     * You may omit {@link PackageFile#path} parameter of the icon,
     * if {@link JetProject#packageFilesDir} already contains an icon at the specified {@link PackageFile#packagePath}
     * parameter, othwerwise the icon will be added to the the specified {@link PackageFile#packagePath} folder of the package
     * and used for the shortcut.
     * (If no icon is set for the shortcut and its {@code target} is a binary executable 
     * that has an icon, that icon will be used, otherwise the system default icon for 
     * the target file type will be used.)
     */
    public PackageFile icon;

    /**
     * Pathname of the working directory of the shortcut target within the package.
     * If not set, the directory containing the target will be used.
     */
    public String workingDirectory;

    /**
     * Command-line arguments for the target.
     */
    public String[] arguments;

    ShortcutLocationType location() {
        return ShortcutLocationType.fromString(location);
    }

    void validate(ExcelsiorJet excelsiorJet) throws JetTaskFailureException {
        if (name == null) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.ShortcutNameNull"));
        }

        if (location == null) {
            location = ShortcutLocationType.PROGRAM_FOLDER.toString();
        } else {
            ShortcutLocationType.validate(location, name);
        }

        if (target == null) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.ShortcutTargetNull", name));
        }

        if (!icon.isEmpty() && !excelsiorJet.isAdvancedExcelsiorInstallerFeaturesSupported()) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.ShortcutIconNotSupported", name));
        } else {
            icon.type = PackageFileType.FILE.toString(); //check that icon is file. Maven creates sub objects
                                                        // from scratch so we need to set type explicitly.
            icon.validate("JetApi.ExcelsiorInstaller.ShortcutIconDoesNotExist", name);
        }

        if (workingDirectory == null) {
            workingDirectory = "";
        }
    }
}
