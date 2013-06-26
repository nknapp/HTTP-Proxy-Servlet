package org.mitre.dsmiley.httpproxy;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;

/** @author knappmeier */
public class HeaderModificationProxyServlet extends ProxyServlet {

    private static HeaderGroup removedHeaders = new HeaderGroup();
    static {
        removedHeaders.addHeader(new BasicHeader("X-RemovedHeader",null));
        removedHeaders.addHeader(new BasicHeader("X-ReplacedHeader", null));
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
            public HeaderGroup removals() {
                return removedHeaders;
            }

            public HeaderGroup additions() {
                return addedHeaders;
            }
        });
        return proxy;
    }

}
