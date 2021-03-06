package org.jenkinsci.plugins.maintenancejobsscheduler.integration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.maintenancejobsscheduler.MaintenanceJobsPeriodicWork;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

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
    public void testWithoutAnyBuilds() throws IOException, InterruptedException {
        FreeStyleProject project1 = j.createFreeStyleProject("project1");
        project1.setDescription("description");
        FreeStyleProject project2 = j.createFreeStyleProject("project2");
        project2.setDescription("description");

        MaintenanceJobsPeriodicWork work = new MaintenanceJobsPeriodicWork();
        work.execute(true, 1, "disabled", "", false);
        waitUntilThreadEnds(work);
        assertEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());
    }

    @LocalData
    @Test
    public void testWithDisableJobs() throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject project1 = j.jenkins.getItemByFullName("project1", FreeStyleProject.class);
        FreeStyleProject project2 = j.jenkins.getItemByFullName("project2", FreeStyleProject.class);

        project1.setDescription("description");
        project1.disable();
        project2.setDescription("description");
        project2.disable();
        MaintenanceJobsPeriodicWork work = new MaintenanceJobsPeriodicWork();
        work.execute(true, 1, "disabled", "", false);
        waitUntilThreadEnds(work);
        assertEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());
    }

    @LocalData
    @Test
    public void testWithOldJobs() throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject project1 = j.jenkins.getItemByFullName("project1", FreeStyleProject.class);
        FreeStyleProject project2 = j.jenkins.getItemByFullName("project2", FreeStyleProject.class);

        project1.setDescription("description");
        project2.setDescription("description");

        // Run build and wait for it to finish
        project2.scheduleBuild2(0);
        QueueTaskFuture<FreeStyleBuild> f = project2.scheduleBuild2(0);
        f.waitForStart();
        f.get();

        MaintenanceJobsPeriodicWork work = new MaintenanceJobsPeriodicWork();
        work.execute(true, 365, "disabled", "", false);
        waitUntilThreadEnds(work);
        assertNotEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());

        project1 = j.jenkins.getItemByFullName("project1", FreeStyleProject.class);
        project1.setDescription("description");
        work.execute(false, 365, "disabled", "", false);
        waitUntilThreadEnds(work);
        assertEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());
    }

    @LocalData
    @Test
    public void testWithOldJobsAndRemoveOption() throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject project2 = j.jenkins.getItemByFullName("project2", FreeStyleProject.class);

        project2.setDescription("description");

        // Run build and wait for it to finish
        project2.scheduleBuild2(0);
        QueueTaskFuture<FreeStyleBuild> f = project2.scheduleBuild2(0);
        f.waitForStart();
        f.get();

        MaintenanceJobsPeriodicWork work = new MaintenanceJobsPeriodicWork();
        work.execute(true, 365, "disabled", "", true);
        waitUntilThreadEnds(work);
        FreeStyleProject project1 = j.jenkins.getItemByFullName("project1", FreeStyleProject.class);
        assertNull(project1);
        assertEquals("description", project2.getDescription());
    }

    @LocalData
    @Test
    public void testWithOldJobsAndExcludedOption() throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject project1 = j.jenkins.getItemByFullName("project1", FreeStyleProject.class);
        FreeStyleProject project2 = j.jenkins.getItemByFullName("project2", FreeStyleProject.class);

        project1.setDescription("description");
        project2.setDescription("description");

        // Run build and wait for it to finish
        project2.scheduleBuild2(0);
        QueueTaskFuture<FreeStyleBuild> f = project2.scheduleBuild2(0);
        f.waitForStart();
        f.get();

        MaintenanceJobsPeriodicWork work = new MaintenanceJobsPeriodicWork();
        work.execute(true, 365, "disabled", ".*1$", false);
        waitUntilThreadEnds(work);
        assertEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());

        work.execute(true, 365, "disabled", "doesn't exist regex", false);
        waitUntilThreadEnds(work);
        assertNotEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());

        project1.setDescription("description");
        work.execute(true, 365, "disabled", "+*wrongregexp(])", false);
        waitUntilThreadEnds(work);
        assertEquals("description", project1.getDescription());
        assertEquals("description", project2.getDescription());
    }
}
