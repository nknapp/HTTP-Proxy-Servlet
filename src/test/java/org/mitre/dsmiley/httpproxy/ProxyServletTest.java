package org.mitre.dsmiley.httpproxy;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.URI;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author David Smiley - dsmiley@mitre.org
 */
public class ProxyServletTest extends AbstractProxyServletTest {

    @After
  public void tearDown() throws Exception {
   servletRunner.shutDown();
   localTestServer.stop();
  }

  private static String[] testUrlSuffixes = new String[]{
          "","/pathInfo","?q=v","/p?q=v","/p?#f","/p?#",
          "/p?id=p%20i", "/p%20i" // encoded space in param then in path
  };

  @Test
  public void testGet() throws Exception {
    for (String urlSuffix : testUrlSuffixes) {
      execAssert(makeGetMethodRequest(sourceBaseUri + urlSuffix));
    }
  }

  @Test @Ignore
  public void testOnlyFragment() throws Exception {
    //TODO These fail; should they?  Do they fail because of the test infrastructure? Maybe we should switch to Jetty.
    execAssert(makeGetMethodRequest(sourceBaseUri + "/p#f"));
    execAssert(makeGetMethodRequest(sourceBaseUri + "#f"));
    execAssert(makeGetMethodRequest(sourceBaseUri + "/#f"));
  }

  @Test
  public void testPost() throws Exception {
    for (String urlSuffix : testUrlSuffixes) {
      execAndAssert(makePostMethodRequest(sourceBaseUri + urlSuffix));
    }
  }

  @Test
  public void testRedirect() throws IOException, SAXException {
    localTestServer.register("/targetPath*",new HttpRequestHandler()
    {
      public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        response.setHeader(HttpHeaders.LOCATION,request.getFirstHeader("xxTarget").getValue());
        response.setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY);
      }
    });//matches /targetPath and /targetPath/blahblah
    GetMethodWebRequest request = makeGetMethodRequest(sourceBaseUri);
    assertRedirect(request, "/dummy", "/dummy");//TODO represents a bug to fix
    assertRedirect(request, targetBaseUri+"/dummy?a=b", sourceBaseUri+"/dummy?a=b");
  }

    @Test
  public void testSendFile() throws Exception {
    final PostMethodWebRequest request = new PostMethodWebRequest("http://localhost/proxyMe",true);//true: mime encoded
    InputStream data = new ByteArrayInputStream("testFileData".getBytes("UTF-8"));
    request.selectFile("fileNameParam", "fileName", data, "text/plain");
    WebResponse rsp = execAndAssert(request);
    assertTrue(rsp.getText().contains("Content-Type: multipart/form-data; boundary="));
  }

  @Test
  public void testProxyWithUnescapedChars() throws Exception {
    execAssert(makeGetMethodRequest(sourceBaseUri + "?fq={!f=field}"), "?fq=%7B!f=field%7D");//has squiggly brackets
    execAssert(makeGetMethodRequest(sourceBaseUri + "?fq=%7B!f=field%7D"));//already escaped; don't escape twice
  }

  /** http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html */
  @Test
  public void testHopByHopHeadersOnSource() throws Exception {
    //"Proxy-Authenticate" is a hop-by-hop header
    final String HEADER = "Proxy-Authenticate";
    localTestServer.register("/targetPath*", new RequestInfoHandler() {
      public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        assertNull(request.getFirstHeader(HEADER));
        response.setHeader(HEADER, "from-server");
        super.handle(request, response, context);
      }
    });

    GetMethodWebRequest req = makeGetMethodRequest(sourceBaseUri);
    req.getHeaders().put(HEADER, "from-client");
    WebResponse rsp = execAndAssert(req, "");
    assertNull(rsp.getHeaderField(HEADER));
  }

}
