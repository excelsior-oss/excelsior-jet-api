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
package com.excelsiorjet.api.tasks.config;

/**
 * (Winodws) Post install checkbox description.
 *
 * @author Nikita Lipsky
 */
public class PostInstallCheckbox {

    /**
     * Post install checkbox type.
     * Valid values are {@code run}, {@code open}, {@code restart}.
     */
    public String type;

    /**
     * Whether the checkbox should be checked by default.
     * Default value is {@code true}
     */
    public boolean checked = true;

    /**
     * Location of the post install checkbox action target within the package.
     * Not valid for {@code restart} {@link #type}.
     */
    public String target;

    /**
     * Pathname of the working directory of the  target within the package.
     * If it is not set, the the directory containing target will be used.
     * Valid for {@code run} type only.
     */
    public String workingDirectory;

    /**
     * Command line arguments for the target.
     * Valid for {@code run} type only.
     */
    public String[] arguments;
}
