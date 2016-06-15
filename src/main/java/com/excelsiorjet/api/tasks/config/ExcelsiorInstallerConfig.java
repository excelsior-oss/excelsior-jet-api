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

import com.excelsiorjet.api.tasks.JetProject;
import com.excelsiorjet.api.tasks.JetTaskFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.excelsiorjet.api.util.EncodingDetector.detectEncoding;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Configuration parameters for Excelsior Installer packaging type.
 *
 * @author Nikita Lipsky
 */
public class ExcelsiorInstallerConfig {

    private static final String AUTO_DETECT_EULA_ENCODING = "autodetect";
    private static final String UNICODE_EULA_FLAG = "-unicode-eula";
    private static final String EULA_FLAG = "-eula";

    private static final Set<String> VALID_EULA_ENCODING_VALUES = new LinkedHashSet<String>() {{
        add(StandardCharsets.US_ASCII.name());
        add(StandardCharsets.UTF_16LE.name());
        add(AUTO_DETECT_EULA_ENCODING);
    }};


    /**
     * The license agreement file. Used for Excelsior Installer.
     * File containing the end-user license agreement, for Excelsior Installer to display during installation.
     * The file must be a plain text file either in US-ASCII or UTF-16LE encoding.
     * If not set, and the file {@code ${project.basedir}/src/main/jetresources/eula.txt} exists,
     * that file is used by convention.
     *
     * @see #eulaEncoding eulaEncoding
     */
    public File eula;

    /**
     * Encoding of the EULA file. Permitted values:
     * <ul>
     *     <li>{@code US-ASCII}</li>
     *     <li>{@code UTF-16LE}</li>
     *     <li>{@code autodetect} (Default value)</li>
     * </ul>
     * If set to {@code autodetect}, the plugin looks for a byte order mark (BOM) in the file specified by {@link #eula}, and:
     * <ul>
     * <li>assumes US-ASCII encoding if no BOM is present,</li>
     * <li>assumes UTF-16LE encoding if the respective BOM ({@code 0xFF 0xFE}) is present, or </li>
     * <li>halts execution with error if some other BOM is present.</li>
     * </ul>
     * @see <a href="https://en.wikipedia.org/wiki/Byte_order_mark">Byte order mark</a>
     * @see #eula eula
     */
    protected String eulaEncoding = AUTO_DETECT_EULA_ENCODING;

    /**
     * (Windows) Excelsior Installer splash screen image in BMP format.
     * If not set, and the file {@code ${project.basedir}/src/main/jetresources/installerSplash.bmp} exists,
     * that file is used by convention.
     */
    public File installerSplash;

    public void fillDefaults(JetProject config) throws JetTaskFailureException {
        //check eula settings
        if (!VALID_EULA_ENCODING_VALUES.contains(eulaEncoding)) {
            throw new JetTaskFailureException(s("JetApi.Package.Eula.UnsupportedEncoding", eulaEncoding));
        }

        if (eula == null) {
            eula = new File(config.basedir(), "src/main/jetresources/eula.txt");
        }

        if (installerSplash == null) {
            installerSplash = new File(config.basedir(), "src/main/jetresources/installerSplash.bmp");
        }
    }

    public String eulaFlag() throws JetTaskFailureException, IOException {
        String actualEncoding;
        actualEncoding = detectEncoding(eula);

        if (!AUTO_DETECT_EULA_ENCODING.equals(eulaEncoding)) {
            if (!actualEncoding.equals(eulaEncoding)) {
                throw new JetTaskFailureException(s("JetApi.Package.Eula.EncodingDoesNotMatchActual", actualEncoding, eulaEncoding));
            }
        }

        if (StandardCharsets.UTF_16LE.name().equals(actualEncoding)) {
            return UNICODE_EULA_FLAG;
        } else if (StandardCharsets.US_ASCII.name().equals(actualEncoding)) {
            return EULA_FLAG;
        } else {
            throw new JetTaskFailureException(s("JetApi.Package.Eula.UnsupportedEncoding", eulaEncoding));
        }
    }


}
