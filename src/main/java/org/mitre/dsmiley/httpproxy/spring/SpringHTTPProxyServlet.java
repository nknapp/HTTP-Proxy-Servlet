package org.mitre.dsmiley.httpproxy.spring;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.mitre.dsmiley.httpproxy.AbstractProxyServlet;
import org.mitre.dsmiley.httpproxy.HTTPProxy;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * HTTPProxyServlet that retrieve its proxy-instance from a spring container.
 *
 * <h1>Example-configuration in web.xml</h1>
 *
 * <pre>
 * {@code
 *
 * <servlet>
 *      <servlet-name>ProxyServlet</servlet-name>
 *      <servlet-class>org.mitre.dsmiley.httpproxy.spring.SpringHTTPProxyServlet</servlet-class>
 *      <init-param>
 *          <param-name>proxyBeanName</param-name>
 *          <param-value>proxy</param-value>
 *      </init-param>
 *      <load-on-startup>1</load-on-startup>
 * </servlet>
 * <servlet-mapping>
 *      <servlet-name>ProxyServlet</servlet-name>
 *      <url-pattern>/proxy/*</url-pattern>
 * </servlet-mapping>
 * }
 * </pre>
 *
 * <h1>Corresponding spring-configuration</h1>
 *
 * <pre>{@code
 *    <bean id="proxy" class="org.mitre.dsmiley.httpproxy.spring.SpringProxyFactory" factory-method="createProxy">
 *      <constructor-arg index="0" value="http://localhost:8090,http://localhost:8091,http://localhost:8092" />
 *      <constructor-arg index="1" value="" />
 *      <constructor-arg index="2" value="1" />
 *    </bean>
 * }
 *
 * </pre>
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
