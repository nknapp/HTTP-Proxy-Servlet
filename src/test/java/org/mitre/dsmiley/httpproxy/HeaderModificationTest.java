package org.mitre.dsmiley.httpproxy;

import org.junit.Assert;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebResponse;

/** @author knappmeier */
public class HeaderModificationTest extends AbstractProxyServletTest {


    @Override
    protected Class<? extends ProxyServlet> proxyServletClass() {
        return HeaderModificationProxyServlet.class;
    }

    @Test
    public void testRemoveHeader() throws Exception {
        GetMethodWebRequest request = makeGetMethodRequest(sourceBaseUri);
        request.setHeaderField("X-RemovedHeader","testValue");
        WebResponse webResponse = execAssert(request);
        String responseText = webResponse.getText();
        Assert.assertFalse("Check for X-RemovedHeader", responseText.contains("X-RemovedHeader:"));
    }

    @Test
    public void testReplaceHeader() throws Exception {
        GetMethodWebRequest request = makeGetMethodRequest(sourceBaseUri);
        request.setHeaderField("X-ReplacedHeader","testValue");
        WebResponse webResponse = execAssert(request);
        String responseText = webResponse.getText();
        Assert.assertTrue("Check for X-ReplacedHeader", responseText.contains("X-ReplacedHeader: replaced"));
    }

    @Test
    public void testAddHeader() throws Exception {
        GetMethodWebRequest request = makeGetMethodRequest(sourceBaseUri);
        request.setHeaderField("X-AddedHeader","testValue");
        WebResponse webResponse = execAssert(request);
        String responseText = webResponse.getText();
        Assert.assertTrue("Check for X-AddedHeader", responseText.contains("X-AddedHeader: added"));
        Assert.assertTrue("Check for X-AddedHeader", responseText.contains("X-AddedHeader: testValue"));
    }

    @Test
    public void testUntouchedHeader() throws Exception {
        GetMethodWebRequest request = makeGetMethodRequest(sourceBaseUri);
        request.setHeaderField("X-UntouchedHeader","testValue");
        WebResponse webResponse = execAssert(request);
        String responseText = webResponse.getText();
        Assert.assertTrue("Check for X-UntouchedHeader", responseText.contains("X-UntouchedHeader: testValue"));
    }


}
