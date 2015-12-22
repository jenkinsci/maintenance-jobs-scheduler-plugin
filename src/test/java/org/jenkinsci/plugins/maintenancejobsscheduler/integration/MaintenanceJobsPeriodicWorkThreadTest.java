package org.jenkinsci.plugins.maintenancejobsscheduler.integration;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.maintenancejobsscheduler.MaintenanceJobsPeriodicWork;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author victor.martinez.
 */
public class MaintenanceJobsPeriodicWorkThreadTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private void waitUntilThreadEnds(MaintenanceJobsPeriodicWork calculation) throws InterruptedException{
        Thread thread = null;
        //wait until thread ends
        for(Thread t : Thread.getAllStackTraces().keySet()){
            if(calculation.name.equals(t.getName())){
                while(thread.isAlive())
                    Thread.sleep(100);
                break;
            }
        }
    }

    static Result build(FreeStyleProject project) throws Exception {
        return project.scheduleBuild2(0).get().getResult();
    }

    @Test
    public void testWithoutAnyJobs() throws IOException, InterruptedException{
        FreeStyleProject project1 = j.createFreeStyleProject("project1");
        project1.setDescription("description");
        FreeStyleProject project2 = j.createFreeStyleProject("project2");
        project2.setDescription("description");

        MaintenanceJobsPeriodicWork work = new MaintenanceJobsPeriodicWork();
        work.execute(true, 1, "disabled");
        waitUntilThreadEnds(work);
        assertEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());
    }
}