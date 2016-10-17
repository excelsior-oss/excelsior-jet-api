/*
 * Copyright (c) 2015, Excelsior LLC.
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
package com.excelsiorjet.api;

import java.util.Arrays;

/**
 * Excelsior JET Edition enum.
 *
 * @author Nikita Lipsky
 */
public enum JetEdition {
    EVALUATION("Evaluation"),
    STANDARD("Standard Edition"),
    PROFESSIONAL("Professional Edition"),
    ENTERPRISE("Enterprise Edition"),
    EMBEDDED("Embedded Edition"),
    EMBEDDED_EVALUATION("Embedded Evaluation");

    private final String fullName;

    JetEdition(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Examples: "Enterprise Edition", "Embedded Evaluation".
     */
    public String fullEditionName() {
        return fullName;
    }

    static JetEdition retrieveEdition(String version) {
        return Arrays.stream(JetEdition.values())
                .filter(e -> version.contains(e.fullEditionName()))
                .findFirst()
                .orElse(null);
    }
}
