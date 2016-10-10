/*
 * Copyright (c) 2016, Excelsior LLC.
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

import com.excelsiorjet.api.util.Utils;

import java.io.File;

/**
 * Excelsior JET specific dependency settings.
 *
 * @author Nikita Lipsky
 */
public class DependencySettings {

    /**
     * Dependency group id.
     * <p>
     * If {@link #artifactId}, {@link #version} are not set, all properties will be assigned to all dependencies
     * sharing this group id.
     * </p>
     */
    public String groupId;

    /**
     * Dependency artifact id.
     * <p>
     * If you are sure that there is the only dependency with a certain artifactId in the project,
     * you may omit {@link #groupId} and {@link #version} from the configuration,
     * </p>
     */
    public String artifactId;

    /**
     * Dependency version. Last part of dependency description.
     */
    public String version;

    /**
     * Path to an additional dependency.
     * <p>
     * If you need some additional dependency to appear in the application classpath that is not listed
     * in the project explicitly (for example, you need to access some resources in a directory via
     * {@code ResourceBundle.getResource()} you may
     * set this parameter instead of "groupId/artifactId/version" pointing to a directory or jar/zip.
     * </p>
     * You should not set this parameter if you set either of "groupId/artifactId/version".
     */
    public File path;

    /**
     * Code protection mode. Valid values are "all", and "not-required".
     * <p>
     * If set to "all", all class files of the dependency will be compiled to native code,
     * thus protecting them from reverse engineering.
     * </p>
     * If set to "not-required", the JET Optimizer may avoid compilation of some classes
     * in favor of reducing the download size and compilation time of the application.
     */
    public String protect;

    /**
     * Optimization mode. Valid values are "all" and "auto-detect".
     * <p>
     * If set to "all", all class files of the dependency are compiled to native code with all optimizations enabled.
     * Typically, this mode provides the best performance, however, it may negatively affect the size
     * of resulting executable and compilations time.
     * </p>
     * If set to "auto-detect", the compiler detects which classes will be used at run time,
     * and optimizes those classes only, leaving the not used code in bytecode or not optimized form.
     * This mode provides smaller binaries, but it may negatively affect application performance,
     * if the compiler assumptions fail.
     */
    public String optimize;

    /**
     * Hint telling the plugin if this dependency is third party library or your own.
     * <p>
     * If set to {@code true}, sets protection mode to "not-required" and optimization mode to "auto-detect".
     * </p>
     * <p>
     * If set to {@code false}, sets protection and optimization modes to "all".
     * </p>
     * If you set this property, you must not set "optimize" and "protect" at the same time.
     */
    public Boolean isLibrary;

    /**
     * Packing mode. Valid values are "none", "auto-detect", "all".
     * <p>
     * If set to "auto-detect", everything but compiled .class files is packed to the executable.
     * This is the default mode. Simply speaking, in this mode non-compiled class files and all resource files
     * are packed to the executable, and can be accessed by the application at run time.
     * </p>
     * <p>
     * If set to "all", all class and resource files are packed to the executable.
     * This makes sense when compiling JARs of security providers and other APIs
     * that require class files to be available at run time.
     * </p>
     * If set to "none", neither original class files, nor resource files are packed into the executable.
     * The dependency will be copied to the final package as is for this mode unless {@link #disableCopyToPackage}
     * is set to {@code true}. "none" is the only available mode for directories.
     *
     * @see #packagePath
     */
    public String pack;

    /**
     * Controls dependency appearance in the final package for dependencies that have "pack" parameter set to "none".
     * <p>
     * Default value is "lib" for jar dependencies and "/" (root of the package) for directories.
     * </p>
     */
    public String packagePath;

    /**
     * If set to {@code true} disables coping the dependency to the final package that has "pack" parameter set to "none".
     */
    public Boolean disableCopyToPackage;

    public DependencySettings() {
    }

    public DependencySettings(String groupId, String artifactId, String version, File path) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.path = path;
    }

    public DependencySettings(File path, String protect, String optimize) {
        this.path = path;
        this.protect = protect;
        this.optimize = optimize;
    }

    public String idStr() {
        return Utils.idStr(groupId, artifactId, version);
    }

    public boolean matches(ProjectDependency dep) {
        return (this.groupId == null || this.groupId.equals(dep.groupId)) &&
                (this.artifactId == null || this.artifactId.equals(dep.artifactId)) &&
                (this.version == null || this.version.equals(dep.version));
    }

    public boolean isArtifactOnly() {
        return artifactId != null && groupId == null && version == null;
    }

    public boolean isExternal() {
        return path != null;
    }

}
