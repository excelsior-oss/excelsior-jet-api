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

import com.excelsiorjet.api.tasks.ApplicationType;
import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.tasks.PackagingType;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * Configuration parameters of Windows Services.
 *
 * @see com.excelsiorjet.api.tasks.ApplicationType#WINDOWS_SERVICE
 *
 * @author Nikita Lipsky
 */
public class WindowsServiceConfig {

    public enum LogOnType {
        LOCAL_SYSTEM_ACCOUNT,
        USER_ACCOUNT;

        public String toString() {
            return Utils.enumConstantNameToParameter(name());
        }

        public static LogOnType fromString(String logOnType) {
            try {
                return LogOnType.valueOf(Utils.parameterToEnumConstantName(logOnType));
            } catch (Exception e) {
                return null;
            }
        }
    }

    public enum StartupType {
        AUTOMATIC("auto"),
        MANUAL("manual"),
        DISABLED("disabled");

        private String xpackValue;

        StartupType(String xpackValue) {
            this.xpackValue = xpackValue;
        }

        public String toString() {
            return Utils.enumConstantNameToParameter(name());
        }

        public static StartupType fromString(String startupType) {
            try {
                return StartupType.valueOf(Utils.parameterToEnumConstantName(startupType));
            } catch (Exception e) {
                return null;
            }
        }

        public String toISrvCmdFlag() {
            return "-" + xpackValue;
        }

        public String toXPackValue() {
            return xpackValue;
        }
    }

    /**
     * The system name of the service.
     * It is used to install, remove and otherwise manage the service.
     * It can also be used to recognize messages from this service in the system event log.
     * This name is set during the creation of the service executable.
     * It is displayed for reference so you cannot edit it.
     * <p>
     * By default, {@link JetProject#outputName} is used for the name.
     * </p>
     */
    public String name;

    /**
     * The descriptive name of the service.
     * It is shown in the Event Viewer system tool and in the Services applet of the Windows Control Panel.
     * <p>
     * By default, {@link #name} is used for the dispaly name.
     * </p>
     */
    public String displayName;

    /**
     * The user description of the service. It must not exceed 1000 characters.
     */
    public String description;

    /**
     * The command line arguments passed to the service upon startup.
     */
    public String[] arguments;

    /**
     * Specifies an account to be used by the service. Valid values are: "local-system-account" (default), "user-account".
     * <p>
     * {@code local-system-account} - run the service under the built-in system account.
     * </p>
     * <p>
     * {@code user-account} - run the service under a user account.
     *                        When installing the package, the user will be prompted for an account name
     *                        and password necessary to run the service.
     * </p>
     */
    public String logOnType;

    /**
     * Specifies if the service needs to interact with the system desktop,
     * e.g. open/close other windows, etc. This option is only available under the "local-system-account".
     */
    public boolean allowDesktopInteraction;

    /**
     * Specify how to start the service. Valid values are "automatic" (default), "manual", "disabled".
     * <p>
     * {@code automatic} - specifies that the service should start automatically when the system starts.
     * </p>
     * <p>
     * {@code manual} - specifies that a user or a dependent service can start the service.
     *                  Services with Manual startup type do not start automatically when the system starts.
     * </p>
     * <p>
     * {@code disabled} - prevents the service from being started by the system, a user, or any dependent service.
     * </p>
     */
    public String startupType;

    /**
     * Specifies if the service should be started immediately after installation.
     *
     * Available only for {@link PackagingType#EXCELSIOR_INSTALLER} packaging type.
     *
     */
    public boolean startServiceAfterInstall = true;

    /**
     * List of other service names on which the service depends.
     */
    public String[] dependencies;

    public void fillDefaults(JetProject jetProject) throws JetTaskFailureException {
        if (Utils.isEmpty(name)) {
            name = jetProject.outputName();
        }

        if (Utils.isEmpty(displayName)) {
            displayName = jetProject.appType() == ApplicationType.TOMCAT ? "Apache Tomcat": name;
        }

        if (description == null) {
            description = jetProject.appType() == ApplicationType.TOMCAT ?
                    "Apache Tomcat Server - http://tomcat.apache.org/": displayName;
        }

        if (logOnType == null) {
            logOnType = LogOnType.LOCAL_SYSTEM_ACCOUNT.toString();
        } else if (LogOnType.fromString(logOnType) == null) {
            throw new JetTaskFailureException(s("JetApi.UnknownLogOnType.Failure", logOnType));
        }
        if (startupType == null) {
            startupType = StartupType.AUTOMATIC.toString();
        } else if (StartupType.fromString(startupType) == null) {
            throw new JetTaskFailureException(s("JetApi.UnknownStartupType.Failure", startupType));
        }

        if (allowDesktopInteraction && (LogOnType.fromString(logOnType) != LogOnType.LOCAL_SYSTEM_ACCOUNT)) {
            throw new JetTaskFailureException(s("JetApi.IntractiveSeviceForNonLocalSystem.Failure", startupType));
        }
    }

    public LogOnType getLogOnType() {
        return LogOnType.fromString(logOnType);
    }

    public StartupType getStartupType() {
        return StartupType.fromString(startupType);
    }
}
