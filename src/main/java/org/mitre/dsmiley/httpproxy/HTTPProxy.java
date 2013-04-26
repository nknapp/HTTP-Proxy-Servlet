package org.mitre.dsmiley.httpproxy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A proxy interface for http requests.
 *
 * @author gpaul
 *         Created on 4/26/13 10:02 AM
 */
public interface HTTPProxy {

    /**
     * Release any resource held by this proxy.
     */
    void shutdown();

    /**
     * Proxy the given request
     * @param servletRequest the request
     * @param servletResponse the response
     * @throws IOException
     * @throws ServletException
     */
    void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException;
}
