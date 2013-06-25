package org.mitre.dsmiley.httpproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

/** @author knappmeier */
public class AbstractProxyServletTest {

    private static final Log log = LogFactory.getLog(ProxyServletTest.class);
    /**
     * From Apache httpcomponents/httpclient. Note httpunit has a similar thing called PseudoServlet but it is
     * not as good since you can't even make it echo the request back.
     */
    protected LocalTestServer localTestServer;
    /** From Meterware httpunit. */
    protected ServletRunner servletRunner;
    protected String targetBaseUri;
    protected String sourceBaseUri;
    private ServletUnitClient sc;

    //Fixes problems in HttpUnit in which I can't specify the query string via the url. I don't want to use
    // setParam on a get request.
    @SuppressWarnings({"unchecked"})
    private static <M> M makeMethodRequest(final String url, Class<M> clazz) {
      log.info("Making request to url "+url);
      String urlNoQuery;
      final String queryString;
      int qIdx = url.indexOf('?');
      if (qIdx == -1) {
        urlNoQuery = url;
        queryString = null;
      } else {
        urlNoQuery = url.substring(0,qIdx);
        queryString = url.substring(qIdx + 1);

      }
      //WARNING: Ugly! Groovy could do this better.
      if (clazz == PostMethodWebRequest.class) {
        return (M) new PostMethodWebRequest(urlNoQuery) {
          @Override
          public String getQueryString() {
            return queryString;
          }
          @Override
          protected String getURLString() {
            return url;
          }
        };
      } else if (clazz == GetMethodWebRequest.class) {
        return (M) new GetMethodWebRequest(urlNoQuery) {
          @Override
          public String getQueryString() {
            return queryString;
          }
          @Override
          protected String getURLString() {
            return url;
          }
        };
      }
      throw new IllegalArgumentException(clazz.toString());
    }

    @Before
    public void setUp() throws Exception {
      localTestServer = new LocalTestServer(null, null);
      localTestServer.start();
      localTestServer.register("/targetPath*", new RequestInfoHandler());//matches /targetPath and /targetPath/blahblah
      targetBaseUri = "http://localhost:"+localTestServer.getServiceAddress().getPort()+"/targetPath";

      servletRunner = new ServletRunner();
      Properties params = new Properties();
      params.setProperty("http.protocol.handle-redirects", "false");
      params.setProperty("targetUri",targetBaseUri);
      params.setProperty(ProxyServlet.P_LOG, "true");
      servletRunner.registerServlet("/proxyMe/*", proxyServletClass().getName(), params);//also matches /proxyMe (no path info)
      sourceBaseUri = "http://localhost/proxyMe";//localhost:0 is hard-coded in ServletUnitHttpRequest
      sc = servletRunner.newClient();
      sc.getClientProperties().setAutoRedirect(false);//don't want httpunit itself to redirect
    }

    /**
     * Override in sub-classes to test other ProxyServlet instances
     * @return
     */
    protected Class<? extends ProxyServlet> proxyServletClass() {
        return ProxyServlet.class;
    }

    protected void assertRedirect(GetMethodWebRequest request, String origRedirect, String resultRedirect) throws IOException, SAXException {
      request.setHeaderField("xxTarget", origRedirect);
      WebResponse rsp = sc.getResponse( request );

      assertEquals(HttpStatus.SC_MOVED_TEMPORARILY,rsp.getResponseCode());
      assertEquals("",rsp.getText());
      String gotLocation = rsp.getHeaderField(HttpHeaders.LOCATION);
      assertEquals(resultRedirect, gotLocation);
    }

    protected WebResponse execAssert(GetMethodWebRequest request, String expectedUri) throws Exception {
      return execAndAssert(request, expectedUri);
    }

    protected WebResponse execAssert(GetMethodWebRequest request) throws Exception {
      return execAndAssert(request,null);
    }

    protected WebResponse execAndAssert(PostMethodWebRequest request) throws Exception {
      request.setParameter("abc","ABC");

      WebResponse rsp = execAndAssert(request, null);

      assertTrue(rsp.getText().contains("ABC"));
      return rsp;
    }

    protected WebResponse execAndAssert(WebRequest request, String expectedUri) throws Exception {
      WebResponse rsp = sc.getResponse( request );

      assertEquals(HttpStatus.SC_OK,rsp.getResponseCode());
      //HttpUnit doesn't pass the message; not a big deal
      //assertEquals("TESTREASON",rsp.getResponseMessage());
      final String text = rsp.getText();
      assertTrue(text.startsWith("REQUESTLINE:"));

      if (expectedUri == null)
        expectedUri = request.getURL().toString().substring(sourceBaseUri.length());

      String firstTextLine = text.substring(0,text.indexOf(System.getProperty("line.separator")));

      String expectedTargetUri = new URI(this.targetBaseUri).getPath() + expectedUri;
      String expectedFirstLine = "REQUESTLINE: "+(request instanceof GetMethodWebRequest ? "GET" : "POST");
      expectedFirstLine += " " + expectedTargetUri + " HTTP/1.1";
      assertEquals(expectedFirstLine,firstTextLine);

      return rsp;
    }

    protected GetMethodWebRequest makeGetMethodRequest(final String url) {
      return makeMethodRequest(url,GetMethodWebRequest.class);
    }

    protected PostMethodWebRequest makePostMethodRequest(final String url) {
      return makeMethodRequest(url,PostMethodWebRequest.class);
    }

    /**
     * Writes all information about the request back to the response.
     */
    protected static class RequestInfoHandler implements HttpRequestHandler
    {

      public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos,false);
        final RequestLine rl = request.getRequestLine();
        pw.println("REQUESTLINE: " + rl);

        for (Header header : request.getAllHeaders()) {
          pw.println(header.getName() + ": " + header.getValue());
        }
        pw.println("BODY: (below)");
        pw.flush();//done with pw now

        if (request instanceof HttpEntityEnclosingRequest) {
          HttpEntityEnclosingRequest enclosingRequest = (HttpEntityEnclosingRequest) request;
          HttpEntity entity = enclosingRequest.getEntity();
          byte[] body = EntityUtils.toByteArray(entity);
          baos.write(body);
        }

        response.setStatusCode(200);
        response.setReasonPhrase("TESTREASON");
        response.setEntity(new ByteArrayEntity(baos.toByteArray()));
      }
    }
}
