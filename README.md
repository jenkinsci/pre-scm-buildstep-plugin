# Pre-SCM Build Step

This plugin allows build steps to run before SCM checkout so that build steps can be performed in the workspace before SCM checkout.
Build steps can perform cleanup, add SCM configuration files, etc.
They can call other scripts that need to be run before SCM checkout.

## Configuration example

![](docs/images/pre_scm_buildstep_config.png)

This plugin was originally written to provide a build step point where a perl script could be run after detecting changes in the polling but before the SCM checkout.
To make it more usable and flexible it was decided to allow any number of build steps to be added.

## Warnings on the use of this plugin

Be Careful

- The SCM may modify or remove any files that are in the workspace before the main build steps.
  See [JENKINS-22795](https://issues.jenkins.io/browse/JENKINS-22795) for an example with the git plugin
- Enviroment variables may not exist at this point if they are defined by other plugins.
  Check using a shell script and the env (unix) or equivalent for your executor system

### Warning

- IMPORTANT: Failed pre-SCM build steps will not cause the job to fail at this point and all build steps will be called regardless of the result.

## History

Changelogs are on [GitHub releases](https://github.com/jenkinsci/pre-scm-buildstep-plugin/releases).
