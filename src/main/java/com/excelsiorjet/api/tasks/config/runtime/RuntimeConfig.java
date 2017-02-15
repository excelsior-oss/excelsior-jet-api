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
package com.excelsiorjet.api.tasks.config.runtime;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.tasks.*;

import static com.excelsiorjet.api.log.Log.logger;
import static com.excelsiorjet.api.util.Txt.s;

/**
 * Runtime configuration parameters.
 *
 * @author Nikita Lipsky
 */
public class RuntimeConfig {

    /**
     * Excelsior JET Runtime flavor.
     * <p>
     * Excelsior JET comes with multiple implementations of the runtime system,
     * optimized for different hardware configurations and application types.
     * For details, refer to the Excelsior JET User's Guide, Chapter "Application
     * Considerations", section "Runtime Selection".
     * </p>
     * <p>
     * Available runtime flavors: {@code desktop}, {@code server}, {@code classic}
     * </p>
     */
    public String flavor;

    /**
     * Location of Excelsior JET runtime files in the installation package.
     * By default, Excelsior JET places its runtime files required for the 
     * generated executable to work in a folder named {@code "rt"} located next to that executable.
     * You may change that default location with this parameter.
     * <p>
     * This functionality is only available in Excelsior JET 11.3 and above.
     * </p>
     */
    public String location;

    /**
     * Optional JET Runtime components that have to be added to the package.
     * By default, only the {@code jce} component (Java Crypto Extension) is added.
     * You may pass a special value {@code all} to include all available optional components at once
     * or {@code none} to not include any of them.
     * <p>
     * Available optional components:
     * {@code runtime_utilities}, {@code fonts}, {@code awt_natives}, {@code api_classes}, {@code jce},
     * {@code accessibility}, {@code javafx}, {@code javafx-webkit}, {@code nashorn}, {@code cldr}
     * </p>
     */
    public String[] components;

    /**
     * Locales and charsets that have to be included in the package.
     * By default only {@code European} locales are added.
     * You may pass a special value {@code all} to include all available locales at once
     * or {@code none} to not include any additional locales.
     * <p>
     * Available locales and charsets:
     *    {@code European}, {@code Indonesian}, {@code Malay}, {@code Hebrew}, {@code Arabic},
     *    {@code Chinese}, {@code Japanese}, {@code Korean}, {@code Thai}, {@code Vietnamese}, {@code Hindi},
     *    {@code Extended_Chinese}, {@code Extended_Japanese}, {@code Extended_Korean}, {@code Extended_Thai},
     *    {@code Extended_IBM}, {@code Extended_Macintosh}, {@code Latin_3}
     * </p>
     */
    public String[] locales;

    /**
     * Java SE API subset to be included in the package.
     * Java SE 8 defines three subsets of the standard Platform API called compact profiles.
     * Excelsior JET enables you to deploy your application with one of those subsets.
     * You may set this parameter to specify a particular profile.
     * Valid values are: {@code auto} (default),  {@code compact1},  {@code compact2},  {@code compact3}, {@code full}
     * <p>
     * {@code auto} value (default) forces Excelsior JET to detect which parts of the Java SE Platform API
     * are referenced by the application and select the smallest compact profile that includes them all,
     * or the entire Platform API if there is no such profile.
     * </p>
     * This functionality is available for Excelsior JET 11.3 and above.
     */
    public String profile;

    /**
     * (32-bit only) Disk footprint reduction mode.
     * Excelsior JET can reduce the disk footprint of the application by including the supposedly
     * unused Java SE API classes in the resulting package in a compressed form.
     * Valid values are: {@code none},  {@code medium} (default),  {@code high-memory},  {@code high-disk}.
     * <p>
     * This feature is only available if {@link JetProject#globalOptimizer} is enabled.
     * In this mode, the Java SE classes that were not compiled into the resulting executable are placed
     * into the resulting package in bytecode form, possibly compressed depending on the mode:
     * </p>
     * <dl>
     * <dt>none</dt>
     * <dd>Disable compression</dd>
     * <dt>medium</dt>
     * <dd>Use a simple compression algorithm that has minimal run time overheads and permits
     * selective decompression.</dd>
     * <dt>high-memory</dt>
     * <dd>Compress all unused Java SE API classes as a whole. This results in more significant disk
     * footprint reduction compared to the {@code medium} compression. However, if one of the compressed classes
     * is needed at run time, the entire bundle must be decompressed to retrieve it.
     * In the {@code high-memory} reduction mode the bundle is decompressed
     * onto the heap and can be garbage collected later.</dd>
     * <dt>high-disk</dt>
     * <dd>Same as {@code high-memory}, but decompress to the temp directory.</dd>
     * </dl>
     */
    public String diskFootprintReduction;

    /**
     * (32-bit only) Java Runtime Slim-Down configuration parameters.
     *
     * @see SlimDownConfig#detachedBaseURL
     * @see SlimDownConfig#detachComponents
     * @see SlimDownConfig#detachedPackage
     */
    public SlimDownConfig slimDown;

    public void fillDefaults(JetProject jetProject, ExcelsiorJet excelsiorJet) throws JetTaskFailureException {

        if (flavor != null) {
            RuntimeFlavorType flavorType = RuntimeFlavorType.validate(flavor);
            if (!excelsiorJet.isRuntimeSupported(flavorType)) {
                throw new JetTaskFailureException(s("JetApi.RuntimeKindNotSupported.Failure", flavor));
            }
        }

        if (location != null) {
            if (!excelsiorJet.isChangeRTLocationAvailable()) {
                throw new JetTaskFailureException(s("JetApi.RuntimeLocationNotAvailable.Failure", flavor));
            }
        }

        if (profile == null) {
            profile = CompactProfileType.AUTO.toString();
        } else {
            CompactProfileType compactProfile = CompactProfileType.validate(profile);
            if (!excelsiorJet.isCompactProfilesSupported()) {
                switch (compactProfile) {
                    case COMPACT1: case COMPACT2: case COMPACT3:
                        throw new JetTaskFailureException(s("JetApi.CompactProfilesNotSupported.Failure", profile));
                    case AUTO: case FULL:
                        break;
                    default:  throw new AssertionError("Unknown compact profile: " + compactProfile);
                }
            }
        }

        if ((slimDown != null) && !slimDown.isDefined()) {
            slimDown = null;
        }

        if (slimDown != null) {
            if (!excelsiorJet.isSlimDownSupported()) {
                logger.warn(s("JetApi.NoSlimDown.Warning"));
                slimDown = null;
            } else {
                if (slimDown.detachedBaseURL == null) {
                    throw new JetTaskFailureException(s("JetApi.DetachedBaseURLMandatory.Failure"));
                }

                if (slimDown.detachedPackage == null) {
                    slimDown.detachedPackage = jetProject.artifactName() + ".pkl";
                }

                jetProject.globalOptimizer(true);
            }

        }

        if (diskFootprintReduction != null) {
            DiskFootprintReductionType.validate(diskFootprintReduction);
            if (!excelsiorJet.isDiskFootprintReductionSupported()) {
                logger.warn(s("JetApi.NoDiskFootprintReduction.Warning"));
                diskFootprintReduction = null;
            } else if (!jetProject.globalOptimizer()) {
                logger.warn(s("JetApi.DiskFootprintReductionForGlobalOnly.Warning"));
                diskFootprintReduction = null;
            }
        }

    }
}
