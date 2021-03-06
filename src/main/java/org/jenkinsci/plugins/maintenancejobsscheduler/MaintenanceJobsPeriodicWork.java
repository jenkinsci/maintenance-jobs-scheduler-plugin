package org.jenkinsci.plugins.maintenancejobsscheduler;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.AperiodicWork;
import hudson.model.AsyncAperiodicWork;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author Victor Martinez
 */
@Extension
public class MaintenanceJobsPeriodicWork extends AsyncAperiodicWork {

    //last scheduled task;
    private static MaintenanceJobsPeriodicWork currentTask;

    public MaintenanceJobsPeriodicWork() {
        super("MaintenanceJobsPeriodicWork Worker Thread");
    }

    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {
        GlobalPluginConfiguration conf = GlobalPluginConfiguration.get();
        execute(conf.isEnable(), Integer.parseInt(conf.getFilter()), conf.getDescription(), conf.getExcludedJobs(), conf.isRemoveJobs());
    }

    public void execute(boolean enable, int filter, String defaultDescription, String excludedJobs, boolean removeJobs) throws IOException, InterruptedException {
        if (enable) {
            Date today = new Date();

            List<String> listJobs = null;
            if(StringUtils.isNotBlank(excludedJobs)) {
                listJobs = Arrays.asList(excludedJobs.split("\n"));
            }

            for (Object item : Jenkins.getInstance().getItems()) {
                if (item instanceof AbstractProject) {
                    AbstractProject project = (AbstractProject) item;
                    long purgeTime = System.currentTimeMillis() - (filter * 24 * 60 * 60 * 1000);
                    if (project.getLastBuild() == null) {
                        logger.log(Level.FINER, "Excluded that job '" + project.getName() + "' since it doesn't have any builds yet");
                    } else if (project.isDisabled()) {
                        logger.log(Level.FINER, "Excluded that job '" + project.getName() + "' since it doesn't have any builds yet");
                    } else if (project.getLastBuild().getTimeInMillis() < purgeTime) {
                        boolean found = true;
                        if (listJobs != null) {
                            for (String excluded : listJobs) {
                                try {
                                    Pattern search = Pattern.compile(excluded);
                                    if (search.matcher(project.getName()).matches()) {
                                        found = false;
                                    }
                                } catch(PatternSyntaxException pse) {
                                    logger.log(Level.WARNING, "It does nothing since Invalid regular expression [" + excluded +
                                                "] exception: " + pse.getDescription());
                                    found = false;
                                }
                            }
                        }
                        if (found) {
                            if (removeJobs) {
                                logger.log(Level.FINER, "Removing job '" + project.getName() + "'");
                                project.delete();
                            } else {
                                logger.log(Level.FINER, "Disabling job '" + project.getName() + "'");
                                project.disable();
                                String description = defaultDescription + " '" + today.toString() + "'\n";
                                //TODO: add dependency with https://wiki.jenkins-ci.org/display/JENKINS/OWASP+Markup+Formatter+Plugin
                                // in order to add description in html format
                                // if (Jenkins.getInstance().getMarkupFormatter() instanceof hudson.markup.RawHtmlMarkupFormatter)
                                project.setDescription(description + project.getDescription());
                            }
                        } else {
                            logger.log(Level.FINER, "Excluded that job '" + project.getName() + "' since it matches the excluded regex!");
                        }
                    } else {
                        logger.log(Level.FINER, "Excluded that job '" + project.getName() + "' for some other reason");
                    }
                }
            }
        } else {
            logger.log(Level.FINER, this.name + " is disabled.");
        }
    }

    @Override
    public AperiodicWork getNewInstance() {
        if(currentTask!=null){
            currentTask.cancel();
        }
        else{
            cancel();
        }
        currentTask = new MaintenanceJobsPeriodicWork();
        return currentTask;
    }

    public String getThreadName(){
        return name +" thread";
    }

    public MaintenanceJobsPeriodicWork getLastTask(){
        return currentTask;
    }

    @Override
    public long getInitialDelay(){
        return getRecurrencePeriod();
    }

    @Override
    public boolean cancel(){
        ScheduledThreadPoolExecutor ex = (ScheduledThreadPoolExecutor) Timer.get();
        ex.purge();
        return super.cancel();
    }

    public CronTab getCronTab() throws ANTLRException, NullPointerException {
        GlobalPluginConfiguration conf = GlobalPluginConfiguration.get();
        String cron = conf.getDisabledSpec();
        CronTab tab = new CronTab(cron);
        return tab;
    }

    @Override
    public long getRecurrencePeriod() {
        try {
            CronTab tab = getCronTab();
            GregorianCalendar now = new GregorianCalendar();
            Calendar nextExecution = tab.ceil(now.getTimeInMillis());
            long period = nextExecution.getTimeInMillis() - now.getTimeInMillis();
            if(nextExecution.getTimeInMillis() - now.getTimeInMillis()<=60000)
                period = period + 60000l; //add one minute to not schedule it during one minute one than once
            logger.log(Level.FINER, "Waiting ... " + period + " ms");
            return period;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            //it should not happen
            return 1000*60*6;
        }
    }
}