package org.avaje.metric.httpsend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.avaje.metric.Metric;
import org.avaje.metric.report.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.okhttp.OkHttpClient;

/**
 * Http(s) based Reporter that sends JSON formatted metrics message to a Repo server.
 */
public class HttpSendReporter implements MetricReporter {

  protected final Logger log = LoggerFactory.getLogger(HttpSendReporter.class);
  
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
    this.key = makeJsonSafe(key);
  }

  public void setApp(String app) {
    this.app = makeJsonSafe(app);
  }

  public void setEnv(String env) {
    this.env = makeJsonSafe(env);
  }

  public void setServer(String server) {
    this.server = makeJsonSafe(server);
  }


  public void report(List<Metric> metrics) {
    
    String json = buildJsonPayload(metrics);
    
    if (log.isTraceEnabled()) {
      log.trace("Sending:\n"+json);
    }
    
    try {
      String responseMessage = postJson(json.getBytes("UTF-8"));
      if (!"ok".equals(responseMessage)) {
        log.info("Did not get ok response, storing message for resend");
        storeJsonForResend(json);
      }
    } catch (Exception e) {
      // store json message in a file to resend later...
      log.error("Exception sending metrics, storing message for resend", e);
      storeJsonForResend(json);
    }
  }
  
  protected String makeJsonSafe(String value) {
    if (value == null) {
      return null;
    }
    // quotes considered invalid for our header values
    return value.replace("\"", "").replace("'", "").trim();
  }

  protected void storeJsonForResend(String json) {
    //TODO: storeJsonForResend
    log.info("Storing message for resend");
  }

  protected String buildJsonPayload(List<Metric> metrics) {
    
    JsonMetricVisitor jsonVisitor = new JsonMetricVisitor();
    return jsonVisitor.buildJson(this, metrics);    
  }

  
  public void cleanup() {
    // Do nothing
  }
  
  /**
   * Light weight http(s) post of JSON content to repo server.
   */
  protected String postJson(byte[] body) throws IOException {
    
    HttpURLConnection connection = client.open(url);
    OutputStream out = null;
    InputStream in = null;
    try {
      // Write the request.
      connection.setRequestMethod("POST");
      connection.setDoInput(true);

      connection.addRequestProperty("Content-Type", "application/json");

      out = connection.getOutputStream();
      out.write(body);
      out.close();

      // Read the response.
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException("Unexpected HTTP response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
      }
      in = connection.getInputStream();
      return readFirstLine(in);
      
    } finally {
      // Clean up.
      if (out != null) out.close();
      if (in != null) in.close();
    }
  }
  
  protected String readFirstLine(InputStream in) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    return reader.readLine();
  }

}
