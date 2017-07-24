/*
 * Copyright 2017 Charlie Black
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo.geode;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.apache.geode.StatisticDescriptor;
import org.apache.geode.Statistics;
import org.apache.geode.StatisticsType;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Declarable;
import org.apache.geode.distributed.DistributedSystem;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by Charlie Black on 6/29/17.
 */
public class DemoInitializer implements Declarable {

    private final MetricRegistry metricRegistry = new MetricRegistry();

    /**
     * Initializes a user-defined object using the given properties. Note that any uncaught exception
     * thrown by this method will cause the <code>Cache</code> initialization to fail.
     *
     * @param props Contains the parameters declared in the declarative xml file.
     * @throws IllegalArgumentException If one of the configuration options in <code>props</code> is
     *                                  illegal or malformed.
     */
    @Override
    public void init(Properties props) {

        props = new Properties();
        try {
            props.load(DemoInitializer.class.getResourceAsStream("/expose_metrics.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DistributedSystem ds = CacheFactory.getAnyInstance().getDistributedSystem();
        final Properties finalProps = props;
        props.stringPropertyNames().forEach(name -> {
            StatisticsType type = ds.findType(name);
            if (type != null) {
                String[] value = finalProps.getProperty(name).split("\\|");
                String statName = ".*";
                String[] statsRegularExpression;
                if (value.length >= 2) {
                    statName = value[0];
                    statsRegularExpression = value[1].split(",");
                } else {
                    statsRegularExpression = value[0].split(",");
                }
                addStatsToRegistry(ds, type, statName, statsRegularExpression);
            }
        });
        final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry)
                .inDomain("DemoRegistry")
                .build();
        reporter.start();


    }

    private void addStatsToRegistry(DistributedSystem ds, StatisticsType type, String statNameRegex, String[] statsRegularExpression) {
        for (Statistics currStatistics : ds.findStatisticsByType(type)) {
            if (Pattern.matches(statNameRegex, currStatistics.getTextId())) {
                for (StatisticDescriptor currDesciptor : type.getStatistics()) {
                    checkForMatchAndAdd(type, statsRegularExpression, currStatistics, currDesciptor);
                }
            }
        }
    }

    private void checkForMatchAndAdd(StatisticsType type, String[] statsRegularExpression, Statistics currStatistics, StatisticDescriptor currDesciptor) {
        for (String currRegex : statsRegularExpression) {
            if (Pattern.matches(currRegex, currDesciptor.getName())) {
                MyInternalGauge gauge = new MyInternalGauge(currStatistics, currDesciptor);
                metricRegistry.register(MetricRegistry.name(type.getName(), currStatistics.getTextId(), currDesciptor.getName()), gauge);
            }
        }
    }

    private class MyInternalGauge implements Gauge<Number> {

        private Statistics statistics;
        private StatisticDescriptor statisticDescriptor;

        public MyInternalGauge(Statistics statistics, StatisticDescriptor statisticDescriptor) {
            this.statistics = statistics;
            this.statisticDescriptor = statisticDescriptor;
        }

        @Override
        public Number getValue() {
            return statistics.get(statisticDescriptor);
        }
    }
}
