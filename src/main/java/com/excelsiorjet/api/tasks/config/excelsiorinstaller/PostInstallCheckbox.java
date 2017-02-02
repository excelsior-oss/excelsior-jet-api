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

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * (Windows) Post install checkbox description.
 *
 * @author Nikita Lipsky
 */
public class PostInstallCheckbox {

    /**
     * Post install checkbox type.
     * Valid values are {@code run} (default), {@code open}, {@code restart}.
     */
    public String type;

    /**
     * Whether the checkbox should be checked by default.
     * Default value is {@code true}.
     */
    public boolean checked = true;

    /**
     * Location of the post install checkbox action target within the package.
     * Not valid for the {@code restart} {@link #type}.
     */
    public String target;

    /**
     * Pathname of the working directory of the target within the package.
     * If not set, the directory containing target will be used.
     * Valid for the {@code run} type only.
     */
    public String workingDirectory;

    /**
     * Command-line arguments for the target.
     * Valid for the {@code run} type only.
     */
    public String[] arguments;

    public PostInstallActionType type() {
        return PostInstallActionType.fromString(type);
    }

    public String checkedArg() {
        return checked? "checked" : "unchecked";
    }

    void validate() throws JetTaskFailureException {
        if (type == null) {
            type = PostInstallActionType.RUN.toString();
        } else {
            PostInstallActionType.validate(type);
        }

        if ((type() != PostInstallActionType.RESTART) && (target == null)) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.PostInstallActionTargetNull"));
        } else if (type() == PostInstallActionType.RESTART && (target != null)) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.PostInstallActionTargetNotNullForRestart"));
        }

        if (type() != PostInstallActionType.RUN) {
            if (workingDirectory != null) {
                throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.NotRunPostInstallActionParameter", "workingDirectory", target));
            }
            if (!Utils.isEmpty(arguments)) {
                throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.NotRunPostInstallActionParameter", "arguments", target));
            }
        }
    }
}
