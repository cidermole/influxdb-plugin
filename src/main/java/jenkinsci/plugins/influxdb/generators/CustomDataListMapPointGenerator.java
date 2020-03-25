package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.*;

public class CustomDataListMapPointGenerator extends AbstractPointGenerator {

    private final String customPrefix;
    private final Map<String, List<Map<String, Object>>> customDataListMap;
    private final Map<String, List<Map<String, String>>> customDataListMapTags;
    private long timestamp;

    public CustomDataListMapPointGenerator(Run<?, ?> build, TaskListener listener,
                                           MeasurementRenderer<Run<?, ?>> projectNameRenderer,
                                           long timestamp, String jenkinsEnvParameterTag,
                                           String customPrefix, Map<String, List<Map<String, Object>>> customDataListMap,
                                           Map<String, List<Map<String, String>>> customDataListMapTags) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        this.customDataListMap = customDataListMap;
        this.customDataListMapTags = customDataListMapTags;
        this.timestamp = timestamp;
    }

    public boolean hasReport() {
        return (customDataListMap != null && customDataListMap.size() > 0);
    }

    public Point[] generate() {
        List<Point> points = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entries : customDataListMap.entrySet()) {
            int i = 0;
            for(Map<String, Object> entry : entries.getValue()) {
                Point.Builder pointBuilder = buildPointInc(entries.getKey(), customPrefix, build).fields(entry);

                if (customDataListMapTags != null) {
                    List<Map<String, String>> customTagList = customDataListMapTags.get(entries.getKey());
                    if (customTagList != null) {
                        if (customTagList.size() > i) {
                            Map<String, String> customTags = customTagList.get(i);
                            if(customTags != null && customTags.size() > 0) {
                                pointBuilder.tag(customTags);
                            }
                        }
                    }
                }

                Point point = pointBuilder.build();

                points.add(point);
                i++;
            }
        }

        return points.toArray(new Point[0]);
    }

    /**
     * Initializes a basic build point with the basic data already set with a specified timestamp.
     * Post-increments the timestamp, such that multiple points can be added for the same measurement.
     */
    private Point.Builder buildPointInc(String name, String customPrefix, Run<?, ?> build) {
        Point.Builder builder = buildPoint(name, customPrefix, build, timestamp);
        timestamp++;
        return builder;
    }
}
