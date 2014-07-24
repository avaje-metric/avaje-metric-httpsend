package org.avaje.metric.httpsend;

import java.net.URL;
import java.util.List;

import com.squareup.okhttp.*;
import org.avaje.metric.Metric;
import org.avaje.metric.report.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http(s) based Reporter that sends JSON formatted metrics message to a Repo server.
 */
public class HttpSendReporter implements MetricReporter {

  protected final Logger log = LoggerFactory.getLogger(HttpSendReporter.class);

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  protected final OkHttpClient client = new OkHttpClient();

  protected URL url;

  protected String key;

  protected String app;

  protected String env;

  protected String server;

  public HttpSendReporter() {

  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public void setKey(String key) {
    this.key = stripQuotes(key);
  }

  public void setApp(String app) {
    this.app = stripQuotes(app);
  }

  public void setEnv(String env) {
    this.env = stripQuotes(env);
  }

  public void setServer(String server) {
    this.server = stripQuotes(server);
  }


  public void report(List<Metric> metrics) {

    long collectionTime = System.currentTimeMillis();
    String json = buildJsonPayload(metrics, collectionTime);

    if (log.isTraceEnabled()) {
      log.trace("Sending:\n {}", json);
    }

    try {

      RequestBody body = RequestBody.create(JSON, json);
      Request request = new Request.Builder()
        .url(url)
        .post(body)
        .build();

      Response response = client.newCall(request).execute();
      if (!response.isSuccessful()) {
        log.info("Unsuccessful sending metrics payload to server");
        storeJsonForResend(json);
      }

    } catch (Exception e) {
      // store json message in a file to resend later...
      log.error("Exception sending metrics to server", e);
      storeJsonForResend(json);
    }
  }

  /**
   * Trim out single and double quotes from our key header values.
   */
  protected String stripQuotes(String value) {
    if (value == null) {
      return null;
    }
    // quotes considered invalid for our header values
    return value.replace("\"", "").replace("'", "").trim();
  }

  protected void storeJsonForResend(String json) {
    // currently not doing this;
  }

  protected String buildJsonPayload(List<Metric> metrics, long collectionTime) {

    JsonMetricVisitor jsonVisitor = new JsonMetricVisitor(collectionTime);
    return jsonVisitor.buildJson(this, metrics);
  }

  public void cleanup() {
    // Do nothing
  }

}
