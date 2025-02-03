package org.jenkinsci.plugins.preSCMbuildstep;
/* The MIT License
 *
 * Copyright (c) 2011 Chris Johnson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause.LegacyCodeCause;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildTrigger;
import hudson.tasks.BuildWrapper;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Class to allow any build step to be performed before the SCM checkout occurs.
 *
 * @author Chris Johnson
 */
public class PreSCMBuildStepsWrapper extends BuildWrapper {
    /** Stored build steps to run before the scm checkout is called */
    public final ArrayList<BuildStep> buildSteps;

    public final boolean failOnError;

    /**
     * Constructor taking a list of buildsteps to use.
     *
     * @param buildstep list of but steps configured in the UI
     */
    @DataBoundConstructor
    public PreSCMBuildStepsWrapper(ArrayList<BuildStep> buildstep, boolean failOnError) {
        this.buildSteps = buildstep;
        this.failOnError = failOnError;
    }

    /**
     * Overridden setup returns a noop class as we don't want to add anything here.
     *
     * @param build the running build
     * @param launcher available launcher to run steps in the build
     * @param listener destination that will receive information about steps as they progress
     * @return noop Environment class
     */
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        return new NoopEnv();
    }

    /**
     * Overridden precheckout step, this is where wedo all the work.
     *
     * <p>Checks to make sure we have some buildsteps set, and then calls the prebuild and perform
     * on all of them. TODO: handle build steps failure in some sort of reasonable way
     *
     * @param build the running build
     * @param launcher available launcher to run steps in the build
     * @param listener destination that will receive information about steps as they progress
     */
    @Override
    public void preCheckout(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        PrintStream log = listener.getLogger();

        /* touch workspace so that it is created on first time */
        if (!build.getWorkspace().exists()) {
            build.getWorkspace().mkdirs();
        }

        if (buildSteps == null) {
            log.println("No build steps declared");
            return;
        }

        log.println("Running Prebuild steps");
        for (BuildStep bs : buildSteps) {
            if (!bs.prebuild(build, listener)) {
                log.println("Failed pre build for " + bs.toString());
                if (failOnError) {
                    throw new AbortException("pre-build step failed to setup environment");
                }
            }
        }
        /* end of prebuild steps */
        for (BuildStep bs : buildSteps) {
            if (bs instanceof BuildTrigger bt) {
                for (AbstractProject p : bt.getChildProjects()) {
                    log.println("Triggering build for " + p.getDisplayName());
                    p.scheduleBuild(0, new LegacyCodeCause());
                }
            } else if (!bs.perform(build, launcher, listener)) {
                log.println("Failed build for " + bs.toString());
                if (failOnError) {
                    throw new AbortException("pre-build step failed to setup environment");
                }
            } else {
                log.println("Success build for" + bs.toString());
            }
        }
        /* end of preform build */
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        /** This human readable name is used in the configuration screen. */
        public String getDisplayName() {
            // TODO localization
            return "Run buildstep before SCM runs";
        }
    }

    class NoopEnv extends Environment {}
}
