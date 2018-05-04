package com.excelsiorjet.api.tasks;

import com.excelsiorjet.api.ExcelsiorJet;
import com.excelsiorjet.api.util.Utils;

import java.io.IOException;

/**
 * Cleans up Excelsior JET Project database (PDB) directory for the current project.
 */
public class JetCleanTask {
    private final JetProject project;
    private final ExcelsiorJet excelsiorJet;

    public JetCleanTask(JetProject project, ExcelsiorJet excelsiorJet) {
        this.project = project;
        this.excelsiorJet = excelsiorJet;
    }

    public void execute() throws JetTaskFailureException, IOException {
        project.validate(excelsiorJet, true);

        Utils.cleanDirectory(project.pdbConfiguration().pdbLocation());
    }
}
