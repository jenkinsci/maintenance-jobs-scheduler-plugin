package org.jenkinsci.plugins.maintenancejobsscheduler;

import antlr.ANTLRException;
import hudson.Extension;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

    private boolean enableDisabler = false;
    private String filter;
    private String spec;
    private String excludedJobs;
    private String description = "This job has been disabled automatically via the DisableJobsPeriodicWork";

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
            Collection<FormValidation> validations = new ArrayList<FormValidation>();
            updateValidationsForSanity(validations, ctl);
            updateValidationsForNextRun(validations, ctl);
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

    private void updateValidationsForNextRun(Collection<FormValidation> validations, CronTabList ctl) {
        Calendar prev = ctl.previous();
        Calendar next = ctl.next();
        if (prev != null && next != null) {
            DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
            validations.add(FormValidation.ok(Messages.would_last_have_run_at_would_next_run_at(fmt.format(prev.getTime()), fmt.format(next.getTime()))));
        } else {
            validations.add(FormValidation.warning(Messages.no_schedules_so_will_never_run()));
        }
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
            for (String excluded : listJobs) {
                try {
                    Pattern search = Pattern.compile(excluded);
                    for (TopLevelItem item : Jenkins.getInstance().getItems()) {
                        if (search.matcher(item.getName()).matches()) {
                            found = true;
                        }
                    }
                } catch (PatternSyntaxException pse) {
                    return FormValidation.error("Invalid regular expression [" +
                            excluded + "] exception: " +
                            pse.getDescription());
                }
            }
            if (!found) {
                return FormValidation.warning("No jobs with the above regex");
            } else {
                return FormValidation.ok();
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

    public String toString() {
        return " spec - " + (spec !=null ? spec : "nothing") +
               " enableDisabler - " + enableDisabler;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}