package org.avaje.metric.httpsend;

import org.avaje.metric.Metric;
import org.avaje.metric.MetricManager;
import org.avaje.metric.TimedMetric;
import org.avaje.metric.core.DefaultTimedMetric;
import org.avaje.metric.report.HeaderInfo;
import org.avaje.metric.report.ReportMetrics;
import org.junit.Ignore;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HttpSendReporterTest {

  private static long NANOS_TO_MICROS = 1000L;

  @Ignore
  @Test
  public void testIntegrationWithLocalRepository() throws MalformedURLException {

    URL url = new URL("http://127.0.0.1:8080/");
    HttpSendReporter reporter = new HttpSendReporter(url);

    reporter.report(metrics());
  }


  private ReportMetrics metrics() {

    HeaderInfo headerInfo = new HeaderInfo();
    headerInfo.setApp("app1");
    headerInfo.setEnv("PROD");
    headerInfo.setKey("UNLOCK");
    headerInfo.setServer("homer");

    List<Metric> metrics = new ArrayList<>();
    metrics.add(createTimedMetric());

    long collectTime = System.currentTimeMillis();

    return new ReportMetrics(headerInfo, collectTime, metrics);
  }

  private TimedMetric createTimedMetric() {

    TimedMetric metric = new DefaultTimedMetric(MetricManager.name("org.test.TimedFoo.doStuff"));

    // add duration times in nanos
    metric.addEventDuration(true, 100 * NANOS_TO_MICROS); // 100 micros
    metric.addEventDuration(true, 120 * NANOS_TO_MICROS); // 120 micros
    metric.addEventDuration(true, 140 * NANOS_TO_MICROS);
    metric.addEventDuration(false, 200 * NANOS_TO_MICROS);
    metric.addEventDuration(false, 220 * NANOS_TO_MICROS);

    metric.collectStatistics();
    return metric;
  }
}