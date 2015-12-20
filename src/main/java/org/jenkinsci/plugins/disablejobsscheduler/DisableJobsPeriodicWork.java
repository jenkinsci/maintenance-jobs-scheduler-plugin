package org.jenkinsci.plugins.disablejobsscheduler;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.AperiodicWork;
import hudson.model.AsyncAperiodicWork;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import jenkins.model.Jenkins;
import jenkins.util.Timer;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;

@Extension
public class DisableJobsPeriodicWork extends AsyncAperiodicWork {

    //last scheduled task;
    private static DisableJobsPeriodicWork currentTask;

    private boolean cancelled;

    public DisableJobsPeriodicWork() {
        super("DisableJobsPeriodicWork Worker Thread");
    }

    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {
        GlobalPluginConfiguration conf = GlobalPluginConfiguration.get();
        if (conf.isEnableDisabler()) {
            logger.log(Level.INFO, "Running that job");
            try{
                for (Object item : Jenkins.getInstance().getItems()) {
                    if (item instanceof AbstractProject) {
                        AbstractProject project = (AbstractProject) item;
                        //do not count building project
                        if (project.isBuilding())
                            continue;
                        try {
                            if (project.getLastBuild() != null) {
                                logger.log(Level.INFO, "Disabling that job " + project.getName());

                                //project.getLastBuild().getTimeInMillis() > 30 dias
                                project.disable();
                            } else {
                                // That project doesn't have any build yet, let's avoid disabling those jobs
                                logger.log(Level.INFO, "Excluded that job " + project.getName());

                                continue;
                            }
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Error when recording disk usage for " + project.getName(), ex);
                        }
                    }
                }
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Error when recording disk usage for jobs.", e);
            }
        }
        else {
            logger.log(Level.FINER, "Calculation of jobs is disabled.");
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
        currentTask = new DisableJobsPeriodicWork();
        return currentTask;
    }

    public String getThreadName(){
        return name +" thread";
    }

    public DisableJobsPeriodicWork getLastTask(){
        return currentTask;
    }

    @Override
    public long getInitialDelay(){
        return getRecurrencePeriod();
    }

    @Override
    public boolean cancel(){
        cancelled = true;
        ScheduledThreadPoolExecutor ex = (ScheduledThreadPoolExecutor) Timer.get();
        ex.purge();
        return super.cancel();
    }

    public boolean isCancelled(){
        return cancelled;
    }

    public CronTab getCronTab() throws ANTLRException {
        GlobalPluginConfiguration conf = GlobalPluginConfiguration.get();
        String cron = conf.getSpec();
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
            logger.log(Level.INFO, "Waiting ... " + period);
            return period;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            //it should not happen
            return 1000*60*6;
        }
    }



}