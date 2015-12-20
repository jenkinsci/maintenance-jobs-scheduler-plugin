package org.jenkinsci.plugins.disablejobsscheduler;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.scheduler.CronTabList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import java.util.logging.Logger;

/**
 * Descriptor for global configuration.
 *
 * @author Victor Martinez
 */
@Extension
public final class GlobalPluginConfiguration extends GlobalConfiguration {

    private static final String PLUGIN_NAME = Messages.PluginName();

    private static final Logger LOGGER = Logger.getLogger(GlobalPluginConfiguration.class.getName());

    private boolean enableDisabler = false;
    private String spec;


    /**
     * Creates GlobalPluginConfiguration instance with specified parameters.
     *
     * @param enableDisabler
     *            if this feature is enabled.
     * @param spec
     *            the crontab specification
     */
    @DataBoundConstructor
    public GlobalPluginConfiguration(boolean enableDisabler, String spec) {

        this.enableDisabler = enableDisabler;
        this.spec = spec;
    }

    /**
     * Create GlobalPluginConfiguration from disk.
     */
    public GlobalPluginConfiguration() {
        load();
    }

    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
        req.bindJSON(this, json);

        save();
        return true;
    }

    /**
     * Gets whether this plugin is enabled or not.
     *
     * @return true if this plugin is enabled.
     */
    public boolean isEnableDisabler() {
        return enableDisabler;
    }

    /**
     * Sets flag whether this plugin is enabled or not.
     *
     * @param enableDisabler true if this plugin is enabled.
     */
    public void setEnableDisabler(boolean enableDisabler) {
        this.enableDisabler = enableDisabler;
    }

    /**
     * Gets the crontab specification.
     *
     * If you are not using cron service, just ignore it.
     */
    public final String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    /**
     * Gets this extension's instance.
     *
     * @return the instance of this extension.
     */
    public static GlobalPluginConfiguration get() {
        return GlobalConfiguration.all().get(GlobalPluginConfiguration.class);
    }

    /**
     * Performs syntax check.
     */
    public FormValidation doCheckSpec(@QueryParameter String value) {
        try {
            CronTabList ctl = CronTabList.create((value));
        } catch (ANTLRException e) {
            if (value.trim().indexOf('\n')==-1 && value.contains("**"))
                return FormValidation.error(Messages.Error());
            return FormValidation.error(e.getMessage());
        }
        return FormValidation.ok();
    }

    public String toString() {
        return " spec - " + (spec !=null ? spec : "nothing") +
               " enableDisabler - " + enableDisabler;
    }
}