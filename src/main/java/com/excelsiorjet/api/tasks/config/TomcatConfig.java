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
package com.excelsiorjet.api.tasks.config;

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.util.Utils;

import java.io.File;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * Tomcat web applications specific parameters
 */
public class TomcatConfig {

    public static final String WEBAPPS_DIR = "webapps";

    /**
     * The location of the master Tomcat application server installation,
     * a required parameter for Web application projects.
     * Your .war artifact will be deployed into a copy of the master Tomcat installation and compiled together with it.
     *
     * You may also use the tomcat.home system property, or one of TOMCAT_HOME and CATALINA_HOME environment variables
     * to set this parameter.
     */
    public String tomcatHome;

    /**
     * The name of the war file to be deployed into Tomcat.
     * Default value is the name of your main artifact, which should be a war file.
     * <p>
     * By default, Tomcat uses the war file name as the context path of the respective Web application.
     * If you need your Web application to be on the "/" context path,
     * set warDeployName to "ROOT" value.
     * </p>
     */
    public String warDeployName;

    /**
     * If you do not want your end users to inspect or modify the Tomcat configuration files
     * located in &lt;tomcatHome&gt;/conf/, set this plugin parameter to {@code true}
     * to have them placed into the executable and not appear in the conf/ subdirectory
     * of end user installations of your Web application.
     */
    public boolean hideConfig;

    /**
     * If you want to continue using standard Tomcat scripts such as {@code startup} and {@code shutdown},
     * with the natively compiled Tomcat, set this plugin parameter to {@code true} (default).
     * As a result, the scripts working with the compiled Tomcat will be created in "target/jet/app/bin"
     * along with the executable.
     */
    public boolean genScripts = true;

    public void fillDefaults() throws JetTaskFailureException {
        // check Tomcat home
        if (Utils.isEmpty(tomcatHome)) {
            tomcatHome = System.getProperty("tomcat.home");
            if (Utils.isEmpty(tomcatHome)) {
                tomcatHome = System.getenv("TOMCAT_HOME");
                if (Utils.isEmpty(tomcatHome)) {
                    tomcatHome = System.getenv("CATALINA_HOME");
                }
            }
        }

        if (Utils.isEmpty(tomcatHome)) {
            throw new JetTaskFailureException(s("JetMojo.TomcatNotSpecified.Failure"));
        }

        if (!new File(tomcatHome).exists()) {
            throw new JetTaskFailureException(s("JetMojo.TomcatDoesNotExist.Failure", tomcatHome));
        }

        if (!new File(tomcatHome, WEBAPPS_DIR).exists()) {
            throw new JetTaskFailureException(s("JetMojo.TomcatWebappsDoesNotExist.Failure", tomcatHome));
        }

        if (!Utils.isEmpty(warDeployName) && !warDeployName.endsWith(".war")) {
            warDeployName = warDeployName + ".war";
        }
    }
}
