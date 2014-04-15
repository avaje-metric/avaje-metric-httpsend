package org.avaje.metric.httpsend;

import java.util.List;

import org.avaje.metric.CounterMetric;
import org.avaje.metric.CounterStatistics;
import org.avaje.metric.GaugeMetric;
import org.avaje.metric.GaugeMetricGroup;
import org.avaje.metric.Metric;
import org.avaje.metric.TimedMetric;
import org.avaje.metric.ValueMetric;
import org.avaje.metric.ValueStatistics;
import org.avaje.metric.report.MetricVisitor;

/**
 * Writes the metric information as JSON to a buffer for sending.
 */
public class JsonMetricVisitor implements MetricVisitor {

  protected final int decimalPlaces;

  protected final StringBuilder buffer;

  public JsonMetricVisitor() {
    this.buffer = new StringBuilder(500);
    this.decimalPlaces = 2;
  }
  
  public JsonMetricVisitor(StringBuilder writer) {
    this.buffer = writer;
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
      metric.visitCollectedStatistics(this);
      buffer.append("\n");
    }
  }

  public String getBufferValue() {
    return buffer.toString();
  }
  

  protected void appendHeader(HttpSendReporter reporter) {
    
    writeHeader("time", System.currentTimeMillis());
    writeHeader("key", reporter.key);
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
      writeKeyNumber(m.getName().getName(), m.getFormattedValue(decimalPlaces));
    }
    buffer.append("]");
    writeMetricEnd(gaugeMetricGroup);
  }

  @Override
  public void visit(GaugeMetric metric) {

    writeMetricStart("gauge", metric);
    writeKeyNumber("value", metric.getFormattedValue(decimalPlaces));
    writeMetricEnd(metric);
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

  protected void writeKeyNumber(String key, double numberValue) {
    writeKeyNumber(key, String.valueOf(numberValue));
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
