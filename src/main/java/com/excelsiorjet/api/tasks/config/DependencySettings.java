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

import com.excelsiorjet.api.tasks.OptimizationPreset;
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
     * If {@link #artifactId} and {@link #version} are not set, all settings will be assigned to all dependencies
     * sharing this group id.
     * </p>
     */
    public String groupId;

    /**
     * Dependency artifact id.
     * <p>
     * If you are sure that there is only one dependency with the given {@code artifactId} in the project,
     * you may omit {@link #groupId} and {@link #version} from the configuration.
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
     * {@code ResourceBundle.getResource()}), you may
     * set this parameter to point to a directory or jar/zip file, <em>instead of</em>
     * setting {@link #groupId}, {@link #artifactId}, and/or {@link #version}.
     * </p>
     * You should not set this parameter if you set any of {@link #groupId}, {@link #artifactId}, or {@link #version}.
     */
    public File path;

    /**
     * Code protection mode. Valid values are {@code "all"} and {@code "not-required"}.
     * <p>
     * If set to {@code "all"}, all class files of the dependency will be compiled to native code,
     * thus protecting them from reverse engineering.
     * </p>
     * If set to {@code "not-required"}, the JET Optimizer may avoid compilation of some classes
     * in favor of reducing the download size and compilation time of the application.
     */
    public String protect;

    /**
     * Optimization mode. Valid values are {@code "all"} and {@code "auto-detect"}.
     * <p>
     * If set to {@code "all"}, all class files of the dependency are compiled to native code with all optimizations enabled.
     * Typically, this mode provides the best performance, however, it may negatively affect the size
     * of the resulting executable and compilation time.
     * </p>
     * If set to {@code "auto-detect"}, the compiler detects which classes will be used at run time,
     * and optimizes those classes only, leaving the unused ones in bytecode or non-optimized form.
     * This mode provides smaller binaries, but may negatively affect application performance,
     * should any of the compiler assumptions fail.
     */
    public String optimize;

    /**
     * A hint telling the plugin if this dependency is a third-party library or your own code.
     * The hint affects the default values of {@link #protect} and {@link #optimize} modes
     * when {@link OptimizationPreset#SMART} is enabled for the project:
     * <p>
     * If this property is set to {@code true}, {@link #protect} defaults to {@code "not-required"}
     * and {@link #optimize} to {@code "auto-detect"}.
     * </p>
     * <p>
     * If this property is set to {@code false}, both {@link #protect} and {@link #optimize} default
     * to {@code "all"}.
     * </p>
     * If you set this property for a dependency, you should not at the same time set
     * {@link #optimize} or {@link #protect} .
     */
    public Boolean isLibrary;

    /**
     * Packing mode. Valid values are {@code "none"}, {@code "auto-detect"}, and {@code "all"}.
     * <p>
     * If set to {@code "auto-detect"}, everything but the compiled {@code .class} files gets packed into the executable.
     * This is the default mode. Simply speaking, in this mode non-compiled class files and all resource files
     * are packed into the executable, and can be accessed by the application at run time.
     * </p>
     * <p>
     * If set to {@code "all"}, all class and resource files are packed to the executable.
     * This only makes sense when the depepandency is a jar file of a security provider
     * or some other API that requires its class files to be available at run time.
     * </p>
     * If set to "none", neither class nor resource files get packed into the executable.
     * In this mode, the dependency will be copied to the final package as is, unless {@link #disableCopyToPackage}
     * is set to {@code true}. {@code "none"} is the only available mode for directories.
     *
     * @see #packagePath
     */
    public String pack;

    /**
     * For the dependencies that have the {@link #pack} parameter set to {@code "none"}, controls their location in the final package.
     * <p>
     * The default value is "{@code lib}" for jar dependencies and "{@code /}" (root of the package) for directories.
     * </p>
     */
    public String packagePath;

    /**
     * If set to {@code true}, disables copying of dependencies that have the {@link #pack} parameter set to {@code "none"} to the final package 
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
        return Utils.idStr(groupId, artifactId, version, path);
    }

    public boolean matches(ProjectDependency dep) {
        return (this.groupId == null || this.groupId.equals(dep.groupId)) &&
                (this.artifactId == null || this.artifactId.equals(dep.artifactId)) &&
                (this.version == null || this.version.equals(dep.version)) &&
                (this.path == null || pathMatches(this.path, dep.path))
                ;
    }

    static private boolean pathMatches(File path1, File path2) {
         return (path2 != null) && Utils.getCanonicalPath(path1).equals(Utils.getCanonicalPath(path2));
    }

    public boolean isArtifactOnly() {
        return artifactId != null && groupId == null && version == null;
    }

    public boolean hasPathOnly() {
        return (path != null) && (groupId == null) && (artifactId == null) && (version == null);
    }

    public String toString() {
        return idStr();
    }
}
