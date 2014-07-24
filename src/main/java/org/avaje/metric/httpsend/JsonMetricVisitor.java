package org.avaje.metric.httpsend;


import org.avaje.metric.*;
import org.avaje.metric.filereport.NumFormat;

import java.util.List;

/**
 * Writes the metric information as JSON to a buffer for sending.
 */
public class JsonMetricVisitor implements MetricVisitor {

  protected final int decimalPlaces;

  protected final StringBuilder buffer;

  protected long collectionTime;

  public JsonMetricVisitor(long collectionTime) {
    this(collectionTime, 1000);
  }

  public JsonMetricVisitor(long collectionTime, int bufferSize) {
    this(collectionTime, new StringBuilder(bufferSize));
  }
  
  public JsonMetricVisitor(long collectionTime, StringBuilder buffer) {
    this.collectionTime = collectionTime;
    this.buffer = buffer;
    this.decimalPlaces = 2;
  }

  public String buildJson(HttpSendReporter reporter, List<Metric> metrics) {
    
    buffer.append("{");
    appendHeader(reporter);
    writeKey("metrics");
    buffer.append("[\n");
    buildJson(metrics);
    buffer.append("]");
    buffer.append("}");
    
    return buffer.toString();
  }

  protected void buildJson(List<Metric> metrics) {
    for (int i = 0; i < metrics.size(); i++) {
      if (i == 0) {
        buffer.append("  ");        
      } else {
        buffer.append(" ,");
      }
      Metric metric = metrics.get(i);
      metric.visit(this);
      buffer.append("\n");
    }
  }

  public String getBufferValue() {
    return buffer.toString();
  }
  

  protected void appendHeader(HttpSendReporter reporter) {
    
    writeHeader("time", System.currentTimeMillis());
    writeHeader("app", reporter.app);
    writeHeader("env", reporter.env);
    writeHeader("server", reporter.server);
  }

  protected void writeMetricStart(String type, Metric metric) {

    buffer.append("{");
    writeKey("type");
    writeValue(type);
    buffer.append(",");
    writeKey("name");
    writeValue(metric.getName().getSimpleName());
    buffer.append(",");
  }

  protected void writeMetricEnd(Metric metric) {
    buffer.append("}");
  }

  @Override
  public void visit(TimedMetric metric) {

    writeMetricStart("timed", metric);
    writeSummary("n", metric.getCollectedSuccessStatistics());
    buffer.append(",");
    writeSummary("e", metric.getCollectedErrorStatistics());
    writeMetricEnd(metric);
  }

  @Override
  public void visit(ValueMetric metric) {

    writeMetricStart("value", metric);
    writeSummary("n", metric.getCollectedStatistics());
    writeMetricEnd(metric);
  }

  @Override
  public void visit(CounterMetric metric) {

    writeMetricStart("counter", metric);
    CounterStatistics counterStatistics = metric.getCollectedStatistics();
    writeKeyNumber("count", counterStatistics.getCount());
    buffer.append(",");
    writeKeyNumber("dur", getDuration(counterStatistics.getStartTime()));
    writeMetricEnd(metric);
  }

  @Override
  public void visit(GaugeMetricGroup gaugeMetricGroup) {

    GaugeMetric[] gaugeMetrics = gaugeMetricGroup.getGaugeMetrics();
    writeMetricStart("gaugeGroup", gaugeMetricGroup);
    writeKey("group");
    buffer.append("[");
    for (int i = 0; i < gaugeMetrics.length; i++) {
      if (i > 0) {
        buffer.append(", ");
      }
      GaugeMetric m = gaugeMetrics[i];
      writeKeyNumber(m.getName().getName(), format(m.getValue()));
    }
    buffer.append("]");
    writeMetricEnd(gaugeMetricGroup);
  }

  @Override
  public void visit(GaugeMetric metric) {

    writeMetricStart("gauge", metric);
    writeKeyNumber("value", format(metric.getValue()));
    writeMetricEnd(metric);
  }

    @Override
    public void visit(GaugeCounterMetric metric) {
      writeMetricStart("gaugeCounter", metric);
      writeKeyNumber("value", metric.getValue());
      writeMetricEnd(metric);
    }

    @Override
    public void visit(GaugeCounterMetricGroup gaugeMetricGroup) {

      GaugeCounterMetric[] gaugeMetrics = gaugeMetricGroup.getGaugeMetrics();
      writeMetricStart("gaugeCounterGroup", gaugeMetricGroup);
      writeKey("group");
      buffer.append("[");
      for (int i = 0; i < gaugeMetrics.length; i++) {
        if (i > 0) {
          buffer.append(", ");
        }
        GaugeCounterMetric m = gaugeMetrics[i];
        writeKeyNumber(m.getName().getName(), m.getValue());
      }
      buffer.append("]");
      writeMetricEnd(gaugeMetricGroup);
    }

    protected void writeSummary(String prefix, ValueStatistics valueStats) {

    long count = valueStats.getCount();
    writeKey(prefix);
    buffer.append("{");
    writeKeyNumber("count", count);

    if (count != 0) {
      buffer.append(",");
      writeKeyNumber("avg", valueStats.getMean());
      buffer.append(",");
      writeKeyNumber("max", valueStats.getMax());
      buffer.append(",");
      writeKeyNumber("sum", valueStats.getTotal());
      buffer.append(",");
      writeKeyNumber("dur", getDuration(valueStats.getStartTime()));
    }

    buffer.append("}");
  }

  protected String format(double value) {
    return NumFormat.dp(decimalPlaces, value);
  }

  protected void writeKeyNumber(String key, long numberValue) {
    writeKeyNumber(key, String.valueOf(numberValue));
  }

  protected void writeKeyNumber(String key, String numberValue) {
    writeKey(key);
    writeNumberValue(numberValue);
  }

  public void writeHeader(String key, String value) {
    writeKey(key);
    writeValue(value);
    buffer.append(",");
  }

  public void writeHeader(String key, long value) {
    writeKey(key);
    buffer.append(value);
    buffer.append(",");
  }

  protected void writeKey(String key) {
    buffer.append("\"");
    buffer.append(key);
    buffer.append("\":");
  }

  protected void writeValue(String val) {
    buffer.append("\"");
    buffer.append(val);
    buffer.append("\"");
  }

  protected void writeNumberValue(String val) {
    buffer.append(val);
  }

  protected long getDuration(long startTime) {
    return Math.round((System.currentTimeMillis() - startTime) / 1000d);
  }

}
