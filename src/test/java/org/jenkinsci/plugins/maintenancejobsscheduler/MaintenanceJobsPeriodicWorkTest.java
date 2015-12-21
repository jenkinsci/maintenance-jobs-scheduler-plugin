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

}