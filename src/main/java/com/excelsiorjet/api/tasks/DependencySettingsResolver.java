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

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The class assigns dependency settings to project dependencies, resulting in the creation of a respective
 * {@link ClasspathEntry}.
 *
 * @author Aleksey Zhidkov
 */
class DependencySettingsResolver {

    private final OptimizationPreset optimizationPreset;
    private final String projectGroupId;
    private final List<DependencySettings> dependencySettingsList;

    DependencySettingsResolver(OptimizationPreset optimizationPreset, String projectGroupId, List<DependencySettings> dependencySettingsList) {
        this.optimizationPreset = optimizationPreset;
        this.projectGroupId = projectGroupId;
        this.dependencySettingsList = dependencySettingsList;
    }

    /**
     * Produces a classpath entry for the given dependency using matching settings specified in {@code dependencySettingsList} and
     * set defaults for {@link ClasspathEntry#protect} and {@link ClasspathEntry#optimize} if {@link DependencySettings#isLibrary},
     * {@link DependencySettings#protect} and {@link DependencySettings#optimize} are not set for all matched dependency settings.
     */
    ClasspathEntry resolve(ProjectDependency dep) {
        return toClasspathEntry(dep);
    }

    private ClasspathEntry toClasspathEntry(ProjectDependency projectDependency) {
        TreeSet<DependencySettings> dependencySettings = settingsFor(projectDependency);
        DependencySettings resolvedSettings = dependencySettings.stream().
                reduce(new DependencySettings(projectDependency.groupId, projectDependency.artifactId, projectDependency.version, projectDependency.path), this::copyNonNullSettings);
        if (resolvedSettings.isLibrary == null) {
            resolvedSettings.isLibrary = !projectGroupId.equals(projectDependency.groupId);
        }
        if (resolvedSettings.protect == null) {
            resolvedSettings.protect = optimizationPreset.getDefaultProtectionType(resolvedSettings.isLibrary).userValue;
        }
        if (resolvedSettings.optimize == null) {
            resolvedSettings.optimize = optimizationPreset.getDefaultOptimizationType(resolvedSettings.isLibrary).userValue;
        }
        return new ClasspathEntry(resolvedSettings, projectDependency.isMainArtifact);
    }

    private DependencySettings copyNonNullSettings(DependencySettings to, DependencySettings from ) {
        if (from.isLibrary != null) {
            to.isLibrary = from.isLibrary;
        }
        if (from.protect != null) {
            to.protect = from.protect;
        }
        if (from.optimize != null) {
            to.optimize = from.optimize;
        }
        if (from.pack != null) {
            to.pack = from.pack;
        }
        if (from.packagePath != null) {
            to.packagePath = from.packagePath;
        }
        if (from.disableCopyToPackage != null) {
            to.disableCopyToPackage = from.disableCopyToPackage;
        }
        return to;
    }

    boolean hasSettingsFor(ProjectDependency dep) {
        return matchedSettings(dep).count() > 0;
    }

    private TreeSet<DependencySettings> settingsFor(ProjectDependency dep) {
        return matchedSettings(dep).
                collect(Collectors.toCollection(() -> new TreeSet<>(new DependencySettingsPriorityComparator())));
    }

    private Stream<DependencySettings> matchedSettings(ProjectDependency dep) {
        return dependencySettingsList.stream().
                filter(setting -> setting.matches(dep));
    }

}
