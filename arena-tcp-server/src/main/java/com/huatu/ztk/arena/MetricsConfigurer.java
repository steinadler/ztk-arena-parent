package com.huatu.ztk.arena;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import metrics_influxdb.api.measurements.CategoriesMetricMeasurementTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by shaojieyue
 * Created time 2016-07-28 16:00
 */

@Configuration
@EnableMetrics
public class MetricsConfigurer extends MetricsConfigurerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MetricsConfigurer.class);
    private String host = System.getProperty("server_ip");

    @Override
    public void configureReporters(MetricRegistry metricRegistry) {
        final ScheduledReporter reporter = InfluxdbReporter.forRegistry(metricRegistry)
                .protocol(new HttpInfluxdbProtocol("192.168.100.19", 8086, "metrics"))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .skipIdleMetrics(false)
                .tag("server", this.host)
                .build();
        reporter.start(1, TimeUnit.MINUTES);
    }
}
