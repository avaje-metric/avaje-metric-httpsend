package org.avaje.metric.httpsend;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.squareup.okhttp.*;

import org.avaje.metric.Metric;
import org.avaje.metric.report.HeaderInfo;
import org.avaje.metric.report.JsonWriteVisitor;
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

  protected HeaderInfo headerInfo;

  public HttpSendReporter() {

  }

  /**
   * Set the URL of the repository the metrics are sent to.
   */
  public void setUrl(URL url) {
    this.url = url;
  }

  /**
   * Set the header information which identifies the specific source of the metrics.
   */
  public void setHeaderInfo(HeaderInfo headerInfo) {
    this.headerInfo = headerInfo;
  }

  /**
   * Send the non-empty metrics that were collected to the remote repository.
   */
  public void report(List<Metric> metrics) {

    String json = null;
    
    try {  
      long collectionTime = System.currentTimeMillis();
      json = buildJsonPayload(metrics, collectionTime);

      if (log.isTraceEnabled()) {
        log.trace("Sending:\n {}", json);
      }
      
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
      if (json != null) {
        storeJsonForResend(json);
      }
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
    // override this to support store and re-send 
  }

  protected String buildJsonPayload(List<Metric> metrics, long collectionTime) throws IOException {

    JsonWriteVisitor jsonVisitor = new JsonWriteVisitor(collectionTime);
    return jsonVisitor.buildJson(headerInfo, metrics);
  }

  public void cleanup() {
    // Do nothing
  }

}
