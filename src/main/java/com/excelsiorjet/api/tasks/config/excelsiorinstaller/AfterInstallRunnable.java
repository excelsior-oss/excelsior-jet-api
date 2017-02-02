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
 * After-install runnable description.
 *
 * @author Nikita Lipsky
 */
public class AfterInstallRunnable {

    /**
     * Location of the after-install runnable within the package.
     */
    public String target;

    /**
     * Command-line arguments for {@code target}.
     */
    public String[] arguments;

    public boolean isEmpty() {
        return (target == null) && Utils.isEmpty(arguments);
    }

    void validate() throws JetTaskFailureException {
        if (target == null) {
            throw new JetTaskFailureException(s("JetApi.ExcelsiorInstaller.AfterInstallRunnableTargetNull"));
        }
    }

}
