package org.jenkinsci.plugins.maintenancejobsscheduler;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;

/**
 * A Test for Plugin on Jenkins.
 *
 * @author Victor Martinez
 */
public class GlobalPluginConfigurationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPluginLoad() {
        GlobalPluginConfiguration config = GlobalPluginConfiguration.get();
        assertNotNull("Not found instance: GlobalPluginConfiguration", config);
    }

}