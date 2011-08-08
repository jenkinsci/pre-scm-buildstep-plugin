package org.jenkinsci.plugins.preSCMbuildstep;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Cause.LegacyCodeCause;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildTrigger;
import hudson.tasks.BuildStep;


import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.kohsuke.stapler.DataBoundConstructor;

public class PreSCMBuildStepsWrapper extends BuildWrapper {

    public final List<BuildStep> buildSteps;

    @DataBoundConstructor
    public PreSCMBuildStepsWrapper(List<BuildStep> buildstep) {
            this.buildSteps = buildstep;
    }

    @Override
    public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
    }
    
    public  List<BuildStep> getBuildSteps(){
        return buildSteps;
    }
	
     @Override
     public Environment setUp( AbstractBuild build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException{
             return new NoopEnv();
     }

    @Override
    public void preCheckout(AbstractBuild build, Launcher launcher,
                    BuildListener listener) throws IOException, InterruptedException
    {
        listener.getLogger().append("\nRunning before SCM here");
        for( BuildStep bs : buildSteps ) 
        {
            if(!bs.prebuild(build, listener)) 
            {
                listener.getLogger().println("failed pre build " + bs );
            }
        }
        /* end of prebuild steps */
        for( BuildStep bs : buildSteps ) 
        {
            if ( bs instanceof BuildTrigger) 
            {
                BuildTrigger bt = (BuildTrigger)bs;
                for(AbstractProject p : bt.getChildProjects()) 
                {
                    listener.getLogger().println(" scheduling build for " + p.getDisplayName());
                    p.scheduleBuild(0, new LegacyCodeCause());
                }
            } 
            else if(!bs.perform(build, launcher, listener)) 
            {
                listener.getLogger().println("failed build " + bs );
            } 
            else 
            {
                listener.getLogger().println("build " + bs );
            }
        }
        /* end of preform build */
    }



    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

    //~ @Override
    //~ public JobPropertyImpl newInstance(StaplerRequest req, JSONObject json) throws Descriptor.FormException {

        //~ buildSteps = (List)Descriptor.newInstancesFromHeteroList(
            //~ req, c, "buildStep", (List) PromotionProcess.getAll());

     //~ }
            /**
             * This human readable name is used in the configuration screen.
             */
            public String getDisplayName() {
                    // TODO localization
                    return "Run buildstep before SCM runs";
            }

    }

     class NoopEnv extends Environment{
     }
}
