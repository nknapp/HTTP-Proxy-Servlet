package org.mitre.dsmiley.httpproxy.balancing;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.message.HeaderGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mitre.dsmiley.httpproxy.HTTPProxy;
import org.mitre.dsmiley.httpproxy.balancing.BalancingProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author knappmeier */
public class BalancingProxyTest {

    public static final int REQUESTS_PER_TARGET = 2;
    private LinkedList<DummyProxy> httpProxies;
    private BalancingProxy balancingProxy;


    @Before
    public void setup() {
        httpProxies = new LinkedList<DummyProxy>();
        for (int i=0;i<5;i++) {
            httpProxies.add(new DummyProxy());
        }
        balancingProxy = new BalancingProxy(httpProxies, REQUESTS_PER_TARGET);
    }

    @Test
    public void testRequestsPerTarget() throws Exception {

        LinkedList<Callable<Object>> tasks = new LinkedList<Callable<Object>>();
        for (int i=0; i<200; i++) {
            tasks.add(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    balancingProxy.service(new MockHttpServletRequest(), new MockHttpServletResponse());
                    return null;
                }
            });
        }

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        executorService.invokeAll(tasks);
        executorService.shutdown();

        for (DummyProxy httpProxy : httpProxies) {
            Assert.assertTrue("DummyProxy may not exceed requestLimit", httpProxy.getMaxRequestCount()<=2);
        }
    }


    /**
     * When possible, requests should be dispatched in a round-robin fashion
     * @throws Exception
     */
    @Test
    public void testRoundRobin() throws Exception {

        for (int i=0; i<5; i++) {
            balancingProxy.service(new MockHttpServletRequest(), new MockHttpServletResponse());
        }

        for (DummyProxy httpProxy : httpProxies) {
            Assert.assertEquals("Each proxy must be chosen once", 1, httpProxy.getMaxRequestCount());
        }
    }

    @Test
    public void testDispatchHeaderModifications() throws Exception {
        HTTPProxy.HeaderModificatons headerModificatons = new HTTPProxy.HeaderModificatons() {
            @Override
            public HeaderGroup removals() {
                return new HeaderGroup();
            }

            @Override
            public HeaderGroup additions() {
                return new HeaderGroup();
            }
        };
        balancingProxy.setHeaderModificatons(headerModificatons);
        for (DummyProxy httpProxy : httpProxies) {
            Assert.assertSame(httpProxy.getHeaderModifications(),headerModificatons);
        }
    }

    @Test
    public void testDispatchShutdownWithExceptions() {
        Exception exception = null;
        try {
            balancingProxy.shutdown();
        } catch (RuntimeException e) {
            exception = e;
        }
        Assert.assertNotNull("An exception must have been thrown during shutdown",exception);
        for (DummyProxy httpProxy : httpProxies) {
            Assert.assertTrue("Proxy must be shut down", httpProxy.isShutdownReceived());
        }
    }



    public static class DummyProxy implements HTTPProxy {

        private AtomicInteger currentRequestCount = new AtomicInteger();
        private AtomicInteger maxRequestCount = new AtomicInteger();
        private boolean shutdownReceived;
        private HeaderModificatons headerModifications;


        @Override
        public void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {
            int i = currentRequestCount.incrementAndGet();
            maxRequestCount.set(Math.max(i,maxRequestCount.get()));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            currentRequestCount.decrementAndGet();
        }

        @Override
        public void shutdown() {
            this.shutdownReceived = true;
            throw new RuntimeException("Exception thrown during shutdown to test shutdown of all proxies.");
        }

        @Override
        public void setHeaderModificatons(HTTPProxy.HeaderModificatons headerModifications) {
            this.headerModifications = headerModifications;
        }

        public int getMaxRequestCount() {
            return maxRequestCount.get();
        }

        public boolean isShutdownReceived() {
            return shutdownReceived;
        }

        private HeaderModificatons getHeaderModifications() {
            return headerModifications;
        }
    }
}
