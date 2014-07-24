package org.avaje.metric.httpsend;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.avaje.metric.*;
import org.avaje.metric.core.DefaultCounterMetric;
import org.avaje.metric.core.DefaultGaugeMetric;
import org.avaje.metric.core.DefaultTimedMetric;
import org.avaje.metric.core.DefaultValueMetric;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonMetricVisitorTest {

  private JsonMetricVisitor newJsonMetricVisitor() {
    return new JsonMetricVisitor(System.currentTimeMillis());
  }
  
  @Test
  public void testCounter() {
    
    JsonMetricVisitor jsonVisitor = newJsonMetricVisitor();
    
    CounterMetric counter = createCounterMetric();
    
    jsonVisitor.visit(counter);
    String counterJson = jsonVisitor.getBufferValue();
    
    Assert.assertEquals("{\"type\":\"counter\",\"name\":\"org.test.CounterFoo.doStuff\",\"count\":10,\"dur\":0}", counterJson);
  }

  
  @Test
  public void testGaugeMetric() {
    
    JsonMetricVisitor jsonVisitor = newJsonMetricVisitor();
    GaugeMetric metric = createGaugeMetric();
    
    jsonVisitor.visit(metric);
    String counterJson = jsonVisitor.getBufferValue();
    
    Assert.assertEquals("{\"type\":\"gauge\",\"name\":\"org.test.GaugeFoo.doStuff\",\"value\":24.0}", counterJson);
  }

  @Test
  public void testValueMetric() {
    
    JsonMetricVisitor jsonVisitor = newJsonMetricVisitor();
    
    ValueMetric metric = createValueMetric();
    
    jsonVisitor.visit(metric);
    String counterJson = jsonVisitor.getBufferValue();
    
    Assert.assertEquals("{\"type\":\"value\",\"name\":\"org.test.ValueFoo.doStuff\",\"n\":{\"count\":3,\"avg\":14,\"max\":16,\"sum\":42,\"dur\":0}}", counterJson);
  }

  
  @Test
  public void testTimedMetric() {
    
    JsonMetricVisitor jsonVisitor = newJsonMetricVisitor();
    
    TimedMetric metric = createTimedMetric();
    
    jsonVisitor.visit(metric);
    String counterJson = jsonVisitor.getBufferValue();
    
    // values converted into milliseconds
    Assert.assertEquals("{\"type\":\"timed\",\"name\":\"org.test.TimedFoo.doStuff\",\"n\":{\"count\":3,\"avg\":120,\"max\":140,\"sum\":360,\"dur\":0},\"e\":{\"count\":2,\"avg\":210,\"max\":220,\"sum\":420,\"dur\":0}}", counterJson);
  }

  @Test
  public void testMetricList() throws IOException {
    
    List<Metric> metrics = new ArrayList<Metric>();
    metrics.add(createValueMetric());
    metrics.add(createGaugeMetric());
    metrics.add(createCounterMetric());
    metrics.add(createTimedMetric());
    

    JsonMetricVisitor jsonVisitor = newJsonMetricVisitor();

    HttpSendReporter reporter = new HttpSendReporter();
    reporter.setKey("key-val");
    reporter.setEnv("dev");
    reporter.setApp("app-val");
    reporter.setServer("server-val");
    
    String json = jsonVisitor.buildJson(reporter, metrics);
    System.out.println("---");
    System.out.println(json);
    System.out.println("---");

    Assert.assertTrue(json.contains("\"env\":\"dev\""));
    Assert.assertTrue(json.contains("\"app\":\"app-val\""));
    Assert.assertTrue(json.contains("\"server\":\"server-val\""));
    
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonObject = mapper.readValue(json, ObjectNode.class);
    
    Assert.assertEquals("dev",jsonObject.get("env").asText());
    Assert.assertEquals("app-val",jsonObject.get("app").asText());
    Assert.assertEquals("server-val",jsonObject.get("server").asText());
    
    JsonNode jsonNode = jsonObject.get("metrics");
    ArrayNode metricArray = (ArrayNode)jsonNode;
    Assert.assertEquals(4, metricArray.size());
    
    boolean sendToLocalServer = true;
    if (sendToLocalServer) {
      reporter.setUrl(new URL("http://127.0.0.1:8090/message/hello"));
      reporter.report(metrics);
    }
  }
  

  private CounterMetric createCounterMetric() {
    CounterMetric counter = new DefaultCounterMetric(MetricManager.nameParse("org.test.CounterFoo.doStuff"));
    counter.markEvents(10);
    counter.collectStatistics();
    return counter;
  }
  
  private GaugeMetric createGaugeMetric() {
    Gauge gauge = new Gauge() {
      @Override
      public double getValue() {
        return 24d;
      }
    };
    GaugeMetric metric = new DefaultGaugeMetric(MetricManager.nameParse("org.test.GaugeFoo.doStuff"), gauge);
    metric.collectStatistics();
    return metric;
  }
  
  private ValueMetric createValueMetric() {
    ValueMetric metric = new DefaultValueMetric(MetricManager.nameParse("org.test.ValueFoo.doStuff"));
    metric.addEvent(12);
    metric.addEvent(14);
    metric.addEvent(16);
    metric.collectStatistics();
    return metric;
  }
  
  private TimedMetric createTimedMetric() {
    TimedMetric metric = new DefaultTimedMetric(MetricManager.nameParse("org.test.TimedFoo.doStuff"));
    metric.addEventDuration(true, 100000);
    metric.addEventDuration(true, 120000);
    metric.addEventDuration(true, 140000);
    metric.addEventDuration(false, 200000);
    metric.addEventDuration(false, 220000);
    
    metric.collectStatistics();
    return metric;
  }
}
