package org.avaje.metric.httpsend;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.avaje.metric.report.HeaderInfo;
import org.avaje.metric.report.JsonWriteVisitor;
import org.avaje.metric.report.MetricReporter;
import org.avaje.metric.report.ReportMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.net.URL;

/**
 * Http(s) based Reporter that sends JSON formatted metrics message to a Repo server.
 */
public class HttpSendReporter implements MetricReporter {

  protected static final Logger logger = LoggerFactory.getLogger(HttpSendReporter.class);

  protected static final String KEY_CODE = "key-code";

  protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  protected final OkHttpClient client = new OkHttpClient();

  protected final URL url;

  public HttpSendReporter(URL url) {
    this.url = url;
  }

  /**
   * Send the non-empty metrics that were collected to the remote repository.
   */
  @Override
  public void report(ReportMetrics reportMetrics) {

    HeaderInfo headerInfo = reportMetrics.getHeaderInfo();
    String key = headerInfo.getKey();

    String json = null;

    try {
      StringWriter writer = new StringWriter(1000);
      JsonWriteVisitor jsonVisitor = new JsonWriteVisitor(writer, reportMetrics);
      jsonVisitor.write();

      json = writer.toString();

      if (logger.isTraceEnabled()) {
        logger.trace("Sending:\n {}", json);
      }

      RequestBody body = RequestBody.create(JSON, json);
      Request request = new Request.Builder()
          .url(url)
          .addHeader(KEY_CODE, key)
          .post(body)
          .build();

      Response response = client.newCall(request).execute();
      if (!response.isSuccessful()) {
        logger.info("Unsuccessful sending metrics payload to server");
        storeJsonForResend(json);
      }

    } catch (Exception e) {
      // store json message in a file to resend later...
      logger.error("Exception sending metrics to server", e);
      if (json != null) {
        storeJsonForResend(json);
      }
    }
  }

  protected void storeJsonForResend(String json) {
    // override this to support store and re-send 
  }


  @Override
  public void cleanup() {
    // Do nothing
  }

}
