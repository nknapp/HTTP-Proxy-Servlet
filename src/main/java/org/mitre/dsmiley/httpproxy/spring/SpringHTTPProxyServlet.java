package org.mitre.dsmiley.httpproxy.spring;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.mitre.dsmiley.httpproxy.AbstractProxyServlet;
import org.mitre.dsmiley.httpproxy.HTTPProxy;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * HTTPProxyServlet that retrieve its proxy-instance from a spring container.
 * The bean name must be configured.
 *
 * <
 *
 * @author knappmeier */
public class SpringHTTPProxyServlet extends AbstractProxyServlet {

    public static final String PROXY_BEAN_NAME = "proxyBeanName";

    @Override
    protected HTTPProxy createProxy(ServletConfig servletConfig) throws ServletException {
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        String proxyBeanName = servletConfig.getInitParameter(PROXY_BEAN_NAME);
        if (proxyBeanName==null)  {
            throw new ServletException("Init-Parameter '" + PROXY_BEAN_NAME + "' must be configured for servlet " +getServletName());
        }
        return context.getBean(proxyBeanName, HTTPProxy.class);
    }
}
