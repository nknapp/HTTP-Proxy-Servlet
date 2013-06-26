package org.mitre.dsmiley.httpproxy.spring;

import org.junit.Assert;
import org.junit.Test;
import org.mitre.dsmiley.httpproxy.HTTPProxy;
import org.mitre.dsmiley.httpproxy.Proxy;
import org.mitre.dsmiley.httpproxy.balancing.BalancingProxy;

/** @author knappmeier */
public class SpringProxyFactoryTest {

    @Test
    public void testCreateProxy() throws Exception {
        HTTPProxy proxy = SpringProxyFactory.createProxy("http://localhost:8090/,http://localhost:8091", "/proxyPath", 2);
        Assert.assertTrue("Must be a balancing proxy", proxy instanceof BalancingProxy);
    }

    @Test
    public void testCreateSingleProxy() throws Exception {
        HTTPProxy proxy = SpringProxyFactory.createProxy("http://localhost:8090/", "/proxyPath", 2);
        Assert.assertTrue("Must be a single proxy", proxy instanceof Proxy);
    }

    @Test
    public void testCreateHandleEmptyURLAfterRealURL() throws Exception {
        HTTPProxy proxy = SpringProxyFactory.createProxy("http://localhost:8090/,", "/proxyPath", 2);
        Assert.assertTrue("Must be a single proxy", proxy instanceof Proxy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateHandleEmptyURL() throws Exception {
        HTTPProxy proxy = SpringProxyFactory.createProxy("", "/proxyPath", 2);
        Assert.assertTrue("Must be a single proxy", proxy instanceof Proxy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateHandleNullURL() throws Exception {
        HTTPProxy proxy = SpringProxyFactory.createProxy(null, "/proxyPath", 2);
        Assert.assertTrue("Must be a single proxy", proxy instanceof Proxy);
    }
}
