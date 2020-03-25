package jenkinsci.plugins.influxdb.generators;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang.StringUtils;
import org.influxdb.dto.Point;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class CustomDataListMapPointGeneratorTest {

    private static final String JOB_NAME = "master";
    private static final int BUILD_NUMBER = 11;
    private static final String CUSTOM_PREFIX = "test_prefix";

    private Run<?,?> build;
    private TaskListener listener;

    private MeasurementRenderer<Run<?, ?>> measurementRenderer;

    private long currTime;

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        Job job = Mockito.mock(Job.class);
        listener = Mockito.mock(TaskListener.class);
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);

        Mockito.when(build.getNumber()).thenReturn(BUILD_NUMBER);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);

        currTime = System.currentTimeMillis();
    }

    @Test
    public void hasReport() {
        //check with customDataMap = null

        CustomDataListMapPointGenerator cdmGen1 = new CustomDataListMapPointGenerator(build, listener, measurementRenderer,
                currTime, StringUtils.EMPTY, CUSTOM_PREFIX, null, null);
        assertThat(cdmGen1.hasReport(), is(false));

        //check with empty customDataMap
        CustomDataListMapPointGenerator cdmGen2 = new CustomDataListMapPointGenerator(build, listener, measurementRenderer,
                currTime, StringUtils.EMPTY, CUSTOM_PREFIX, Collections.emptyMap(), Collections.emptyMap());
        assertThat(cdmGen2.hasReport(), is(false));
    }

    @Test
    public void generate() {
        Map<String, Object> customDataMap1 = new HashMap<>();
        customDataMap1.put("fullName", "com.example.Test.testCase1");
        customDataMap1.put("status", "PASSED");
        Map<String, Object> customDataMap2 = new HashMap<>();
        customDataMap2.put("fullName", "com.example.Test.testCase2");
        customDataMap2.put("status", "FAILED");

        Map<String, List<Map<String, Object>>> customDataListMap = new HashMap<>();
        customDataListMap.put("jenkins_test_data", Arrays.asList(customDataMap1, customDataMap2));

        Map<String, List<Map<String, String>>> customDataListMapTags = new HashMap<>();
        Map<String, String> customTags1 = new HashMap<>();
        customTags1.put("status", "PASSED");
        Map<String, String> customTags2 = new HashMap<>();
        customTags2.put("status", "FAILED");
        customDataListMapTags.put("jenkins_test_data", Arrays.asList(customTags1, customTags2));

        CustomDataListMapPointGenerator cdlmGen = new CustomDataListMapPointGenerator(build, listener, measurementRenderer,
                currTime, StringUtils.EMPTY, CUSTOM_PREFIX, customDataListMap, customDataListMapTags);
        Point[] pointsToWrite = cdlmGen.generate();

        String lineProtocol1 = pointsToWrite[0].lineProtocol();
        String lineProtocol2 = pointsToWrite[1].lineProtocol();
        assertThat(lineProtocol1, startsWith("jenkins_test_data,prefix=test_prefix,project_name=test_prefix_master,project_path=folder/master,status=PASSED build_number=11i,fullName=\"com.example.Test.testCase1\",project_name=\"test_prefix_master\",project_path=\"folder/master\",status=\"PASSED\""));
        assertThat(lineProtocol2, startsWith("jenkins_test_data,prefix=test_prefix,project_name=test_prefix_master,project_path=folder/master,status=FAILED build_number=11i,fullName=\"com.example.Test.testCase2\",project_name=\"test_prefix_master\",project_path=\"folder/master\",status=\"FAILED\""));
    }
}
