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

import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Windows version-information resource description.
 *
 * @author Nikita Lipsky
 */
public class WindowsVersionInfoConfig {

    /**
     * Company name for the version-information resource.
     *
     * By default, {@link JetProject#vendor} is used.
     */
    public String company;

    /**
     * Product name for the version-information resource.
     *
     * By default, {@link JetProject#product} is used.
     */
    public String product;

    /**
     * Version number string for the version-information resource.
     * (Both {@code ProductVersion} and {@code FileVersion} resource strings are set to the same value.)
     * Must have {@code v1.v2.v3.v4} format where {@code vi} is a number.
     * If not set, {@code ${project.version}} is used. If the value does not meet the required format,
     * it is coerced. For instance, "1.2.3-SNAPSHOT" becomes "1.2.3.0"
     */
    public String version;

    /**
     * Legal copyright notice string for the version-information resource.
     * By default, {@code "Copyright © {$project.inceptionYear},[curYear] [vendor]"} is used.
     */
    public String copyright;

    /**
     * File description string for the version-information resource.
     *
     * The value of {@link #product} is used by default.
     */
    public String description;

    public void fillDefaults(JetProject jetProject) {
        if (company == null) {
            company = jetProject.vendor();
        }

        if (product == null) {
            product = jetProject.product();
        }

        if (version == null) {
            version = jetProject.version();
        }

        //Coerce winVIVersion to v1.v2.v3.v4 format.
        String finalVersion = Utils.deriveFourDigitVersion(version);
        if (!version.equals(finalVersion)) {
            logger.warn(s("JetApi.NotCompatibleExeVersion.Warning", version, finalVersion));
            version = finalVersion;
        }

        if (copyright == null) {
            String inceptionYear = jetProject.inceptionYear();
            String curYear = new SimpleDateFormat("yyyy").format(new Date());
            String years = Utils.isEmpty(inceptionYear) ? curYear : inceptionYear + "," + curYear;
            copyright = "Copyright \\x00a9 " + years + " " + company;
        }
        if (description == null) {
            description = product;
        }
    }
}
