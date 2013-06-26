package org.mitre.dsmiley.httpproxy.balancing;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mitre.dsmiley.httpproxy.HTTPProxy;

/**
 * A simple load-balancing proxy that makes sure that each target
 * server is only handed a limited number requests at a time.
 *
 *
 * @author knappmeier
 **/
public class BalancingProxy implements HTTPProxy {


    private static final Log LOG = LogFactory.getLog(BalancingProxy.class);

    private List<? extends HTTPProxy> proxyList;
    /**
     * List of target servers that are currently available for a request
     */
    private LinkedBlockingQueue<HTTPProxy> availableProxies;

    /**
     * Create a new BalancingProxy
     * @param proxyList the list of delegate proxy instances
     * @param requestsPerTarget the number of simultaneous requests that each proxy instance should handle at a time.
     */
    public BalancingProxy(List<? extends HTTPProxy> proxyList, int requestsPerTarget) {
        this.proxyList = proxyList;
        this.availableProxies = new LinkedBlockingQueue<HTTPProxy>(proxyList.size()*requestsPerTarget);
        for (int i=0; i<requestsPerTarget; i++) {
            for (HTTPProxy httpProxy : proxyList) {
                this.availableProxies.add(httpProxy);
            }
        }

    }

    @Override
    public void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {
        HTTPProxy currentProxy;
        try {
            currentProxy = availableProxies.poll(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interupted while taking proxy from pool",e);
        }
        try {
            currentProxy.service(servletRequest,servletResponse);
        } finally {
            try {
                availableProxies.put(currentProxy);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interupted while putting proxy back into pool",e);
            }
        }

    }

    /**
     * Shutdown all delegate proxies. It is ensured that {@link org.mitre.dsmiley.httpproxy.HTTPProxy#shutdown()}
     * is called for all delegate proxies, even if one of them throws an exception.
     * If multiple exceptions are caught during shutdown, the last one is re-thrown in the end.
     */
    @Override
    public void shutdown() {
        RuntimeException exception = null;
        for (HTTPProxy proxy : proxyList) {
            try {
                proxy.shutdown();
            } catch (RuntimeException e) {
                exception = e;
                LOG.error("Error while shutting down proxy "+proxy,e);
            }
        }
        if (exception!=null) {
            throw exception;
        }

    }

    @Override
    public void setHeaderModificatons(HeaderModificatons headerModificatons) {
        for (HTTPProxy httpProxy : proxyList) {
            httpProxy.setHeaderModificatons(headerModificatons);
        }
    }

}
