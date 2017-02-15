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

import com.excelsiorjet.api.tasks.config.dependencies.DependencySettings;

import java.util.Comparator;

/**
 * Comparator of {@code DependencySettings} instances that compares them in priority order, from lower to higher.
 * <p>
 * List of all possible combinations of dependency id components:
 * <p>
 * <ul>
 * <li>--- (no components)</li>
 * <li>g--</li>
 * <li>-a-</li>
 * <li>--v</li>
 * <li>ga-</li>
 * <li>-av</li>
 * <li>g-v</li>
 * <li>gav (group, artifact, version)</li>
 * </ul>
 * <p>
 * General rules:
 * <ol>
 * <li>Settings instances that have neither group nor artifact are invalid (---, --v);</li>
 * <li>If a settings instance has artifact, it has higher priority;</li>
 * <li>If a settings instance has version, it has higher priority.</li>
 * </ol>
 * <p>
 * Comparison matrix for valid settings ("^" means that upper settings has higher priority, "<" means that left settings has higher priority)
 * <pre>
 *     g-- -a- ga- -av g-v gav
 * g-- =x   ^   ^   ^   ^   ^
 * -a- =====x   ^   ^   <   ^
 * ga- =========x   ^   <   ^
 * -av =============x   <   ^
 * g-v =================x   ^
 * gav =====================x
 * </pre>
 *
 * @author Aleksey Zhidkov
 */
class DependencySettingsPriorityComparator implements Comparator<DependencySettings> {

    @Override
    public int compare(DependencySettings o1, DependencySettings o2) {
        if (o1 == o2) {
            return 0;
        }
        boolean dep1HasFullId = hasFullId(o1);
        boolean dep2HasFullId = hasFullId(o2);
        if (dep1HasFullId ^ dep2HasFullId) {
            return dep1HasFullId ? 1 : -1;
        }
        if (o1.artifactId == null ^ o2.artifactId == null) {
            return o1.artifactId != null ? 1 : -1;
        }
        // at this point both dependencies either have or not artifact id
        boolean dep1HasVersion = o1.version != null;
        boolean dep2HasVersion = o2.version != null;
        if (dep1HasVersion ^ dep2HasVersion) {
            return dep1HasVersion ? 1 : -1;
        }
        // at this point both either have or not artifact id and version
        boolean dep1HasGroup = o1.groupId != null;
        boolean dep2HasGroup = o2.groupId != null;
        if (dep1HasGroup ^ dep2HasGroup) {
            return dep1HasGroup ? 1 : -1;
        }
        throw new AssertionError("There should not be settings with equal ids");
    }

    private boolean hasFullId(DependencySettings d) {
        return d.groupId != null && d.artifactId != null && d.version != null;
    }

}
