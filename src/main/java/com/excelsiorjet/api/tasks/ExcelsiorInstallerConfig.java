package com.excelsiorjet.api.tasks;

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
    protected File eula;

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
    protected File installerSplash;

    void fillDefaults(AbstractJetTaskConfig config) throws ExcelsiorJetApiException {
        //check eula settings
        if (!VALID_EULA_ENCODING_VALUES.contains(eulaEncoding)) {
            throw new ExcelsiorJetApiException(s("JetMojo.Package.Eula.UnsupportedEncoding", eulaEncoding));
        }

        if (eula == null) {
            eula = new File(config.basedir(), "src/main/jetresources/eula.txt");
        }

        if (installerSplash == null) {
            installerSplash = new File(config.basedir(), "src/main/jetresources/installerSplash.bmp");
        }
    }

    String eulaFlag() throws ExcelsiorJetApiException {
        String actualEncoding;
        try {
            actualEncoding = detectEncoding(eula);
        } catch (IOException e) {
            throw new ExcelsiorJetApiException(s("JetMojo.Package.Eula.UnableToDetectEncoding", eula.getAbsolutePath()), e);
        }

        if (!AUTO_DETECT_EULA_ENCODING.equals(eulaEncoding)) {
            if (!actualEncoding.equals(eulaEncoding)) {
                throw new ExcelsiorJetApiException(s("JetMojo.Package.Eula.EncodingDoesNotMatchActual", actualEncoding, eulaEncoding));
            }
        }

        if (StandardCharsets.UTF_16LE.name().equals(actualEncoding)) {
            return UNICODE_EULA_FLAG;
        } else if (StandardCharsets.US_ASCII.name().equals(actualEncoding)) {
            return EULA_FLAG;
        } else {
            throw new ExcelsiorJetApiException(s("JetMojo.Package.Eula.UnsupportedEncoding", eulaEncoding));
        }
    }


}
