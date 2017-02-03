package com.excelsiorjet.api.tasks.config.packagefile;

import com.excelsiorjet.api.tasks.JetTaskFailureException;
import com.excelsiorjet.api.util.Utils;

import static com.excelsiorjet.api.util.Txt.s;

/**
 * Package files type enumeration.
 */
public enum PackageFileType {
    AUTO,
    FILE,
    FOLDER;

    public String toString() {
        return Utils.enumConstantNameToParameter(name());
    }

    public static PackageFileType validate(String type) throws JetTaskFailureException {
        try {
            return PackageFileType.valueOf(Utils.parameterToEnumConstantName(type));
        } catch (Exception e) {
            throw new JetTaskFailureException(s("JetApi.UnknownPackageFileType.Error", type));
        }
    }

}
