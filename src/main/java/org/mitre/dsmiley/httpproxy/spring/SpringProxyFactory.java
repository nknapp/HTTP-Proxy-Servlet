package org.mitre.dsmiley.httpproxy.spring;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.mitre.dsmiley.httpproxy.HTTPProxy;
import org.mitre.dsmiley.httpproxy.Proxy;
import org.mitre.dsmiley.httpproxy.balancing.BalancingProxy;
import org.springframework.util.StringUtils;

/**
 *
 * A factory bean that creates a proxy for a given number of target URLs.
 *
 * <h1>Example usage:</h1>
 *
 * see {@link SpringHTTPProxyServlet}
 *
 * @author knappmeier
 */
public class SpringProxyFactory {

    /**
     * Create a proxy instance for a number of target servers.
     * @param targetUrls the base URLs of the target servers. This may be a single URL, or a comma-separated list.
     *     If a more than one URL is provided, a {@link org.mitre.dsmiley.httpproxy.balancing.BalancingProxy} is created.
     * @param proxyPath passed to {@link Proxy#Proxy(java.net.URI, String)}
     * @param requestLimit passed to {@link org.mitre.dsmiley.httpproxy.balancing.BalancingProxy#BalancingProxy(java.util.List, int)} if multiple URLs
     * are provided. If 'targetUrls' only contains a single URL, this parameter has no effect.
     * @return an instance of {@link HTTPProxy}
     */
    public static HTTPProxy createProxy(String targetUrls,String proxyPath, int requestLimit) {
        if (targetUrls==null) {
            throw new IllegalArgumentException("targetUrls may not be null");
        }
        List<HTTPProxy> proxyList = new LinkedList<HTTPProxy>();
        String[] tokenizedUrls = StringUtils.tokenizeToStringArray(targetUrls, ",");
        if (tokenizedUrls.length==0) {
            throw new IllegalArgumentException("No targetUrls provided");
        }
        for (String url : tokenizedUrls) {
            proxyList.add(new Proxy(URI.create(url),proxyPath));
        }

        if (proxyList.size()==1) {
            return proxyList.get(0);
        } else {
            return new BalancingProxy(proxyList, requestLimit);
        }

    }
}
