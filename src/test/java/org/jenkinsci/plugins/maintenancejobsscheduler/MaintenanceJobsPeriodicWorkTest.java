package org.jenkinsci.plugins.maintenancejobsscheduler;

import hudson.scheduler.CronTab;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import antlr.ANTLRException;

public class MaintenanceJobsPeriodicWorkTest {

    private MaintenanceJobsPeriodicWork periodicWork;

    @Before
    public void setup() {
        periodicWork = new MaintenanceJobsPeriodicWork();
    }

    /**
     * Depends on test testReschedule() - if testReshedule fails this test probably will fail too.
     *
     * see @testReschedule()
     */
    @Test
    public void testScheduledExecutionTime() throws Exception{
        // Trigger.timer = new Timer("Jenkins cron thread"); // it should be enought there is no need to start Jenkins
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(Calendar.MINUTE, 10);
        int minute = calendar.get(Calendar.MINUTE);
        //  attribut currentTask should have value calculation
        TestDiskUsageCalculation calculation = (TestDiskUsageCalculation) new TestDiskUsageCalculation(minute + " * * * *", false);
        if(calculation.getLastTask()!=null){
            //should not be any, but if cancel;
            calculation.getLastTask().cancel();
        }
        Long expectedNextExecution = calendar.getTimeInMillis();
        assertEquals("Scheduled time of disk usage calculation should 0, because calculation is not scheduled", 0, calculation.scheduledLastInstanceExecutionTime(), 60000);
        Timer.get().schedule(calculation.getNewInstance(), calculation.getRecurrencePeriod(), TimeUnit.MILLISECONDS);
        assertEquals("Scheduled time of disk usage calculation should be in 10 minutes", expectedNextExecution, calculation.scheduledLastInstanceExecutionTime(), 60000);

        //scheduled time should be changed if configuration of cron is changed
        calendar.add(Calendar.MINUTE, 10);
        minute = calendar.get(Calendar.MINUTE);
        calculation.setCron(minute + " * * * *");
        calculation.reschedule();
        expectedNextExecution = calendar.getTimeInMillis();
        assertEquals("Scheduled time of disk usage calculation should be changed", expectedNextExecution, calculation.scheduledLastInstanceExecutionTime(), 60000);

    }

}