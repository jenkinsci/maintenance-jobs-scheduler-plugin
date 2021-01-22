package org.jenkinsci.plugins.maintenancejobsscheduler;

import antlr.ANTLRException;
import hudson.Extension;
import static hudson.Util.fixNull;
import hudson.model.TopLevelItem;
import hudson.scheduler.CronTabList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.Messages;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Descriptor for global configuration.
 *
 * @author Victor Martinez
 */
@Extension
public final class GlobalPluginConfiguration extends GlobalConfiguration {

    private static final String PLUGIN_NAME = Messages.PluginName();

    private boolean enable = false;
    private boolean removeJobs = false;
    private String filter;
    private String disabledSpec;
    private String excludedJobs;
    private String description = Messages.Description();

    /**
     * Creates GlobalPluginConfiguration instance with specified parameters.
     *
     * @param enable
     *            if this feature is enabled.
     * @param disabledSpec
     *            the crontab specification
     */
    @DataBoundConstructor
    public GlobalPluginConfiguration(boolean enable, String disabledSpec) {
        this.enable = enable;
        this.disabledSpec = disabledSpec;
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
    public boolean isEnable() {
        return enable;
    }

    /**
     * Sets flag whether this plugin is enabled or not.
     *
     * @param enable true if this plugin is enabled.
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * Gets the crontab specification.
     *
     * If you are not using cron service, just ignore it.
     */
    public final String getDisabledSpec() {
        return disabledSpec;
    }

    public void setDisabledSpec(String disabledSpec) {
        this.disabledSpec = disabledSpec;
    }

    public String getExcludedJobs() {
        return excludedJobs;
    }

    public void setExcludedJobs(String excludedJobs) {
        this.excludedJobs = excludedJobs;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRemoveJobs() {
        return removeJobs;
    }

    public void setRemoveJobs(boolean removeJobs) {
        this.removeJobs = removeJobs;
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
    public FormValidation doCheckDisabledSpec(@QueryParameter String value) {
        try {
            CronTabList ctl = CronTabList.create(fixNull(value));
            Collection<FormValidation> validations = new ArrayList<FormValidation>();
            updateValidationsForSanity(validations, ctl);
            return FormValidation.aggregate(validations);
        } catch (ANTLRException e) {
            if (value.trim().indexOf('\n')==-1 && value.contains("**"))
                return FormValidation.error(Messages.Error());
            return FormValidation.error(e.getMessage());
        }
    }

    private void updateValidationsForSanity(Collection<FormValidation> validations, CronTabList ctl) {
        String msg = ctl.checkSanity();
        if(msg!=null)  validations.add(FormValidation.warning(msg));
    }

    /**
     * Check the regular expression entered by the user
     */
    public FormValidation doCheckExcludedJobs(@QueryParameter String value) {
        List<String> listJobs = null;
        if(StringUtils.isNotBlank(value)) {
            listJobs = Arrays.asList(value.split("\n"));
        }
        if (listJobs!=null) {
            boolean found = false;
            int numberOfProjects = 0;
            for (String excluded : listJobs) {
                try {
                    Pattern search = Pattern.compile(excluded);
                    for (TopLevelItem item : Jenkins.getInstance().getItems()) {
                        if (search.matcher(item.getName()).matches()) {
                            found = true;
                            numberOfProjects++;
                        }
                    }
                } catch (PatternSyntaxException pse) {
                    return FormValidation.error("Invalid regular expression [" +
                            excluded + "] exception: " +
                            pse.getDescription());
                }
            }
            if (!found) {
                return FormValidation.warning(Messages.no_jobs());
            } else {
                return FormValidation.ok(Messages.there_are_jobs(numberOfProjects));
            }
        }else{
            return FormValidation.ok();
        }
    }

    /**
     * Check the Integer expression entered by the user
     *
     */
    public FormValidation doFilter(@QueryParameter String value) {
        try {
            Integer.parseInt(value);
        } catch (Exception e) {
            return FormValidation.error("Invalid numeric [" +
                    value + "] exception: " + e.getMessage());
        }
        return FormValidation.ok();
    }

    /**
     * Check the Integer expression entered by the user
     *
     */
    public FormValidation doCheckRemoveJobs(@QueryParameter boolean value) {
        if (value)
            return FormValidation.warning(Messages.warning_removing_jobs());
        return FormValidation.ok();
    }

    public String toString() {
        return " disabledSpec - " + (disabledSpec !=null ? disabledSpec : "nothing") +
               " enable - " + enable;
    }
}
