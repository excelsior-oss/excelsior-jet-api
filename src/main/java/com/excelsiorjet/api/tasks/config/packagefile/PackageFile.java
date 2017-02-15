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
package com.excelsiorjet.api.tasks.config.packagefile;

import com.excelsiorjet.api.tasks.JetTaskFailureException;

import java.io.File;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * Description of a file within the resulting package.
 *
 * @author Nikita Lipsky
 */
public class PackageFile {

    /**
     * Package file type. Valid values are: {@code auto} (default), {@code file}, {@code folder}.
     */
    public String type = PackageFileType.AUTO.toString();

    /**
     * Path to the file on the host system.
     */
    public File path;

    /**
     * Location of the file in the resulting package.
     * If not set, the file will be added to the root installation directory.
     */
    public String packagePath;

    public PackageFile() {
    }

    public PackageFile(PackageFileType type) {
        this.type = type.toString();
    }

    public PackageFile(PackageFileType type, File path) {
        this.type = type.toString();
        this.path = path;
    }

    public PackageFile(PackageFileType type, File path, String packagePath) {
        this.type = type.toString();
        this.path = path;
        this.packagePath = packagePath;
    }

    public PackageFile(File path, String packagePath) {
        this.path = path;
        this.packagePath = packagePath;
    }

    public boolean isDefined() {
        return (path != null) || (packagePath != null);
    }

    /**
     * @return location of this file in the package including file name, or empty string if the file is not {@link #isDefined()}}
     */
    public String getLocationInPackage() {
        if (!isDefined()) {
            return "";
        } else if (path != null) {
            assert packagePath != null: "validate() must be called before";
            return packagePath.endsWith("/") ? packagePath + path.getName() : packagePath + "/" + path.getName();
        } else {
            return packagePath;
        }
    }

    public void validate(String notExistErrorKey, String errorParam) throws JetTaskFailureException {
        if (!isDefined())
            return;

        if (path != null) {
            if (!path.exists()) {
                throw new JetTaskFailureException(s(notExistErrorKey, path.getAbsolutePath(), errorParam));
            }
            switch (PackageFileType.validate(type)) {
                case FILE:
                    if (!path.isFile()) {
                        throw new JetTaskFailureException(s("JetApi.PackageFileNotFile.Error", path.getAbsolutePath()));
                    }
                    break;
                case FOLDER:
                    if (!path.isDirectory()) {
                        throw new JetTaskFailureException(s("JetApi.PackageFileNotFolder.Error", path.getAbsolutePath()));
                    }
                    break;
                case AUTO:
                    break;
                default:
                    throw new AssertionError("Unknown file type: " + type);
            }

        }

        if (packagePath == null) {
            packagePath = "/";
        }
    }

    public void validate(String notExistErrorKey) throws JetTaskFailureException {
        validate(notExistErrorKey, null);
    }
}
