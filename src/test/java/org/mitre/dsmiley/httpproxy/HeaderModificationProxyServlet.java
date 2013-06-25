package org.mitre.dsmiley.httpproxy;

import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;

/** @author knappmeier */
public class HeaderModificationProxyServlet extends ProxyServlet {

    private static Set<String> removedHeaders = new HashSet<String>();
    static {
        removedHeaders.add("X-RemovedHeader");
        removedHeaders.add("X-ReplacedHeader");
    }

    private static HeaderGroup addedHeaders = new HeaderGroup();
    static {
        addedHeaders.addHeader(new BasicHeader("X-AddedHeader","added"));
        addedHeaders.addHeader(new BasicHeader("X-ReplacedHeader","replaced"));
    }


    @Override
    protected HTTPProxy createProxy(ServletConfig servletConfig) throws ServletException {
        HTTPProxy proxy = super.createProxy(servletConfig);
        proxy.setHeaderModificatons(new Proxy.HeaderModificatons() {
            public Set<String> removals(HttpServletRequest servletRequest) {
                return removedHeaders;
            }

            public HeaderGroup additions(HttpServletRequest servletRequest) {
                return addedHeaders;
            }
        });
        return proxy;
    }

}
