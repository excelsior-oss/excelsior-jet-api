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

package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.tasks.config.DependencySettings;
import com.excelsiorjet.api.tasks.config.ProjectDependency;
import com.excelsiorjet.api.util.Utils;

import java.io.File;

/**
 * Representation of Excelsior JET's project classpath entry: !classpathentry for plain Java applications,
 * !classloaderentry for Tomcat web applications.
 *
 * Classpath entry is created when a dependency settings specified by a plugin users
 * are resolved for a project dependency using {@link DependencySettingsResolver}.
 *
 * @see DependencySettings
 * @see ProjectDependency
 * @see DependencySettingsResolver
 *
 * @author Aleksey Zhidkov
 */
public class ClasspathEntry {

    public final File path;

    public final ProtectionType protect;

    public final OptimizationType optimize;

    public final PackType pack;

    public final String packagePath;

    public final Boolean disableCopyToPackage;

    public final boolean isMainArtifact;

    public ClasspathEntry(DependencySettings dependencySettings, boolean isMainArtifact) {
        this(dependencySettings.path, deriveProtect(dependencySettings), deriveOptimize(dependencySettings), PackType.fromString(dependencySettings.pack), dependencySettings.packagePath, dependencySettings.disableCopyToPackage, isMainArtifact);
    }

    private static ProtectionType deriveProtect(DependencySettings dependencySettings) {
        if (dependencySettings.protect == null && dependencySettings.isLibrary != null) {
            return dependencySettings.isLibrary ? ProtectionType.NOT_REQUIRED : ProtectionType.ALL;
        } else {
            return ProtectionType.fromString(dependencySettings.protect);
        }
    }

    private static OptimizationType deriveOptimize(DependencySettings dependencySettings) {
        if (dependencySettings.optimize == null && dependencySettings.isLibrary != null) {
            return dependencySettings.isLibrary ? OptimizationType.AUTO_DETECT : OptimizationType.ALL;
        } else {
            return OptimizationType.fromString(dependencySettings.optimize);
        }
    }

    private ClasspathEntry(File path, ProtectionType protect, OptimizationType optimize, PackType pack, String packagePath, Boolean disableCopyToPackage, boolean isMainArtifact) {
        this.path = path;
        this.protect = protect;
        this.optimize = optimize;
        this.pack = pack;
        if (packagePath != null) {
            this.packagePath = packagePath;
        } else {
            this.packagePath = isMainArtifact ? "" : null;
        }
        this.disableCopyToPackage = disableCopyToPackage;
        this.isMainArtifact = isMainArtifact;
    }

    /**
     * Classpath entry optimization type.
     */
    public enum OptimizationType {

        ALL("all", "all"),
        AUTO_DETECT("auto-detect", "autodetect");

        public final String userValue;
        public final String jetValue;

        OptimizationType(String userValue, String jetValue) {
            this.userValue = userValue;
            this.jetValue = jetValue;
        }

        public static OptimizationType fromString(String optimizationType) {
            if (optimizationType == null) {
                return null;
            }
            try {
                return OptimizationType.valueOf(Utils.parameterToEnumConstantName(optimizationType));
            } catch (Exception e) {
                return null;
            }
        }

    }

    /**
     * Classpath entry pack type.
     */
    public enum PackType {

        NONE("none", "none"),
        AUTO_DETECT("auto-detect", "noncompiled"),
        ALL("all", "all");

        public final String userValue;
        public final String jetValue;

        PackType(String userValue, String jetValue) {
            this.userValue = userValue;
            this.jetValue = jetValue;
        }

        public static PackType fromString(String packType) {
            if (packType == null) {
                return null;
            }
            try {
                return PackType.valueOf(Utils.parameterToEnumConstantName(packType));
            } catch (Exception e) {
                return null;
            }
        }

    }

    /**
     * Classpath entry protection type.
     */
    public enum ProtectionType {

        ALL("all", "all"),
        NOT_REQUIRED("not-required", "nomatter");

        public final String userValue;
        public final String jetValue;

        ProtectionType(String userValue, String jetValue) {
            this.userValue = userValue;
            this.jetValue = jetValue;
        }

        public static ProtectionType fromString(String protectType) {
            if (protectType == null) {
                return null;
            }
            try {
                return ProtectionType.valueOf(Utils.parameterToEnumConstantName(protectType));
            } catch (Exception e) {
                return null;
            }
        }

    }
}
