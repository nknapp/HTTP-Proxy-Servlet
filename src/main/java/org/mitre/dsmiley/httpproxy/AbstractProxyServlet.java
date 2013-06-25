package org.mitre.dsmiley.httpproxy;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** @author knappmeier */
public abstract class AbstractProxyServlet extends HttpServlet {

    private HTTPProxy proxy;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.proxy = createProxy(servletConfig);
    }

    protected abstract HTTPProxy createProxy(ServletConfig servletConfig) throws ServletException;

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        proxy.service(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        if (proxy != null) {
            proxy.shutdown();
        }
        super.destroy();
    }
}
