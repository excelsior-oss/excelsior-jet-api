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
 * Description of managed (i.e. Maven or Gradle) project dependency.
 *
 * @author Aleksey Zhidkov
 */
public class ProjectDependency {

    public final String groupId;

    public final String artifactId;

    public final String version;

    public final File path;

    public final boolean isMainArtifact;

    public ProjectDependency(String groupId, String artifactId, String version, File path, boolean isMainArtifact) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.path = path;
        this.isMainArtifact = isMainArtifact;
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    /**
     * @return String in format "([groupId],[artifactId],[version],[path])" (all fields are optional)
     */
    public String idStr(boolean addPath) {
        return Utils.idStr(groupId(), artifactId(), version(), addPath? path: null);
    }

    public String toString() {
        return idStr(true);
    }
}
