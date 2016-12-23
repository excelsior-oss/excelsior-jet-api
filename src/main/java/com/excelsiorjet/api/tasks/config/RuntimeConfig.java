package com.excelsiorjet.api.tasks.config;

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
     * Excelsior JET VM comes with multiple implementations of the runtime system,
     * optimized for different hardware configurations and application types.
     * It enables your application to effectively utilize the computing power of systems
     * that support parallel execution (examples are multi-processor servers,
     * multi-core chips and CPUs that support Hyper-Threading Technology), thereby improving performance.
     *
     * Available runtime kinds: {@code desktop}, {@code server}, {@code classic}
     *
     * <dl>
     * <dt>desktop</dt>
     * <dd> The Desktop Runtime is suitable for applications that typically run on conventional desktop
     * and notebook computers. It is optimized for single-CPU systems, including those based on multi-core chips.
     * This is the best choice for rich clients, visualization and engineering design tools, and other desktop applications.
     * </dd>
     * <dt>server</dt>
     * <dd>The Server Runtime fits best for highly concurrent server applications.
     * It provides out-of-the-box performance and scalability and takes full advantage of the computing power of
     * multi-processor hardware. In particular, the Server Runtime includes the CoreBalance garbage collector
     * that fully utilizes parallel hardware to reduce average and maximum pause time.
     * Note that Excelsior JET, Enterprise Edition is the only retail version that includes the Server Runtime.
     * </dd>
     * <dt>classic</dt>
     * <dd>The Classic Runtime is designed to use on low-end hardware which does not support parallel execution
     * such as uniprocessor systems equipped with old CPU models of the x86 architecture.
     * It is not recommended for use on HyperThread/multi-core CPU and multi-processor systems.
     * Note that the Classic Runtime is the only option in the Standard Edition of Excelsior JET.
     * </dd>
     * </dl>
     */
    public String kind;

    /**
     * By default, Excelsior JET places required Excelsior JET runtime files near to compiled executable
     * to a folder with "rt" name.
     * You may change its default location with this parameter.
     * The functionality is available for Excelsior JET 11.3 and above.
     */
    public String location;

    /**
     * Add optional JET Runtime components to the package.
     * By default, only the {@code jce} component (Java Crypto Extension) is added.
     * You may pass a special value {@code all} to include all available optional components at once
     * or {@code none} to not include any of them.
     * Available optional components:
     * {@code runtime_utilities}, {@code fonts}, {@code awt_natives}, {@code api_classes}, {@code jce},
     * {@code accessibility}, {@code javafx}, {@code javafx-webkit}, {@code nashorn}, {@code cldr}
     */
    public String[] components;

    /**
     * Add locales and charsets.
     * By default only {@code European} locales are added.
     * You may pass a special value {@code all} to include all available locales at once
     * or {@code none} to not include any additional locales.
     * Available locales and charsets:
     *    {@code European}, {@code Indonesian}, {@code Malay}, {@code Hebrew}, {@code Arabic},
     *    {@code Chinese}, {@code Japanese}, {@code Korean}, {@code Thai}, {@code Vietnamese}, {@code Hindi},
     *    {@code Extended_Chinese}, {@code Extended_Japanese}, {@code Extended_Korean}, {@code Extended_Thai},
     *    {@code Extended_IBM}, {@code Extended_Macintosh}, {@code Latin_3}
     */
    public String[] locales;

    /**
     * Java SE 8 defines three subsets of the standard Platform API called compact profiles.
     * Excelsior JET enables you to deploy your application with one of those subsets.
     * You may set this parameter to specify a particular profile.
     * Valid values are: {@code auto} (default),  {@code compact1},  {@code compact2},  {@code compact3}, {@code full}
     *  <p>
     * {@code auto} value (default) forces Excelsior JET to detect which parts of the Java SE Platform API
     * are referenced by the application and select the smallest compact profile that includes them all,
     * or the entire Platform API if there is no such profile.
     * </p>
     * The functionality is available for Excelsior JET 11.3 and above.
     */
    public String profile;

    /**
     * (32-bit only)
     * Reduce the disk footprint of the application by including the supposedly unused Java SE API
     * classes in the resulting package in a compressed form.
     * Valid values are: {@code none},  {@code medium} (default),  {@code high-memory},  {@code high-disk}.
     * <p>
     * The feature is only available if {@link JetProject#globalOptimizer} is enabled.
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
     * footprint reduction compared to than medium compression. However, if one of the compressed classes
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

        if (kind != null) {
            if (kind() == null) {
                throw new JetTaskFailureException(s("JetApi.UnknownRuntimeKind.Failure", kind));
            }
            if (!excelsiorJet.isRuntimeSupported(kind())) {
                throw new JetTaskFailureException(s("JetApi.RuntimeKindNotSupported.Failure", kind));
            }
        }

        if (location != null) {
            if (!excelsiorJet.isChangeRTLocationAvailable()) {
                throw new JetTaskFailureException(s("JetApi.RuntimeLocationNotAvailable.Failure", kind));
            }
        }

        if (profile == null) {
            profile = CompactProfileType.AUTO.toString();
        }
        if (compactProfile() == null) {
            throw new JetTaskFailureException(s("JetApi.UnknownProfileType.Failure", profile));
        }
        if (!excelsiorJet.isCompactProfilesSupported()) {
            switch (compactProfile()) {
                case COMPACT1: case COMPACT2: case COMPACT3:
                    throw new JetTaskFailureException(s("JetApi.CompactProfilesNotSupported.Failure", profile));
                case AUTO: case FULL:
                    break;
                default:  throw new AssertionError("Unknown compact profile: " + compactProfile());
            }
        }
        if ((slimDown != null) && !slimDown.isEnabled()) {
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
            if (diskFootprintReduction() == null) {
                throw new JetTaskFailureException(s("JetApi.UnknownDiskFootprintReductionType.Failure", profile));
            }
            if (!excelsiorJet.isDiskFootprintReductionSupported()) {
                logger.warn(s("JetApi.NoDiskFootprintReduction.Warning"));
                diskFootprintReduction = null;
            } else if (!jetProject.globalOptimizer()) {
                logger.warn(s("JetApi.DiskFootprintReductionForGlobalOnly.Warning"));
                diskFootprintReduction = null;
            }
        }

    }

    public CompactProfileType compactProfile() {
        return CompactProfileType.fromString(profile);
    }

    public DiskFootprintReductionType diskFootprintReduction() {
        return DiskFootprintReductionType.fromString(diskFootprintReduction);
    }

    public RuntimeKindType kind() {
        return RuntimeKindType.fromString(kind);
    }

}
